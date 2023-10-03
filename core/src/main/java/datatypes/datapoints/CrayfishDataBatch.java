package datatypes.datapoints;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Contains a batch of input datapoints. Can also contain the model scorings.
 */
public class CrayfishDataBatch implements Serializable {
    private final CrayfishInputData datapointBatch;
    private final long creationTimestamp;
    private CrayfishPrediction predictions;

    public CrayfishDataBatch(ArrayList<ArrayList<Float>> datapointBatch) {
        this.datapointBatch = new CrayfishInputData(datapointBatch);
        this.creationTimestamp = System.currentTimeMillis();
    }

    public CrayfishDataBatch(ArrayList<ArrayList<Float>> datapointBatch, long creationTimestamp,
                             ArrayList<ArrayList<Float>> predictions) {
        this.datapointBatch = new CrayfishInputData(datapointBatch);
        this.creationTimestamp = creationTimestamp;
        this.predictions = new CrayfishPrediction(predictions);
    }

    public CrayfishDataBatch(ArrayList<ArrayList<Float>> datapointBatch, long creationTimestamp, String predictions) {
        this.datapointBatch = new CrayfishInputData(datapointBatch);
        this.creationTimestamp = creationTimestamp;
        this.predictions = new CrayfishPrediction(predictions);
    }

    public CrayfishDataBatch(CrayfishInputData datapointBatch, long creationTimestamp, CrayfishPrediction predictions) {
        this.datapointBatch = datapointBatch;
        this.creationTimestamp = creationTimestamp;
        this.predictions = predictions;
    }

    public void setPredictions(ArrayList<ArrayList<Float>> predictions) {
        this.predictions = new CrayfishPrediction(predictions);
    }

    public void setPredictions(CrayfishPrediction predictions) {
        this.predictions = predictions;
    }

    public void setPredictions(String predictions) {
        this.predictions = new CrayfishPrediction(predictions);
    }

    public CrayfishInputData getDatapointBatch() {
        return datapointBatch;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public CrayfishPrediction getPredictions() {
        return predictions;
    }

    @Override
    public String toString() {
        return "CrayfishBatch{" + "datapointBatch=" + datapointBatch + ", predictions=" + predictions +
               ", creationTimestamp=" + creationTimestamp + '}';
    }
}