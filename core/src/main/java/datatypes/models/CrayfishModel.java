package datatypes.models;

import datatypes.datapoints.CrayfishInputData;
import datatypes.datapoints.CrayfishPrediction;

public abstract class CrayfishModel {

    public CrayfishModel() {}

    /**
     * Build the model from a byte array.
     *
     * @param location path where the model is located.
     * @throws Exception
     */
    public abstract void loadModel(String modelName, String location) throws Exception;

    public abstract void build() throws Exception;

    /**
     * Applies the model on the data point received from the input stream.
     *
     * @param input the input data point or batch.
     * @return the inference result.
     * @throws Exception
     */
    public abstract CrayfishPrediction apply(CrayfishInputData input) throws Exception;
}