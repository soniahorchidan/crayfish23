package datatypes.datapoints;

import java.io.Serializable;
import java.util.ArrayList;

public class CrayfishInputData implements Serializable {
    private ArrayList<ArrayList<Float>> datapointBatch;

    public CrayfishInputData(ArrayList<ArrayList<Float>> datapointBatch) {
        this.datapointBatch = datapointBatch;
    }

    public ArrayList<ArrayList<Float>> get() {
        return datapointBatch;
    }

    @Override
    public String toString() {
        return "DataBatch{" +
               "datapointBatch=" + datapointBatch +
               '}';
    }
}
