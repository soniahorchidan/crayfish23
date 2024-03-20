import datatypes.datapoints.CrayfishDataBatch;
import datatypes.models.CrayfishModel;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.DataStreamReader;
import org.apache.spark.sql.streaming.DataStreamWriter;
import serde.data.CrayfishDataBatchSerde;
import org.apache.spark.sql.streaming.Trigger;
import utils.CrayfishUtils;

import java.io.Serializable;
import java.util.Properties;

public class SparkSSCrayfishAdapter<M extends CrayfishModel>
        extends Crayfish<SparkSession, Dataset<CrayfishDataBatch>, DataStreamWriter<Row>, M> implements Serializable {
    private static final Logger logger = LogManager.getLogger(SparkSSCrayfishAdapter.class);

    public SparkSSCrayfishAdapter(Class<M> modelClass, String modelName, String modelEndpoint, String globalConfigPath,
                                  String experimentConfigPath, boolean isExternal) throws ConfigurationException {
        super(modelClass, modelName, modelEndpoint, globalConfigPath, experimentConfigPath, isExternal);
    }

    @Override
    public SparkSession streamBuilder() {
        // TODO: config files!
        SparkConf sparkConf = new SparkConf().setAppName("SparkSSCrayfishAdapter")
                                             .set("spark.sql.streaming.checkpointLocation", "/tmp/checkpoint")
                                             .set("spark.streaming.receiver.writeAheadLog.enable", "false")
                                             .set("spark.sql.shuffle.partitions", String.valueOf(parallelism))
                                             .set("spark.default.parallelism", String.valueOf(parallelism));
        SparkSession spark = SparkSession.builder().config(sparkConf).getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");
        return spark;
    }

    @Override
    public Dataset<CrayfishDataBatch> inputOp(SparkSession streamBuilder) {
        // Define the Kafka source
        DataStreamReader reader = streamBuilder.readStream().format("kafka")
                                               .option("kafka.bootstrap.servers", bootstrapServer)
                                               .option("subscribe", inputDataTopic)
                                               .option("startingOffsets", "earliest").option("failOnDataLoss", "false");
        Properties props = getKafkaConsumerProps();
        reader.option("kafka.fetch.message.max.bytes", 52428800)
              .option("kafka.max.partition.fetch.bytes", 52428800);
        if (!kafkaAuthUsername.isEmpty() & !kafkaAuthPassword.isEmpty()) {
            reader.option("kafka.sasl.jaas.config", props.getProperty("sasl.jaas.config"))
                  .option("kafka.sasl.mechanism", props.getProperty("sasl.mechanism"))
                  .option("kafka.security.protocol", props.getProperty("security.protocol"));

        }

        Dataset<Row> kafkaSource = reader.load();
        // Parse the JSON objects using the custom deserializer and convert them to CrayfishDataBatch objects
        return kafkaSource.selectExpr("CAST(value AS STRING)").map((MapFunction<Row, CrayfishDataBatch>) row -> {
            CrayfishDataBatchSerde serde = new CrayfishDataBatchSerde();
            String jsonString = row.getString(0);
            return serde.deserialize(jsonString.getBytes());
        }, Encoders.javaSerialization(CrayfishDataBatch.class));
    }

    @Override
    public Dataset<CrayfishDataBatch> embeddedScoringOp(SparkSession s, Dataset<CrayfishDataBatch> input) throws
                                                                                                          Exception {
        CrayfishModel model = loadModel();
        Broadcast<CrayfishModel> broadcastModel = s.sparkContext().broadcast(model,
                                                                             scala.reflect.ClassManifestFactory.fromClass(
                                                                                     CrayfishModel.class));

        // Apply a mapping function on each CrayfishDataBatch object
        return input.map(new MapFunction<CrayfishDataBatch, CrayfishDataBatch>() {
            @Override
            public CrayfishDataBatch call(CrayfishDataBatch crayfishDataBatch) throws Exception {
                return applyModel(broadcastModel.value(), crayfishDataBatch);
            }
        }, Encoders.javaSerialization(CrayfishDataBatch.class));
    }

    @Override
    public Dataset<CrayfishDataBatch> externalScoringOp(SparkSession s, Dataset<CrayfishDataBatch> input) throws
                                                                                                          Exception {
        return embeddedScoringOp(s, input);
    }

    @Override
    public CrayfishUtils.Either<Dataset<CrayfishDataBatch>, DataStreamWriter<Row>> outputOp(SparkSession sparkSession,
                                                                                            Dataset<CrayfishDataBatch> output) throws
                                                                                                                               Exception {

        // Convert the CrayfishDataBatch objects to JSON using your custom function and write them to another Kafka topic
        Dataset<Row> jsonOutput = output.map((MapFunction<CrayfishDataBatch, byte[]>) crayfishDataBatch -> {
            CrayfishDataBatchSerde serde = new CrayfishDataBatchSerde();
            return serde.serialize(crayfishDataBatch);
        }, Encoders.BINARY()).toDF();

        DataStreamWriter<Row> writer = jsonOutput.writeStream().format("kafka").trigger(Trigger.ProcessingTime("1 millisecond"))
                                                 .option("kafka.bootstrap.servers", bootstrapServer)
                                                 .option("topic", outputTopic).option("failOnDataLoss", "false");

        Properties props = getKafkaProducerProps();
        writer.option("kafka.max.request.size", 52428800);

        if (!kafkaAuthUsername.isEmpty() & !kafkaAuthPassword.isEmpty()) {
            writer.option("kafka.sasl.jaas.config", props.getProperty("sasl.jaas.config"))
                  .option("kafka.sasl.mechanism", props.getProperty("sasl.mechanism"))
                  .option("kafka.security.protocol", props.getProperty("security.protocol"));
        }

        return new CrayfishUtils.Either.Right<>(writer);
    }

    @Override
    public void start(SparkSession sparkSession, Properties metaData,
                      CrayfishUtils.Either<Dataset<CrayfishDataBatch>, DataStreamWriter<Row>> out) throws Exception {
        out.rightOrElse(null).start().awaitTermination();
    }

    @Override
    public boolean hasOperatorParallelism() {
        return false;
    }

    @Override
    public CrayfishUtils.Either<Dataset<CrayfishDataBatch>, DataStreamWriter<Row>> setOperatorParallelism(
            CrayfishUtils.Either<Dataset<CrayfishDataBatch>, DataStreamWriter<Row>> operator, int parallelism) throws
                                                                                                               Exception {
        return null;
    }

    @Override
    public void setDefaultParallelism(SparkSession sparkSession, Properties metaData, int parallelism) throws
                                                                                                       Exception {
        //sparkSession.conf().set("spark.default.parallelism", parallelism);
        //sparkSession.conf().set("spark.cores.max", parallelism);
        //sparkSession.conf().set("spark.sql.shuffle.partitions", 1);
    }

    @Override
    public Properties addMetadata() throws Exception {
        return null;
    }
}