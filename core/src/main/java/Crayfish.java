import datatypes.datapoints.CrayfishDataBatch;
import datatypes.datapoints.CrayfishPrediction;
import datatypes.models.CrayfishModel;
import org.apache.commons.configuration2.ex.ConfigurationException;
import utils.CrayfishUtils;

import java.io.Serializable;
import java.util.Properties;

/**
 * @param <SB> Stream Builder
 * @param <OP> Operator Type
 * @param <SK> Sink Type
 * @param <M>  Model Type
 */
public abstract class Crayfish<SB, OP, SK, M extends CrayfishModel> implements Serializable {
    protected String bootstrapServer;
    protected String inputDataTopic;
    protected String outputTopic;
    protected String modelEndpoint;
    protected String modelName;
    protected int parallelism;
    protected int kafkaPartitionsNum;
    protected String kafkaAuthUsername;
    protected String kafkaAuthPassword;
    protected int kafkaMaxRequestSize;
    protected Class<M> mClass;

    private final boolean isExternal;
    protected boolean isTaskParallel = false;

    public Crayfish(Class<M> modelClass, String modelName, String modelEndpoint, String globalConfigPath,
                    String experimentConfigPath, boolean isExternal) throws ConfigurationException {
        this.mClass = modelClass;

        // Kafka configs
        org.apache.commons.configuration2.Configuration globalConfig = CrayfishUtils.readConfiguration(
                globalConfigPath);
        bootstrapServer = globalConfig.getString("kafka.bootstrap.servers");
        inputDataTopic = globalConfig.getString("kafka.input.data.topic");
        outputTopic = globalConfig.getString("kafka.output.topic");
        if (globalConfig.containsKey("kafka.auth.username") && globalConfig.containsKey("kafka.auth.password")) {
            kafkaAuthUsername = globalConfig.getString("kafka.auth.username");
            kafkaAuthPassword = globalConfig.getString("kafka.auth.password");
        } else {
            kafkaAuthUsername = "";
            kafkaAuthPassword = "";
        }
        kafkaMaxRequestSize = globalConfig.getInt("kafka.max.req.size");
        kafkaPartitionsNum = globalConfig.getInt("kafka.input.data.partitions.num");

        // experiment configs
        this.modelEndpoint = modelEndpoint;
        this.modelName = modelName;
        org.apache.commons.configuration2.Configuration experimentConfig = CrayfishUtils.readConfiguration(
                experimentConfigPath);
        parallelism = experimentConfig.getInt("model_replicas");
        this.isExternal = isExternal;
    }

    public Crayfish(Class<M> modelClass, String modelName, String modelEndpoint, String globalConfigPath,
                    String experimentConfigPath, boolean isExternal, boolean isTaskParallel) throws
                                                                                             ConfigurationException {
        this(modelClass, modelName, modelEndpoint, globalConfigPath, experimentConfigPath, isExternal);
        this.isTaskParallel = isTaskParallel;
    }

    public abstract SB streamBuilder();

    public abstract OP inputOp(SB streamBuilder);

    public abstract OP embeddedScoringOp(SB sb, OP input) throws Exception;

    public abstract OP externalScoringOp(SB sb, OP input) throws Exception;

    public abstract CrayfishUtils.Either<OP, SK> outputOp(SB sb, OP output) throws Exception;

    public abstract void start(SB sb, Properties metaData, CrayfishUtils.Either<OP, SK> out) throws Exception;

    public abstract boolean hasOperatorParallelism();

    public abstract CrayfishUtils.Either<OP, SK> setOperatorParallelism(CrayfishUtils.Either<OP, SK> operator,
                                                                        int parallelism) throws Exception;

    public abstract void setDefaultParallelism(SB sb, Properties metaData, int parallelism) throws Exception;

    public abstract Properties addMetadata() throws Exception;

    public Properties getKafkaConsumerProps() {
        Properties properties = new Properties();
        setDefaultKafkaProps(properties);
        properties.setProperty("fetch.message.max.bytes", String.valueOf(kafkaMaxRequestSize));
        properties.setProperty("max.partition.fetch.bytes", String.valueOf(kafkaMaxRequestSize));
        return properties;
    }

    public Properties getKafkaProducerProps() {
        Properties properties = new Properties();
        setDefaultKafkaProps(properties);
        properties.put("max.request.size", kafkaMaxRequestSize);
        return properties;
    }

    public CrayfishModel loadModel() throws Exception {
        CrayfishModel model = mClass.newInstance();
        model.loadModel(modelName, modelEndpoint);
        model.build();
        return model;
    }

    public CrayfishDataBatch applyModel(CrayfishModel model, CrayfishDataBatch input) throws Exception {
        CrayfishPrediction result = model.apply(input.getDatapointBatch());
        input.setPredictions(result);
        return input;
    }

    private void setDefaultKafkaProps(Properties properties) {
        properties.put("bootstrap.servers", bootstrapServer);
        if (!kafkaAuthUsername.isEmpty() & !kafkaAuthPassword.isEmpty()) {
            String jaasConf =
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + kafkaAuthUsername +
                    "\" password=\"" + kafkaAuthPassword + "\";";
            properties.setProperty("sasl.jaas.config", jaasConf);
            properties.setProperty("security.protocol", "SASL_PLAINTEXT");
            properties.setProperty("sasl.mechanism", "PLAIN");
        }
    }

    public void run() throws Exception {
        Properties metaData = addMetadata();

        SB sb = streamBuilder();
        if (!hasOperatorParallelism() || !isTaskParallel) setDefaultParallelism(sb, metaData, parallelism);

        OP input = inputOp(sb);
        CrayfishUtils.Either<OP, SK> in = new CrayfishUtils.Either.Left<>(input);
        if (hasOperatorParallelism() && isTaskParallel) in = setOperatorParallelism(in, kafkaPartitionsNum);

        OP scoring;
        if (isExternal) scoring = externalScoringOp(sb, in.leftOrElse(null));
        else scoring = embeddedScoringOp(sb, in.leftOrElse(null));
        CrayfishUtils.Either<OP, SK> sc = new CrayfishUtils.Either.Left<>(scoring);
        if (hasOperatorParallelism() && isTaskParallel) sc = setOperatorParallelism(sc, parallelism);

        CrayfishUtils.Either<OP, SK> output = outputOp(sb, sc.leftOrElse(null));
        if (hasOperatorParallelism() && isTaskParallel) output = setOperatorParallelism(output, kafkaPartitionsNum);

        start(sb, metaData, output);
    }
}
