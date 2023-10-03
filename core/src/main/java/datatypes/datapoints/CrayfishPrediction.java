package datatypes.datapoints;

import java.io.Serializable;
import java.util.ArrayList;

public class CrayfishPrediction implements Serializable {
    private final String prediction;

    public CrayfishPrediction(ArrayList<ArrayList<Float>> prediction) {
        this.prediction = prediction.toString();
    }

    public CrayfishPrediction(String prediction) {
        this.prediction = prediction;
    }

    public String get() {
        return prediction;
    }

    @Override
    public String toString() {
        return "Prediction{" +
               "prediction=" + prediction +
               '}';
    }
}