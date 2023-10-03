import datatypes.models.dl4j.DL4JModel;
import datatypes.models.nd4j.ND4JModel;
import datatypes.models.onnx.ONNXModel;
import datatypes.models.tensorflowsavedmodel.TFSavedModel;
import datatypes.models.tensorflowserving.TFServingModel;
import datatypes.models.torchserve.TorchServeModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.CrayfishUtils;
import config.CrayfishConfig;


/**
 * Entry point to run one experiment.
 */
public class ExperimentDriver {
    private static final Logger logger = LogManager.getLogger(ExperimentDriver.class);

    public static void main(String[] args) throws Exception {
        CrayfishConfig config = new CrayfishConfig(args);
        run(config);
    }

    public static void run(CrayfishConfig config) throws Exception {
        switch (config.getString("sp")) {
            case "flink": {
                runFlinkAdapter(config);
                break;
            }
            case "kafkastreams": {
                runKafkaStreamsAdapter(config);
                break;
            }
            case "sparkss": {
                runSparkSSAdapter(config);
                break;
            }
            default: {
                throw new RuntimeException(
                        "Unknown stream processor. Please use the Crayfish API to provide your own implementation.");
            }
        }
    }

    private static void runFlinkAdapter(CrayfishConfig config) throws Exception {
        Crayfish adapter = null;

        switch (config.getString("mf")) {
            case "nd4j": {
                adapter = new FlinkCrayfishAdapter<ND4JModel>(ND4JModel.class, config);
                break;
            }
            case "dl4j": {
                adapter = new FlinkCrayfishAdapter<DL4JModel>(DL4JModel.class, config);
                break;
            }
            case "onnx": {
                adapter = new FlinkCrayfishAdapter<ONNXModel>(ONNXModel.class, config);
                break;
            }
            case "tf-savedmodel": {
                adapter = new FlinkCrayfishAdapter<TFSavedModel>(TFSavedModel.class, config);
                break;
            }
            case "torchserve": {
                adapter = new FlinkCrayfishAdapter<TorchServeModel>(TorchServeModel.class, config);
                break;
            }
            case "tf-serving": {
                adapter = new FlinkCrayfishAdapter<TFServingModel>(TFServingModel.class, config);
                break;
            }
            case "no-op": {
                adapter = new NoOpFlinkAdapter(config);
                break;
            }
            default: {
                logger.error(
                        "Adapter not implemented! Please implement the Flink adapter for Crayfish to serve models in the " +
                        config.getString("mf") + " format.");
            }
        }
        if (adapter != null) {
            adapter.run();
        }
    }

    private static void runKafkaStreamsAdapter(CrayfishConfig config) throws Exception {
        Crayfish adapter = null;

        switch (config.getString("mf")) {
            case "nd4j": {
                adapter = new KafkaStreamsCrayfishAdapter<ND4JModel>(ND4JModel.class, config);
                break;
            }
            case "dl4j": {
                adapter = new KafkaStreamsCrayfishAdapter<DL4JModel>(DL4JModel.class, config);
                break;
            }
            case "onnx": {
                adapter = new KafkaStreamsCrayfishAdapter<ONNXModel>(ONNXModel.class, config);
                break;
            }
            case "tf-savedmodel": {
                adapter = new KafkaStreamsCrayfishAdapter<TFSavedModel>(TFSavedModel.class, config);
                break;
            }
            case "torchserve": {
                adapter = new KafkaStreamsCrayfishAdapter<TorchServeModel>(TorchServeModel.class, config);
                break;
            }
            case "tf-serving": {
                adapter = new KafkaStreamsCrayfishAdapter<TFServingModel>(TFServingModel.class, config);
                break;
            }
            default: {
                logger.error(
                        "Adapter not implemented! Please implement the Kafka Streams adapter for Crayfish to serve models in the " +
                        config.getString("mf") + " format.");
            }
        }
        if (adapter != null) {
            adapter.run();
        }
    }

    private static void runSparkSSAdapter(CrayfishConfig config) throws Exception {
        Crayfish adapter = null;

        switch (config.getString("mf")) {
            case "nd4j": {
                adapter = new SparkSSCrayfishAdapter<ND4JModel>(ND4JModel.class, config);
                break;
            }
            case "dl4j": {
                adapter = new SparkSSCrayfishAdapter<DL4JModel>(DL4JModel.class, config);
                break;
            }
            case "onnx": {
                adapter = new SparkSSCrayfishAdapter<ONNXModel>(ONNXModel.class, config);
                break;
            }
            case "tf-savedmodel": {
                adapter = new SparkSSCrayfishAdapter<TFSavedModel>(TFSavedModel.class, config);
                break;
            }
            case "torchserve": {
                adapter = new SparkSSCrayfishAdapter<TorchServeModel>(TorchServeModel.class, config);
                break;
            }
            case "tf-serving": {
                adapter = new SparkSSCrayfishAdapter<TFServingModel>(TFServingModel.class, config);
                break;
            }
            default: {
                logger.error(
                        "Adapter not implemented! Please implement the Spark Structured Streaming adapter for Crayfish to serve models in the " +
                        config.getString("mf") + " format.");
            }
        }
        if (adapter != null) {
            adapter.run();
        }
    }
}