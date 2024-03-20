import com.google.common.util.concurrent.RateLimiter;
import datatypes.datapoints.CrayfishDataBatch;
import datatypes.datapoints.CrayfishDataGenerator;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import serde.data.CrayfishDataBatchSerde;

import java.io.IOException;
import java.util.Properties;
import java.util.function.Consumer;

public class KafkaInputProducer {
    private static final Logger logger = LogManager.getLogger(KafkaInputProducer.class);
    private final Configuration config;
    private final CrayfishDataGenerator generator;
    private final RateLimiter rateLimiterStable;
    private final RateLimiter rateLimiterBursty;
    private RateLimiter currentRateLimiter;

    private long switchTime;

    public KafkaInputProducer(int inputRate, int numRecords, int batchSize, int[] inputSize, boolean isBursty,
                              Configuration config) throws ConfigurationException {
        this.config = config;
        this.generator = new CrayfishDataGenerator(batchSize, inputSize, numRecords);
        double effectiveInputRate = inputRate >= 0 ? inputRate : -1.0 / inputRate;

        if (isBursty) {
            // if the workload is bursty, then we get as argument the maximum input rate required to be sustained, which
            // is the input rate at the peak time. In this case, the "stable" time is 70% of the sustainable
            // throughput. Mind that the bursty rate is 110% of the sustainable throughput.
            this.rateLimiterStable = RateLimiter.create(0.7 * effectiveInputRate / 1.1);
            this.rateLimiterBursty = RateLimiter.create(effectiveInputRate);
            // create bursts after 2 minutes
            this.switchTime = System.currentTimeMillis() + 120_000;
            // start with stable rate
            this.currentRateLimiter = this.rateLimiterStable;
        } else {
            this.rateLimiterStable = RateLimiter.create(effectiveInputRate);
            this.rateLimiterBursty = null;
        }
    }

    public void run() throws IOException {
        publishMessages();
    }

    public void publishMessages() throws IOException {
        final Producer<Integer, CrayfishDataBatch> producer = getProducerProperties();

        // Send experiment records
        this.generator.forEachRemaining(new Consumer<CrayfishDataBatch>() {
            @Override
            public void accept(CrayfishDataBatch inputBatch) {
                final ProducerRecord<Integer, CrayfishDataBatch> record = new ProducerRecord<Integer, CrayfishDataBatch>(
                        config.getString("kafka.input.data.topic"), null, null, inputBatch.hashCode(), inputBatch);
                producer.send(record);
                RateLimiter currentLimiter = getCurrentRateLimiter();
                currentLimiter.acquire();
            }
        });
    }

    private RateLimiter getCurrentRateLimiter() {
        // Switch rate limiters such that the stable rate workload runs for 2 minutes.
        // The burst lasts for 15 seconds.
        if (System.currentTimeMillis() >= switchTime) {
            if (this.currentRateLimiter.equals(this.rateLimiterStable)) {
                switchTime = System.currentTimeMillis() + 30_000;
                this.currentRateLimiter = rateLimiterBursty;
            } else {
                switchTime = System.currentTimeMillis() + 120_000;
                this.currentRateLimiter = rateLimiterStable;
            }
        }
        return this.currentRateLimiter;
    }

    private Producer<Integer, CrayfishDataBatch> getProducerProperties() throws IOException {
        Properties properties = new Properties();
        String bootstrapServer = config.getString("kafka.bootstrap.servers");
        properties.setProperty("bootstrap.servers", bootstrapServer);
        properties.setProperty("group.id", "KafkaInputProducer");

        if (config.containsKey("kafka.auth.username") && config.containsKey("kafka.auth.password")) {
            String username = config.getString("kafka.auth.username");
            String password = config.getString("kafka.auth.password");
            String jaasConf =
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + username +
                    "\" password=\"" + password + "\";";
            properties.setProperty("sasl.jaas.config", jaasConf);
            properties.setProperty("security.protocol", "SASL_PLAINTEXT");
            properties.setProperty("sasl.mechanism", "PLAIN");
        }

        // set the maximum request size to allow for batching for the large models
        properties.setProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, config.getString("kafka.max.req.size"));

        return new org.apache.kafka.clients.producer.KafkaProducer<Integer, CrayfishDataBatch>(properties,
                                                                                               new IntegerSerializer(),
                                                                                               new CrayfishSerializer());
    }

    private static class CrayfishSerializer implements Serializer<CrayfishDataBatch> {
        CrayfishDataBatchSerde serde = new CrayfishDataBatchSerde();

        @Override
        public byte[] serialize(String s, CrayfishDataBatch data) {
            try {
                return serde.serialize(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
