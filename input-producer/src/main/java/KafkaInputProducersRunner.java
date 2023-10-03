import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.configuration2.ex.ConfigurationException;
import utils.CrayfishUtils;
import config.CrayfishConfig;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KafkaInputProducersRunner {
    private static final Logger logger = LogManager.getLogger(KafkaInputProducersRunner.class);

    public static void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
        CrayfishConfig config = new CrayfishConfig(args);

        // Compute how many producers need to be spawned to achieve the desired input rate and their input rate
        int inputRate = config.getInt("ir");
        int maxRatePerWorker = config.getInt("req");
        int totalRecordsToBeGenerated = config.getInt("rcd");

        // TODO(user): duplicated code; move to utils.CrayfishUtils
        int numProducers =  Math.max((int) Math.ceil((float) inputRate / maxRatePerWorker), 1);
        int[] ratePerProducer = CrayfishUtils.splitIntoParts(inputRate, numProducers);
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
                                                                             config);

                        producer.run();
                    } catch (IOException e) {
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
}
