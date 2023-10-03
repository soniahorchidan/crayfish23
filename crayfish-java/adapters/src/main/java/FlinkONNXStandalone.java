import datatypes.datapoints.CrayfishDataBatch;
import datatypes.datapoints.CrayfishDataGenerator;
import datatypes.datapoints.CrayfishPrediction;
import datatypes.datapoints.ThrottledIterator;
import datatypes.models.onnx.ONNXModel;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.Encoder;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.CrayfishUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class FlinkONNXStandalone {
    private static final String CONFIG_NAME = "model.path.onnx";
    private static final Logger logger = LogManager.getLogger(FlinkONNXStandalone.class);


    public static void run(String globalConfigPath, String modelConfigPath, String experimentConfigPath,
                           int totalRecordsToBeGenerated) throws Exception {
        // CONFIGS
        org.apache.commons.configuration2.Configuration expConfig = CrayfishUtils.readConfiguration(
                experimentConfigPath);
        int inputRate = expConfig.getInt("input_rate");
        int batchSize = expConfig.getInt("batch_size");
        int scoringParallelism = expConfig.getInt("model_replicas");

        System.out.println("DEBUG:: INPUT RATE=" + inputRate);
        org.apache.commons.configuration2.Configuration modelConfig = CrayfishUtils.readConfiguration(modelConfigPath);
        String modelEndpoint = modelConfig.getString(CONFIG_NAME);
        String modelName = modelConfig.getString("model.name");
        int[] inputSize = Arrays.stream(modelConfig.getStringArray("input.shape")).mapToInt(Integer::parseInt)
                                .toArray();

        org.apache.commons.configuration2.Configuration globalConfig = CrayfishUtils.readConfiguration(
                globalConfigPath);
        int sourceSinkParallelism = globalConfig.getInt("kafka.input.data.partitions.num");

        int maxRatePerWorker = globalConfig.getInt("max.input.req.per.worker");
        int numProducers =  Math.max((int) Math.ceil((float) inputRate / maxRatePerWorker), 1);
        int[] ratePerProducer = CrayfishUtils.splitIntoParts(inputRate, numProducers);
        int[] numRecordsPerProducer = CrayfishUtils.splitIntoParts(totalRecordsToBeGenerated, numProducers);

        logger.info("Total input rate = " + inputRate);
        logger.info("Total number of records = " + totalRecordsToBeGenerated);
        logger.info("Input producers = " + numProducers);
        logger.info("Input rate per producers = " + Arrays.toString(ratePerProducer));
        logger.info("Records per producer = " + Arrays.toString(numRecordsPerProducer));

        // DAG
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<CrayfishDataBatch> batches = env
                .addSource(new StandaloneSource(ratePerProducer[0], numRecordsPerProducer[0], batchSize, inputSize))
                .setParallelism(numProducers).disableChaining();

        DataStream<CrayfishDataBatch> scored = batches.map(new RichMapFunction<CrayfishDataBatch, CrayfishDataBatch>() {

            private ONNXModel model;

            @Override
            public void open(Configuration parameters) throws Exception {
                model = new ONNXModel();
                model.loadModel(modelName, modelEndpoint);
                model.build();
                logger.info("Loaded model " + model);
            }

            @Override
            public CrayfishDataBatch map(CrayfishDataBatch crayfishDataBatch) throws Exception {
                CrayfishPrediction result = model.apply(crayfishDataBatch.getDatapointBatch());
                //logger.debug("Scored datapoint crated at timestamp " + crayfishDataBatch.getCreationTimestamp());
                crayfishDataBatch.setPredictions(result);
                return crayfishDataBatch;
            }
        }).setParallelism(scoringParallelism).disableChaining();

        org.apache.flink.core.fs.Path path = new Path("./results-standalone");
        String filePrefix = buildExperimentFootprint(expConfig);
        final StreamingFileSink<CrayfishDataBatch> sink = StreamingFileSink
                .forRowFormat(path, new Encoder<CrayfishDataBatch>() {
                    @Override
                    public void encode(CrayfishDataBatch crayfishDataBatch, OutputStream outputStream) throws
                                                                                                       IOException {
                        long start = crayfishDataBatch.getCreationTimestamp();
                        long end = System.currentTimeMillis();
                        String measurement = start + ", " + end + "\n";
                        outputStream.write(measurement.getBytes());
                    }
                }).withRollingPolicy(
                        DefaultRollingPolicy.builder().withInactivityInterval(TimeUnit.MINUTES.toMillis(500)).build())
                .withOutputFileConfig(OutputFileConfig.builder().withPartPrefix(filePrefix).build()).build();

        scored.addSink(sink).setParallelism(sourceSinkParallelism);

        env.execute();
    }

    // TODO move in utils
    private static String buildExperimentFootprint(org.apache.commons.configuration2.Configuration expConfig) throws
                                                                                                              ConfigurationException {
        String inputRate = expConfig.getString("input_rate");
        String batchSize = expConfig.getString("batch_size");
        String modelReplicas = expConfig.getString("model_replicas");
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(Calendar.getInstance().getTime());
        return timeStamp + "-" + "ir" + inputRate + "-bs" + batchSize + "-mr" + modelReplicas;
    }

    private static class StandaloneSource extends RichParallelSourceFunction<CrayfishDataBatch> {
        private final CrayfishDataGenerator generator;
        private ThrottledIterator<CrayfishDataBatch> throttledIterator;

        public StandaloneSource(int inputRate, int numRecords, int batchSize, int[] inputSize) {
            generator = new CrayfishDataGenerator(batchSize, inputSize, numRecords);
            // TODO(user): the datatypes.datapoints.ThrottledIterator works differently than the Guava RateLimitter; cannot generate less than 1 record per second
            this.throttledIterator = new ThrottledIterator<>(generator, inputRate);
        }

        @Override
        public void run(SourceContext<CrayfishDataBatch> sourceContext) throws Exception {
            while (this.throttledIterator.hasNext()) {
                sourceContext.collect(this.throttledIterator.next());
            }
        }

        @Override
        public void cancel() {}
    }
}