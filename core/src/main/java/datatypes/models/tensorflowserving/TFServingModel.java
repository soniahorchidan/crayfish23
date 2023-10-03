package datatypes.models.tensorflowserving;

import datatypes.datapoints.CrayfishInputData;
import datatypes.datapoints.CrayfishPrediction;
import datatypes.models.CrayfishModel;
import request.InferenceGrpcRequestTfServing;
import request.InferenceRequest;

import java.io.Serializable;

public class TFServingModel extends CrayfishModel implements Serializable {

    private InferenceRequest inferenceRequest;

    @Override
    public void loadModel(String modelName, String location) throws Exception {
        this.inferenceRequest = new InferenceGrpcRequestTfServing(modelName, location);
    }

    @Override
    public void build() throws Exception {}

    @Override
    public CrayfishPrediction apply(CrayfishInputData input) throws Exception {
        if (input == null || input.get() == null) return null;

        String result = this.inferenceRequest.makeGrpcRequest(input);
        return new CrayfishPrediction(result);
    }
}