package request;

import com.google.protobuf.ByteString;
import datatypes.datapoints.CrayfishInputData;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class InferenceGrpcRequestTorch extends InferenceRequest {
    private InferenceAPIsServiceStub stub;
    private String modelName;

    public InferenceGrpcRequestTorch(String modelName, String target) {
        super(target);
        this.modelName = modelName;
        stub = new InferenceAPIsServiceStub(getChannel());
    }

    public String makeGrpcRequest(CrayfishInputData input) throws Exception {
        String request = prepareRequest(input);
        org.pytorch.serve.grpc.inference.PredictionResponse response = stub.request(
                org.pytorch.serve.grpc.inference.PredictionsRequest.newBuilder().setModelName(modelName)
                                                                   .setModelVersion("1.0").putInput("body",
                                                                                                    ByteString.copyFrom(
                                                                                                            request.getBytes()))
                                                                   .build());
        return response.getPrediction().toString();
    }

    private String prepareRequest(CrayfishInputData inputBatch) {
        ArrayList<String> tuples = new ArrayList<>();
        for (ArrayList<Float> inList : inputBatch.get()) {
            String tp = "[" + inList.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
            tuples.add(tp);
        }
        return "[" + StringUtils.join(tuples, ",") + "]";
    }
}