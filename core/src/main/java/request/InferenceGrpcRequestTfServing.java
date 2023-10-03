package request;

import datatypes.datapoints.CrayfishInputData;
import org.apache.commons.lang3.StringUtils;
import org.tensorflow.framework.TensorProto;
import org.tensorflow.framework.TensorShapeProto;
import tensorflow.serving.Model;
import tensorflow.serving.Predict;
import tensorflow.serving.PredictionServiceGrpc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class InferenceGrpcRequestTfServing extends InferenceRequest implements Serializable {
    private PredictionServiceGrpc.PredictionServiceBlockingStub stub;
    private String modelName;

    // TODO
    private String inputName;

    public InferenceGrpcRequestTfServing(String modelName, String target) {
        super(target);
        this.modelName = modelName;
        stub = PredictionServiceGrpc.newBlockingStub(getChannel());
    }

    @Override
    public String makeGrpcRequest(CrayfishInputData input) throws Exception {
        // https://github.com/zafeisn/JavaClient
        // https://github.com/philosophus/tf-serving-demo/blob/master/model/model.py
        ArrayList<ArrayList<Float>> in = input.get();
        int batchSize = in.size();
        int dataSize = in.get(0).size();

        TensorShapeProto shape = null;
        // TODO: refactor; separate request builder for each model since they expect different input shapes
        if (modelName.equals("ffnn")) {
            TensorShapeProto.Dim dim1 = TensorShapeProto.Dim.newBuilder().setSize(batchSize).build();
            TensorShapeProto.Dim dim2 = TensorShapeProto.Dim.newBuilder().setSize(dataSize).build();
            shape = TensorShapeProto.newBuilder().addDim(dim1).addDim(dim2).build();
        } else if (modelName.equals("resnet50")) {
            TensorShapeProto.Dim dim1 = TensorShapeProto.Dim.newBuilder().setSize(batchSize).build();
            TensorShapeProto.Dim dim2 = TensorShapeProto.Dim.newBuilder().setSize(224).build();
            TensorShapeProto.Dim dim3 = TensorShapeProto.Dim.newBuilder().setSize(224).build();
            TensorShapeProto.Dim dim4 = TensorShapeProto.Dim.newBuilder().setSize(3).build();
            shape = TensorShapeProto.newBuilder().addDim(dim1).addDim(dim2).addDim(dim3).addDim(dim4).build();
        }

        TensorProto.Builder proto = TensorProto.newBuilder().setTensorShape(shape)
                                               .setDtype(org.tensorflow.framework.DataType.DT_FLOAT);
        for (int i = 0; i < batchSize; i++)
            proto.addAllFloatVal(in.get(i));

        Predict.PredictResponse response = stub.predict(Predict.PredictRequest.newBuilder().setModelSpec(
                Model.ModelSpec.newBuilder().setName(modelName).build()).putInputs("input_1", proto.build()).build());
        return response.getOutputsMap().toString();
    }

    private String buildRequest(CrayfishInputData inputBatch) {
        ArrayList<String> tuples = new ArrayList<>();
        for (ArrayList<Float> inList : inputBatch.get()) {
            String tp = "[" + inList.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
            tuples.add(tp);
        }
        return "{\"instances\": [" + StringUtils.join(tuples, ",") + "]}";
    }
}