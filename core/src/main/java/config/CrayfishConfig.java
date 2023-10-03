package config;

import org.apache.commons.cli.*;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import utils.CrayfishUtils;

import java.util.Arrays;

/**
 * Pack all the parameters from config files into CrayfishConfig object
 */
public class CrayfishConfig {
    CommandLine cmd;
    Configuration modelConfig;

    public CrayfishConfig(String[] args) throws ConfigurationException {
        this.cmd = getCmd(args);
        this.modelConfig = CrayfishUtils.readConfiguration(getString("mconf"));
    }

    public String getString(String opt) {
        return this.cmd.getOptionValue(opt);
    }

    public int getInt(String opt) {
        return Integer.parseInt(getString(opt));
    }

    public boolean hasOption(String opt) {
        return this.cmd.hasOption(opt);
    }

    public String getModelEndpoint() {
        String modelEndpoint = modelConfig.getString("model.path." + getString("mf"));
        return modelEndpoint;
    }

    public String getModelName() {
        String modelName = modelConfig.getString("model.name");
        return modelName;
    }

    public int[] getModelInputShape() {
        int[] modelInputShape = Arrays.stream(modelConfig.getStringArray("input.shape"))
                                      .mapToInt(Integer::parseInt)
                                      .toArray();
        return modelInputShape;
    }

    private Option createOption(String option, String longOption, String description, boolean required) {
        // create a required option with argument
        Option op = new Option(option, longOption, true, description);
        op.setRequired(required);
        return op;
    }

    private CommandLine getCmd(String[] args) {
        Options options = new Options();

        options.addOption(createOption("sp", "stream-processor", "Stream Processor (e.g., flink)", true));
        options.addOption(createOption("mf", "model-format", "Model format (e.g., nf4j)", true));
        options.addOption(createOption("mn", "model-name", "Model name (e.g., ffnn)", true));
        options.addOption(createOption("mconf", "model-config", "Model configuration file", true));
        options.addOption(createOption("in", "kafka-input-topic", "Name of the Kafka input topic", true));
        options.addOption(createOption("out", "kafka-output-topic", "Name of the Kafka output topic", true));
        options.addOption(createOption("bs", "exp-batch-size", "Batch size", true));
        options.addOption(createOption("mr", "exp-model-replicas", "Number of replication for the model", true));
        options.addOption(createOption("ir", "exp-input-rate", "Input rate", true));
        options.addOption(createOption("ac", "exp-async-op-capacity", "Async op capacity", true));
        options.addOption(createOption("rcd", "exp-records-num", "Number of records per experiment", true));
        options.addOption(createOption("dir", "glo-out-dir", "Path to results directory", true));
        options.addOption(createOption("req", "glo-max-input-req-per-worker", "Maximum input requests per worker", true));
        options.addOption(createOption("rs", "glo-kafka-max-req-size", "Maximum size of a request", true));
        options.addOption(createOption("mtopic", "glo-kafka-input-models-topic", "", true));
        options.addOption(createOption("mtimeout", "glo-kafka-input-models-timeout", "", true));
        options.addOption(createOption("ip", "glo-kafka-input-data-partitions-num", "Number of partitions for the input topic", true));
        options.addOption(createOption("op", "glo-kafka-output-data-partitions-num", "Number of partitions for the output topic", true));
        options.addOption(createOption("ce", "glo-kafka-client-endpoint", "Endpoint (HOST:PORT) of the server to which input-producer, data-processor, output-consumer connect", true));
        options.addOption(createOption("user", "glo-kafka-auth-user", "Username to access Kafka server (only used when run on cluster)", false));
        options.addOption(createOption("pswd", "glo-kafka-auth-pass", "Password to access Kafka server (only used when run on cluster)", false));

        CommandLine cmd = null;
        CommandLineParser cmdParser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            cmd = cmdParser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("All options", options);
            System.exit(1);
        }
        return cmd;
    }
}