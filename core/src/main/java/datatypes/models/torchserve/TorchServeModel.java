package datatypes.models.torchserve;

import datatypes.datapoints.CrayfishInputData;
import datatypes.datapoints.CrayfishPrediction;
import datatypes.models.CrayfishModel;
import request.InferenceGrpcRequestTorch;
import request.InferenceRequest;

import java.io.Serializable;

public class TorchServeModel extends CrayfishModel implements Serializable {
    private InferenceRequest inferenceRequest;

    @Override
    public void loadModel(String modelName, String location) throws Exception {
        this.inferenceRequest = new InferenceGrpcRequestTorch(modelName, location);
    }

    @Override
    public void build() throws Exception {}

    @Override
    public CrayfishPrediction apply(CrayfishInputData input) throws Exception {
        if (input == null || input.get() == null) return null;
        //String data = buildRequest(input);
        String result = this.inferenceRequest.makeGrpcRequest(input);
        return new CrayfishPrediction(result);
    }
}