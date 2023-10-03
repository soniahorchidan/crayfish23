import datatypes.datapoints.CrayfishDataBatch;
import datatypes.datapoints.CrayfishPrediction;
import datatypes.models.CrayfishModel;
import org.apache.kafka.clients.producer.ProducerConfig;
import utils.CrayfishUtils;
import config.CrayfishConfig;

import java.io.Serializable;
import java.util.Properties;

/**
 * @param <SB> Stream Builder
 * @param <OP> Operator Type
 * @param <SK> Sink Type
 * @param <M>  Model Type
 */
public abstract class Crayfish<SB, OP, SK, M extends CrayfishModel> implements Serializable {
    protected Class<M> mClass;
    protected CrayfishConfig config;
    private final boolean isExternal;
    protected String bootstrapServer;
    protected String inputDataTopic;
    protected String outputTopic;
    protected String kafkaAuthUsername;
    protected String kafkaAuthPassword;
    protected int kafkaPartitionsNum;
    protected int kafkaMaxRequestSize;
    protected int parallelism;
    protected String modelName;
    protected String modelEndpoint;

    public Crayfish(Class<M> modelClass, String modelName, String modelEndpoint,
                    CrayfishConfig config, boolean isExternal) {
        this.mClass = modelClass;
        this.config = config;
        this.isExternal = isExternal;

        // Kafka configs
        bootstrapServer = config.getString("ce");
        inputDataTopic = config.getString("in");
        outputTopic = config.getString("out");
        if (config.hasOption("user") && config.hasOption("pswd")) {
            kafkaAuthUsername = config.getString("user");
            kafkaAuthPassword = config.getString("pswd");
        } else {
            kafkaAuthUsername = "";
            kafkaAuthPassword = "";
        }
        kafkaPartitionsNum = config.getInt("ip");
        kafkaMaxRequestSize = config.getInt("rs");

        // experiment configs
        parallelism = config.getInt("mr");
        this.modelName = modelName;
        this.modelEndpoint = modelEndpoint;

        // debug
        System.out.println("Input topic: " + inputDataTopic);
        System.out.println("Output topic: " + outputTopic);
        System.out.println("Bootstrap Server: " + bootstrapServer);
    }

    public Crayfish(Class<M> modelClass, CrayfishConfig config, boolean isExternal) {
        this(modelClass, config.getModelName(), config.getModelEndpoint(), config, isExternal);
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

    public void run() throws Exception {
        // build DAG
        Properties metaData = addMetadata();

        SB sb = streamBuilder();
        OP input = inputOp(sb);
        OP scoring = null;
        if (isExternal) {
            scoring = externalScoringOp(sb, input);
        } else {
            scoring = embeddedScoringOp(sb, input);
        }
        CrayfishUtils.Either<OP, SK> output = outputOp(sb, scoring);

        // set parallelisms
        if (hasOperatorParallelism()) {
            setOperatorParallelism(new CrayfishUtils.Either.Left<>(input), kafkaPartitionsNum);
            setOperatorParallelism(new CrayfishUtils.Either.Left<>(scoring), parallelism);
            setOperatorParallelism(output, kafkaPartitionsNum);
        } else {
            setDefaultParallelism(sb, metaData, parallelism);
        }
        start(sb, metaData, output);
    }

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
        properties.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, kafkaMaxRequestSize);
        return properties;
    }

    public CrayfishModel loadModel() throws Exception {
        CrayfishModel model = mClass.getDeclaredConstructor().newInstance();
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

}