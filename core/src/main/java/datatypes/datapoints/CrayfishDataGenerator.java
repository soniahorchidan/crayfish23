package datatypes.datapoints;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SplittableRandom;

/**
 * Generates images of arbitrary size.
 */
public class CrayfishDataGenerator implements Iterator<CrayfishDataBatch>, Serializable {
    private static final Logger logger = LogManager.getLogger(CrayfishDataGenerator.class);
    private static final SplittableRandom rand = new SplittableRandom();

    private final int batchSize;
    private final int[] dataPointShape;
    private int numRecords;

    /**
     * @param batchSize  the number of datapoints required per batch. The datatypes.datapoints.CrayfishDataGenerator will return one batch at a time.
     * @param inputSize  the shape of the generated datapoint.
     * @param numRecords number of datapoints to generate.
     */
    public CrayfishDataGenerator(int batchSize, int[] inputSize, int numRecords) {
        this.batchSize = batchSize;
        this.dataPointShape = inputSize;
        this.numRecords = numRecords;
    }

    /**
     * Check if the experiment is completed (i.e., number of datapoint have been generated). If not, returns true so
     * that a new batch can be generated. Otherwise returns false so that the experiment can finish.
     *
     * @return true is the experiment should still be running, false otherwise.
     */
    @Override
    public boolean hasNext() {
        if (numRecords < 0) return false;
        return true;
    }

    /**
     * Generates a new batch of random datapoints.
     *
     * @return the generated batch.
     */
    @Override
    public CrayfishDataBatch next() {
        ArrayList<ArrayList<Float>> batch = new ArrayList<>();
        int valuesNum = 1;
        for (int i : dataPointShape)
            valuesNum *= i;
        for (int datapointNum = 0; datapointNum < this.batchSize; datapointNum++) {
            ArrayList<Float> newDatapoint = new ArrayList<>();
            for (int i = 0; i < valuesNum; i++) {
                newDatapoint.add((float) rand.nextDouble());
            }
            batch.add(newDatapoint);
        }
        numRecords--;
        return new CrayfishDataBatch(batch);
    }
}
