import datatypes.datapoints.CrayfishDataBatch;
import org.apache.commons.cli.*;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import serde.data.CrayfishDataBatchSerde;
import utils.CrayfishUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumer {
    private static final Logger logger = LogManager.getLogger(KafkaConsumer.class);

    public static void main(String[] args) throws ConfigurationException {
        CommandLine parser = getParser(args);

        Configuration expConfig = CrayfishUtils.readConfiguration(parser.getOptionValue("econf"));
        Configuration globalConfig = CrayfishUtils.readConfiguration(parser.getOptionValue("gconf"));
        Configuration modelConfig = CrayfishUtils.readConfiguration(parser.getOptionValue("mconf"));
        boolean isTaskParallel = parser.hasOption("tp") && Boolean.parseBoolean(parser.getOptionValue("tp"));

        int totalRecordsToWaitFor = Integer.parseInt(parser.getOptionValue("er"));
        logger.info("Waiting for " + totalRecordsToWaitFor + " experiment datapoints..");

        Properties props = new Properties();
        props.setProperty("bootstrap.servers", globalConfig.getString("kafka.bootstrap.servers"));
        props.setProperty("group.id", "KafkaOutputConsumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        if (globalConfig.containsKey("kafka.auth.username") && globalConfig.containsKey("kafka.auth.password")) {
            String username = globalConfig.getString("kafka.auth.username");
            String password = globalConfig.getString("kafka.auth.password");
            String jaasConf =
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + username +
                    "\" password=\"" + password + "\";";
            props.setProperty("sasl.jaas.config", jaasConf);
            props.setProperty("security.protocol", "SASL_PLAINTEXT");
            props.setProperty("sasl.mechanism", "PLAIN");
        }

        String kafkaMaxRequestSize = globalConfig.getString("kafka.max.req.size");
        props.setProperty("fetch.message.max.bytes", kafkaMaxRequestSize);
        props.setProperty("max.partition.fetch.bytes", kafkaMaxRequestSize);

        org.apache.kafka.clients.consumer.KafkaConsumer<String, CrayfishDataBatch> consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(
                props, new StringDeserializer(), new CrayfishDeserializer());

        consumer.subscribe(Collections.singletonList(globalConfig.getString("kafka.output.topic")));

        // Build experiment file name
        String experimentFootprint = buildExperimentFootprint(expConfig);
        String path = "./" + globalConfig.getString("out.dir") + "/" + parser.getOptionValue("sp");
        if (isTaskParallel) path += "-32-N-32";
        path += "/" + modelConfig.getString("model.name") + "/" + parser.getOptionValue("mf");
        File directory = new File(path);
        if (!directory.exists()) directory.mkdirs();

        long startTime = System.currentTimeMillis();
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(directory.getPath() + "/" + experimentFootprint + ".csv"),
                                       "utf-8"))) {
            while (true) {
                ConsumerRecords<String, CrayfishDataBatch> records = consumer.poll(Duration.ofMillis(5000));
                for (ConsumerRecord<String, CrayfishDataBatch> record : records) {
                    if (totalRecordsToWaitFor == 0) {
                        return;
                    } else {
                        totalRecordsToWaitFor--;
                        if (record.value().getPredictions().get() != null) // filter failed requests
                            writer.write(record.value().getCreationTimestamp() + ", " + record.timestamp() + "\n");

                    }
                }
                writer.flush();
                // TODO: move timeout to configs file
                if (totalRecordsToWaitFor == 0 || System.currentTimeMillis() - startTime > 900000) {
                    return;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildExperimentFootprint(Configuration expConfig) throws ConfigurationException {
        String inputRate = expConfig.getString("input_rate");
        if (Integer.parseInt(inputRate) < 0) inputRate = "1";
        String batchSize = expConfig.getString("batch_size");
        String modelReplicas = expConfig.getString("model_replicas");
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(Calendar.getInstance().getTime());
        return timeStamp + "-ir" + inputRate + "-bs" + batchSize + "-mr" + modelReplicas;
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

        Option er = new Option("er", "experiment-records", true, "Datapoints generated for experiment.");
        er.setRequired(true);
        options.addOption(er);

        Option tp = new Option("tp", "task-parallel", true, "Task parallel execution. Valid only for Flink pipelines.");
        tp.setRequired(false);
        options.addOption(tp);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }
        return cmd;
    }

    private static class CrayfishDeserializer implements Deserializer<CrayfishDataBatch> {
        CrayfishDataBatchSerde serde = new CrayfishDataBatchSerde();

        @Override
        public CrayfishDataBatch deserialize(String s, byte[] bytes) {
            try {

                return serde.deserialize(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
