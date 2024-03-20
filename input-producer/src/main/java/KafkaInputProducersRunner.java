import org.apache.commons.cli.*;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.CrayfishUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KafkaInputProducersRunner {
    private static final Logger logger = LogManager.getLogger(KafkaInputProducersRunner.class);

    public static void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
        CommandLine cmdLine = getParser(args);
        Configuration expConfig = CrayfishUtils.readConfiguration(cmdLine.getOptionValue("econf"));
        Configuration modelConfig = CrayfishUtils.readConfiguration(cmdLine.getOptionValue("mconf"));
        Configuration globalConfig = CrayfishUtils.readConfiguration(cmdLine.getOptionValue("gconf"));

        // Compute how many producers need to be spawned to achieve the desired input rate and their input rate
        int inputRate = expConfig.getInt("input_rate");
        int totalRecordsToBeGenerated = Integer.parseInt(cmdLine.getOptionValue("er"));
        int maxRatePerWorker = Integer.parseInt(cmdLine.getOptionValue("mir"));

        // increase effective input rate needed to be generated if the experiment requires bursty workloads
        boolean isBursty = expConfig.containsKey("is_bursty");
        int effectiveInputRate = (int) Math.ceil(isBursty ? 1.1 * inputRate : inputRate);

        // TODO: duplicated code; move to utils.CrayfishUtils
        int numProducers = Math.max((int) Math.ceil((float) effectiveInputRate / maxRatePerWorker), 1);
        int[] ratePerProducer = CrayfishUtils.splitIntoParts(effectiveInputRate, numProducers);
        int[] numRecordsPerProducer = CrayfishUtils.splitIntoParts(totalRecordsToBeGenerated, numProducers);

        logger.info("Total input rate = " + inputRate);
        logger.info("Total number of records = " + totalRecordsToBeGenerated);
        logger.info("Input producers = " + numProducers);
        logger.info("Input rate per producers = " + Arrays.toString(ratePerProducer));
        logger.info("Records per producer = " + Arrays.toString(numRecordsPerProducer));

        ExecutorService executor = Executors.newFixedThreadPool(numProducers);
        for (int i = 0; i < numProducers; i++) {
            int finalI = i;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        KafkaInputProducer producer = new KafkaInputProducer(ratePerProducer[finalI],
                                                                             numRecordsPerProducer[finalI],
                                                                             expConfig.getInt("batch_size"),
                                                                             Arrays
                                                                                     .stream(modelConfig.getStringArray(
                                                                                             "input.shape"))
                                                                                     .mapToInt(Integer::parseInt)
                                                                                     .toArray(),
                                                                                isBursty,
                                                                             globalConfig);

                        producer.run();
                    } catch (IOException | ConfigurationException e) {
                        logger.error(e.getMessage());
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static CommandLine getParser(String[] args) {
        Options options = new Options();

        Option gc = new Option("gconf", "global-config", true, "Global configuration file.");
        gc.setRequired(true);
        options.addOption(gc);

        Option mc = new Option("mconf", "model-config", true, "Model configuration file.");
        mc.setRequired(true);
        options.addOption(mc);

        Option ec = new Option("econf", "exp-config", true, "Experiment configuration file.");
        ec.setRequired(true);
        options.addOption(ec);

        Option er = new Option("er", "experiment-records", true, "Datapoints generated for experiment.");
        er.setRequired(true);
        options.addOption(er);

        Option mir = new Option("mir", "max-input-rate", true, "Maximum input rate per producer.");
        mir.setRequired(true);
        options.addOption(mir);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("KafkaInputProducerRunner", options);
            System.exit(1);
        }
        return cmd;
    }
}
