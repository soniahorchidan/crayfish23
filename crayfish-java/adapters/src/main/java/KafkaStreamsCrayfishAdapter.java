import datatypes.datapoints.CrayfishDataBatch;
import datatypes.datapoints.CrayfishPrediction;
import datatypes.models.CrayfishModel;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import serde.data.CrayfishDataBatchSerde;
import utils.CrayfishUtils;
import config.CrayfishConfig;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

public class KafkaStreamsCrayfishAdapter<M extends CrayfishModel>
        extends Crayfish<StreamsBuilder, KStream<String, CrayfishDataBatch>, KStream<String, CrayfishDataBatch>, M>
        implements Serializable {
    private static final Logger logger = LogManager.getLogger(KafkaStreamsCrayfishAdapter.class);
    private final StoreBuilder<KeyValueStore<String, CrayfishDataBatch>> crayfishStateStore;

    public KafkaStreamsCrayfishAdapter(Class<M> modelClass, CrayfishConfig config) {
        super(modelClass, config, false);

        this.crayfishStateStore = Stores
                .keyValueStoreBuilder(Stores.persistentKeyValueStore("CrayfishStateStore"), Serdes.String(),
                                      (new KafkaSerDe().getSerDe())).withCachingEnabled();
    }

    @Override
    public StreamsBuilder streamBuilder() {
        final StreamsBuilder builder = new StreamsBuilder();
        builder.addStateStore(crayfishStateStore);
        return builder;
    }

    @Override
    public KStream<String, CrayfishDataBatch> inputOp(StreamsBuilder streamBuilder) {
        return streamBuilder.stream(inputDataTopic, Consumed.with(Serdes.String(), (new KafkaSerDe().getSerDe())));
    }

    @Override
    public KStream<String, CrayfishDataBatch> embeddedScoringOp(StreamsBuilder streamsBuilder,
                                                                KStream<String, CrayfishDataBatch> input) throws
                                                                                                          Exception {
        // Kafka Streams external serving example: https://github.com/kaiwaehner/kafka-streams-machine-learning-examples
        return input.transform(() -> new Transformer<String, CrayfishDataBatch, KeyValue<String, CrayfishDataBatch>>() {
            private ProcessorContext context;
            private CrayfishModel model;

            @Override
            public void init(final ProcessorContext context) {
                this.context = context;
                try {
                    model = loadModel();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public KeyValue<String, CrayfishDataBatch> transform(final String key,
                                                                 final CrayfishDataBatch crayfishDataBatch) {
                try {
                    CrayfishDataBatch result = applyModel(model, crayfishDataBatch);
                    context.forward(key, result);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            @Override
            public void close() {
            }
        }, crayfishStateStore.name());
    }

    @Override
    public KStream<String, CrayfishDataBatch> externalScoringOp(StreamsBuilder streamsBuilder,
                                                                KStream<String, CrayfishDataBatch> input) throws
                                                                                                          Exception {
        //External and Embedded use the same code in this case since the inference calls are blocking.
        return embeddedScoringOp(streamsBuilder, input);
    }

    @Override
    public CrayfishUtils.Either<KStream<String, CrayfishDataBatch>, KStream<String, CrayfishDataBatch>> outputOp(
            StreamsBuilder streamsBuilder, KStream<String, CrayfishDataBatch> output) throws Exception {
        output.to(outputTopic, Produced.with(Serdes.String(), (new KafkaSerDe().getSerDe())));
        return CrayfishUtils.Either.ofLeft(output);
    }

    @Override
    public void start(StreamsBuilder streamsBuilder, Properties streamsConfiguration,
                      CrayfishUtils.Either<KStream<String, CrayfishDataBatch>, KStream<String, CrayfishDataBatch>> out) throws
                                                                                                                        Exception {
        final KafkaStreams streams = new KafkaStreams(streamsBuilder.build(), streamsConfiguration);
        streams.start();
    }


    @Override
    public boolean hasOperatorParallelism() {
        return false;
    }

    @Override
    public CrayfishUtils.Either<KStream<String, CrayfishDataBatch>, KStream<String, CrayfishDataBatch>> setOperatorParallelism(
            CrayfishUtils.Either<KStream<String, CrayfishDataBatch>, KStream<String, CrayfishDataBatch>> operator,
            int parallelism) throws Exception {
        return null;
    }

    @Override
    public void setDefaultParallelism(StreamsBuilder streamsBuilder, Properties metaData, int parallelism) throws
                                                                                                           Exception {
        metaData.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, parallelism);

    }

    @Override
    public Properties addMetadata() throws Exception {
        final Properties streamsConfiguration = getKafkaConsumerProps();
        // ensure the application ID is unique so that the created topic has a unique name to avoid inter-experiment interference.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG,
                                 "crayfish-kafka-streams-serving-" + System.currentTimeMillis());
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.PRODUCER_PREFIX + ProducerConfig.MAX_REQUEST_SIZE_CONFIG,
                                 String.valueOf(kafkaMaxRequestSize));
        return streamsConfiguration;
    }


    private class KafkaSerDe {
        private KafkaSerDe() {}

        // TODO: make static
        public Serde<CrayfishDataBatch> getSerDe() {
            CrayfishSerializer serializer = new CrayfishSerializer();
            CrayfishDeserializer deserializer = new CrayfishDeserializer();
            return Serdes.serdeFrom(serializer, deserializer);
        }
    }

    private static class CrayfishSerializer implements Serializer<CrayfishDataBatch> {
        CrayfishDataBatchSerde serde = new CrayfishDataBatchSerde();

        @Override
        public void configure(Map<String, ?> props, boolean isKey) {
            // nothing to do
        }

        @Override
        public byte[] serialize(String topic, CrayfishDataBatch data) {
            try {
                return serde.serialize(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void close() {
            // nothing to do
        }
    }

    private class CrayfishDeserializer implements Deserializer<CrayfishDataBatch> {
        CrayfishDataBatchSerde serde = new CrayfishDataBatchSerde();

        @Override
        public void configure(Map<String, ?> props, boolean isKey) {
            // nothing to do
        }

        @Override
        public CrayfishDataBatch deserialize(String topic, byte[] bytes) {
            try {
                return serde.deserialize(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void close() {
            // nothing to do
        }
    }
}