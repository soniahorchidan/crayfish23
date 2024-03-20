import datatypes.models.dl4j.DL4JModel;
import datatypes.models.nd4j.ND4JModel;
import datatypes.models.onnx.ONNXModel;
import datatypes.models.tensorflowsavedmodel.TFSavedModel;
import datatypes.models.tensorflowserving.TFServingModel;
import datatypes.models.torchserve.TorchServeModel;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.CrayfishUtils;


/**
 * Entry point to run one experiment.
 */
public class ExperimentDriver {
    private static final Logger logger = LogManager.getLogger(ExperimentDriver.class);
    private static CommandLine parser;

    public static void main(String[] args) throws Exception {
        parser = getParser(args);
        run(parser.getOptionValue("sp"), parser.getOptionValue("mf"), parser.getOptionValue("mconf"),
            parser.getOptionValue("gconf"), parser.getOptionValue("econf"));
    }

    public static void run(String streamProcessor, String modelFormat, String modelConfigPath,
                           String globalExpConfigPath, String experimentConfigPath) throws Exception {
        switch (streamProcessor) {
            case "flink": {
                runFlinkAdapter(modelFormat, modelConfigPath, globalExpConfigPath, experimentConfigPath);
                break;
            }
            case "kafkastreams": {
                runKafkaStreamsAdapter(modelFormat, modelConfigPath, globalExpConfigPath, experimentConfigPath);
                break;
            }
            case "sparkss": {
                runSparkSSAdapter(modelFormat, modelConfigPath, globalExpConfigPath, experimentConfigPath);
                break;
            }
            default: {
                throw new RuntimeException(
                        "Unknown stream processor. Please use the Crayfish API to provide your own implementation.");
            }
        }
    }

    private static void runFlinkAdapter(String modelFormat, String modelConfigPath, String globalExpConfigPath,
                                        String experimentConfigPath) throws Exception {
        org.apache.commons.configuration2.Configuration modelConfig = CrayfishUtils.readConfiguration(modelConfigPath);
        String modelName = modelConfig.getString("model.name");
        Crayfish adapter = null;
        boolean isTaskParallel = parser.hasOption("tp") && Boolean.parseBoolean(parser.getOptionValue("tp"));

        switch (modelFormat) {
            case "nd4j": {
                String modelEndpoint = modelConfig.getString("model.path.nd4j");
                adapter = new FlinkCrayfishAdapter<ND4JModel>(ND4JModel.class, modelName, modelEndpoint,
                                                              globalExpConfigPath, experimentConfigPath, false,
                                                              isTaskParallel);
                break;
            }
            case "dl4j": {
                String modelEndpoint = modelConfig.getString("model.path.dl4j");
                adapter = new FlinkCrayfishAdapter<DL4JModel>(DL4JModel.class, modelName, modelEndpoint,
                                                              globalExpConfigPath, experimentConfigPath, false,
                                                              isTaskParallel);
                break;
            }
            case "onnx": {
                String modelEndpoint = modelConfig.getString("model.path.onnx");
                adapter = new FlinkCrayfishAdapter<ONNXModel>(ONNXModel.class, modelName, modelEndpoint,
                                                              globalExpConfigPath, experimentConfigPath, false,
                                                              isTaskParallel);
                break;
            }
            case "tf-savedmodel": {
                String modelEndpoint = modelConfig.getString("model.path.tf-savedmodel");
                adapter = new FlinkCrayfishAdapter<TFSavedModel>(TFSavedModel.class, modelName, modelEndpoint,
                                                                 globalExpConfigPath, experimentConfigPath, false,
                                                                 isTaskParallel);
                break;
            }
            case "torchserve": {
                String modelEndpoint = modelConfig.getString("model.path.torchserve");
                adapter = new FlinkCrayfishAdapter<TorchServeModel>(TorchServeModel.class, modelName, modelEndpoint,
                                                                    globalExpConfigPath, experimentConfigPath, false,
                                                                    isTaskParallel);
                break;
            }
            case "tf-serving": {
                String modelEndpoint = modelConfig.getString("model.path.tf-serving");
                adapter = new FlinkCrayfishAdapter<TFServingModel>(TFServingModel.class, modelName, modelEndpoint,
                                                                   globalExpConfigPath, experimentConfigPath, false,
                                                                   isTaskParallel);

                break;
            }
            case "no-op": {
                adapter = new NoOpFlinkAdapter(globalExpConfigPath, experimentConfigPath);
                break;
            }
            default: {
                logger.error(
                        "Adapter not implemented! Please implement the Flink adapter for Crayfish to serve models in the " +
                        modelFormat + " format.");
            }
        }
        if (adapter != null) {
            adapter.run();
        }
    }

