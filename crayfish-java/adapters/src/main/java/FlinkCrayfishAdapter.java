import datatypes.datapoints.CrayfishDataBatch;
import datatypes.models.CrayfishModel;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import serde.data.CrayfishDataBatchSerde;
import utils.CrayfishUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class FlinkCrayfishAdapter<M extends CrayfishModel> extends
                                                           Crayfish<StreamExecutionEnvironment, SingleOutputStreamOperator<CrayfishDataBatch>, DataStreamSink<CrayfishDataBatch>, M>
        implements Serializable {
    private static final Logger logger = LogManager.getLogger(FlinkCrayfishAdapter.class);
    private final int asyncOperatorTimeout;
    private final int asyncOperatorCapacity;

    public FlinkCrayfishAdapter(Class<M> modelClass, String modelName, String modelEndpoint, String globalConfigPath,
                                String experimentConfigPath, boolean isExternal, boolean isTaskParallel) throws ConfigurationException {
        super(modelClass, modelName, modelEndpoint, globalConfigPath, experimentConfigPath, isExternal, isTaskParallel);
        org.apache.commons.configuration2.Configuration experimentConfig = CrayfishUtils.readConfiguration(
                experimentConfigPath);
        if (experimentConfig.containsKey("async_op_capacity")) {
            asyncOperatorCapacity = experimentConfig.getInt("async_op_capacity");
        } else {
            asyncOperatorCapacity = 500000;
        }
        asyncOperatorTimeout = 500000;
    }


    @Override
    public StreamExecutionEnvironment streamBuilder() {
        return StreamExecutionEnvironment.getExecutionEnvironment();
    }

    @Override
    public SingleOutputStreamOperator<CrayfishDataBatch> inputOp(StreamExecutionEnvironment streamBuilder) {
        Properties properties = getKafkaConsumerProps();
        KafkaSource<CrayfishDataBatch> source = KafkaSource.<CrayfishDataBatch>builder()
                                                           .setTopics(inputDataTopic) // TODO: set in method above?
                                                           .setProperties(properties)
                                                           .setStartingOffsets(OffsetsInitializer.earliest())
                                                           .setDeserializer(new CrayfishDataPointDeserializer())
                                                           .build();
        return streamBuilder.fromSource(source, WatermarkStrategy.noWatermarks(), "Source");
    }

    @Override
    public SingleOutputStreamOperator<CrayfishDataBatch> embeddedScoringOp(
            StreamExecutionEnvironment streamExecutionEnvironment,
            SingleOutputStreamOperator<CrayfishDataBatch> input) throws Exception {
        return input.map(new RichMapFunction<CrayfishDataBatch, CrayfishDataBatch>() {
            private CrayfishModel model;

            @Override
            public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
                model = loadModel();
            }

            @Override
            public CrayfishDataBatch map(CrayfishDataBatch crayfishDataBatch) throws Exception {
                return applyModel(model, crayfishDataBatch);
            }
        });
    }

    @Override
    public SingleOutputStreamOperator<CrayfishDataBatch> externalScoringOp(
            StreamExecutionEnvironment streamExecutionEnvironment,
            SingleOutputStreamOperator<CrayfishDataBatch> input) throws Exception {
        return AsyncDataStream.orderedWait(input, new RichAsyncFunction<CrayfishDataBatch, CrayfishDataBatch>() {
            private CrayfishModel model;

            @Override
            public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
                model = loadModel();
            }

            @Override
            public void asyncInvoke(CrayfishDataBatch crayfishDataBatch,
                                    ResultFuture<CrayfishDataBatch> resultFuture) throws Exception {
                CrayfishDataBatch result = applyModel(model, crayfishDataBatch);
                resultFuture.complete(Collections.singleton(result));
            }
        }, asyncOperatorTimeout, TimeUnit.MILLISECONDS, asyncOperatorCapacity);
    }

    public CrayfishUtils.Either<SingleOutputStreamOperator<CrayfishDataBatch>, DataStreamSink<CrayfishDataBatch>> outputOp(
            StreamExecutionEnvironment streamExecutionEnvironment,
            SingleOutputStreamOperator<CrayfishDataBatch> output) throws Exception {
        // Add Kafka sink
        Properties properties = getKafkaProducerProps();
        KafkaSink<CrayfishDataBatch> sink = KafkaSink.<CrayfishDataBatch>builder().setKafkaProducerConfig(properties)
                                                     .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                                                                                                        .setTopic(
                                                                                                                this.outputTopic)
                                                                                                        .setValueSerializationSchema(
                                                                                                                new CrayfishDataPointSerializer())
                                                                                                        .build())
                                                     .build();
        return new CrayfishUtils.Either.Right<>(output.sinkTo(sink));
    }

    @Override
    public void start(StreamExecutionEnvironment streamExecutionEnvironment, Properties metaData,
                      CrayfishUtils.Either<SingleOutputStreamOperator<CrayfishDataBatch>, DataStreamSink<CrayfishDataBatch>> out) throws
                                                                                                                                  Exception {
        streamExecutionEnvironment.execute();

    }

    @Override
    public boolean hasOperatorParallelism() {
        return true;
    }

    @Override
    public CrayfishUtils.Either<SingleOutputStreamOperator<CrayfishDataBatch>, DataStreamSink<CrayfishDataBatch>> setOperatorParallelism(
            CrayfishUtils.Either<SingleOutputStreamOperator<CrayfishDataBatch>, DataStreamSink<CrayfishDataBatch>> operator,
            int parallelism) throws Exception {
        SingleOutputStreamOperator<CrayfishDataBatch> op = operator.leftOrElse(null);
        if (op != null) return new CrayfishUtils.Either.Left<>(op.setParallelism(parallelism).disableChaining());
        DataStreamSink<CrayfishDataBatch> op2 = operator.rightOrElse(null);
        if (op2 != null) return new CrayfishUtils.Either.Right<>(op2.setParallelism(parallelism));
        return null;
    }

    @Override
    public void setDefaultParallelism(StreamExecutionEnvironment streamExecutionEnvironment, Properties metaData,
                                      int parallelism) throws Exception {
        streamExecutionEnvironment.getConfig().setParallelism(parallelism);
    }

    @Override
    public Properties addMetadata() throws Exception {
        return null;
    }

    private static class CrayfishDataPointDeserializer implements KafkaRecordDeserializationSchema<CrayfishDataBatch> {
        private CrayfishDataBatchSerde serde = new CrayfishDataBatchSerde();

        @Override
        public TypeInformation<CrayfishDataBatch> getProducedType() {
            return TypeInformation.of(new TypeHint<CrayfishDataBatch>() {
                @Override
                public TypeInformation<CrayfishDataBatch> getTypeInfo() {
                    return super.getTypeInfo();
                }
            });
        }

        @Override
        public void deserialize(ConsumerRecord<byte[], byte[]> consumerRecord,
                                Collector<CrayfishDataBatch> collector) throws IOException {
            collector.collect(serde.deserialize(consumerRecord.value()));
        }
    }

    private static class CrayfishDataPointSerializer implements SerializationSchema<CrayfishDataBatch> {
        CrayfishDataBatchSerde serde = new CrayfishDataBatchSerde();

        @Override
        public byte[] serialize(CrayfishDataBatch crayfishDataBatch) {
            try {
                return serde.serialize(crayfishDataBatch);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
