import com.google.common.util.concurrent.RateLimiter;
import datatypes.datapoints.CrayfishDataBatch;
import datatypes.datapoints.CrayfishDataGenerator;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import serde.data.CrayfishDataBatchSerde;
import config.CrayfishConfig;

import java.io.IOException;
import java.util.Properties;
import java.util.function.Consumer;

public class KafkaInputProducer {
    private static final Logger logger = LogManager.getLogger(KafkaInputProducer.class);
    private final CrayfishConfig config;
    private final CrayfishDataGenerator generator;
    private final RateLimiter rateLimiter;

    public KafkaInputProducer(int inputRate, int numRecords, CrayfishConfig config) {
        this.config = config;
        this.generator = new CrayfishDataGenerator(config.getInt("bs"), config.getModelInputShape(), numRecords);
        double effectiveInputRate = inputRate >= 0 ? inputRate : -1.0 / inputRate;
        this.rateLimiter = RateLimiter.create(effectiveInputRate);
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
                        config.getString("in"), null, null, inputBatch.hashCode(), inputBatch);
                producer.send(record);
                //logger.debug("Sent record created at timestamp " + record.value().getCreationTimestamp());
                rateLimiter.acquire();
            }
        });
    }

    private Producer<Integer, CrayfishDataBatch> getProducerProperties() throws IOException {
        Properties properties = new Properties();
        // String bootstrapServer = config.getString("kafka.bootstrap.servers");
        properties.setProperty("bootstrap.servers", config.getString("ce"));
        properties.setProperty("group.id", "KafkaInputProducer");

        if (config.hasOption("user") && config.hasOption("pswd")) {
            String username = config.getString("user");
            String password = config.getString("pswd");
            String jaasConf =
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + username +
                    "\" password=\"" + password + "\";";
            properties.setProperty("sasl.jaas.config", jaasConf);
            properties.setProperty("security.protocol", "SASL_PLAINTEXT");
            properties.setProperty("sasl.mechanism", "PLAIN");
        }

        // set the maximum request size to allow for batching for the large models
        properties.setProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, config.getString("rs"));

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