    private static void runKafkaStreamsAdapter(String modelFormat, String modelConfigPath, String globalExpConfigPath,
                                               String experimentConfigPath) throws Exception {
        org.apache.commons.configuration2.Configuration modelConfig = CrayfishUtils.readConfiguration(modelConfigPath);
        String modelName = modelConfig.getString("model.name");
        Crayfish adapter = null;

        switch (modelFormat) {
            case "nd4j": {
                String modelEndpoint = modelConfig.getString("model.path.nd4j");
                adapter = new KafkaStreamsCrayfishAdapter<ND4JModel>(ND4JModel.class, modelName, modelEndpoint,
                                                                     globalExpConfigPath, experimentConfigPath, false);
                break;
            }
            case "dl4j": {
                String modelEndpoint = modelConfig.getString("model.path.dl4j");
                adapter = new KafkaStreamsCrayfishAdapter<DL4JModel>(DL4JModel.class, modelName, modelEndpoint,
                                                                     globalExpConfigPath, experimentConfigPath, false);
                break;
            }
            case "onnx": {
                String modelEndpoint = modelConfig.getString("model.path.onnx");
                adapter = new KafkaStreamsCrayfishAdapter<ONNXModel>(ONNXModel.class, modelName, modelEndpoint,
                                                                     globalExpConfigPath, experimentConfigPath, false);
                break;
            }
            case "tf-savedmodel": {
                String modelEndpoint = modelConfig.getString("model.path.tf-savedmodel");
                adapter = new KafkaStreamsCrayfishAdapter<TFSavedModel>(TFSavedModel.class, modelName, modelEndpoint,
                                                                        globalExpConfigPath, experimentConfigPath,
                                                                        false);
                break;
            }
            case "torchserve": {
                String modelEndpoint = modelConfig.getString("model.path.torchserve");
                adapter = new KafkaStreamsCrayfishAdapter<TorchServeModel>(TorchServeModel.class, modelName,
                                                                           modelEndpoint, globalExpConfigPath,
                                                                           experimentConfigPath, false);
                break;
            }
            case "tf-serving": {
                String modelEndpoint = modelConfig.getString("model.path.tf-serving");
                adapter = new KafkaStreamsCrayfishAdapter<TFServingModel>(TFServingModel.class, modelName,
                                                                          modelEndpoint, globalExpConfigPath,
                                                                          experimentConfigPath, false);

                break;
            }
            default: {
                logger.error(
                        "Adapter not implemented! Please implement the Kafka Streams adapter for Crayfish to serve models in the " +
                        modelFormat + " format.");
            }
        }
        if (adapter != null) {
            adapter.run();
        }
    }

    private static void runSparkSSAdapter(String modelFormat, String modelConfigPath, String globalExpConfigPath,
                                          String experimentConfigPath) throws Exception {
        org.apache.commons.configuration2.Configuration modelConfig = CrayfishUtils.readConfiguration(modelConfigPath);
        String modelName = modelConfig.getString("model.name");
        Crayfish adapter = null;

        switch (modelFormat) {
            case "nd4j": {
                String modelEndpoint = modelConfig.getString("model.path.nd4j");
                adapter = new SparkSSCrayfishAdapter<ND4JModel>(ND4JModel.class, modelName, modelEndpoint,
                                                                globalExpConfigPath, experimentConfigPath, false);
                break;
            }
            case "dl4j": {
                String modelEndpoint = modelConfig.getString("model.path.dl4j");
                adapter = new SparkSSCrayfishAdapter<DL4JModel>(DL4JModel.class, modelName, modelEndpoint,
                                                                globalExpConfigPath, experimentConfigPath, false);
                break;
            }
            case "onnx": {
                String modelEndpoint = modelConfig.getString("model.path.onnx");
                adapter = new SparkSSCrayfishAdapter<ONNXModel>(ONNXModel.class, modelName, modelEndpoint,
                                                                globalExpConfigPath, experimentConfigPath, false);
                break;
            }
            case "tf-savedmodel": {
                String modelEndpoint = modelConfig.getString("model.path.tf-savedmodel");
                adapter = new SparkSSCrayfishAdapter<TFSavedModel>(TFSavedModel.class, modelName, modelEndpoint,
                                                                   globalExpConfigPath, experimentConfigPath, false);
                break;
            }
            case "torchserve": {
                String modelEndpoint = modelConfig.getString("model.path.torchserve");
                adapter = new SparkSSCrayfishAdapter<TorchServeModel>(TorchServeModel.class, modelName, modelEndpoint,
                                                                      globalExpConfigPath, experimentConfigPath, false);
                break;
            }
            case "tf-serving": {
                String modelEndpoint = modelConfig.getString("model.path.tf-serving");
                adapter = new SparkSSCrayfishAdapter<TFServingModel>(TFServingModel.class, modelName, modelEndpoint,
                                                                     globalExpConfigPath, experimentConfigPath, false);

                break;
            }
            default: {
                logger.error(
                        "Adapter not implemented! Please implement the Spark Structured Streaming adapter for Crayfish to serve models in the " +
                        modelFormat + " format.");
            }
        }
        if (adapter != null) {
            adapter.run();
        }
    }

    private static CommandLine getParser(String[] args) {
        Options options = new Options();

        Option sp = new Option("sp", "stream-processor", true, "Stream Processor (e.g., flink)");
        sp.setRequired(true);
        options.addOption(sp);

        Option mf = new Option("mf", "model-format", true, "Model format (e.g., nf4j)");
        mf.setRequired(true);
        options.addOption(mf);

        Option mconf = new Option("mconf", "model-config", true, "Model configuration file.");
        mconf.setRequired(true);
        options.addOption(mconf);

        Option gc = new Option("gconf", "global-config", true, "Global configuration file for all experiments.");
        gc.setRequired(true);
        options.addOption(gc);

        Option ec = new Option("econf", "exp-config", true, "Experiment configuration file.");
        ec.setRequired(true);
        options.addOption(ec);

        Option tp = new Option("tp", "task-parallel", true, "Whether to run task-parallel exec.");
        tp.setRequired(false);
        options.addOption(tp);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ExperimentDriver", options);
            System.exit(1);
        }
        return cmd;
    }
}