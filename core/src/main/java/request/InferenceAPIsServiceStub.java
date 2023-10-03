package request;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.ClientCalls;

import static io.grpc.MethodDescriptor.generateFullMethodName;

// TODO: Torch only?
public class InferenceAPIsServiceStub extends AbstractStub<InferenceAPIsServiceStub> {

    public static final String SERVICE_NAME = "org.pytorch.serve.grpc.inference.InferenceAPIsService";
    public static final String METHOD_NAME = "Predictions";
    public static final MethodDescriptor<org.pytorch.serve.grpc.inference.PredictionsRequest, org.pytorch.serve.grpc.inference.PredictionResponse> METHOD_DESCRIPTOR = MethodDescriptor.create(
            MethodDescriptor.MethodType.UNARY, generateFullMethodName(SERVICE_NAME, METHOD_NAME),
            ProtoUtils.marshaller(org.pytorch.serve.grpc.inference.PredictionsRequest.getDefaultInstance()),
            ProtoUtils.marshaller(org.pytorch.serve.grpc.inference.PredictionResponse.getDefaultInstance()));


    public InferenceAPIsServiceStub(Channel channel) {
        super(channel);
    }

    private InferenceAPIsServiceStub(Channel channel, CallOptions callOptions) {
        super(channel, callOptions);
    }

    @Override
    protected InferenceAPIsServiceStub build(Channel channel, CallOptions callOptions) {
        return new InferenceAPIsServiceStub(channel, callOptions);
    }

    public org.pytorch.serve.grpc.inference.PredictionResponse request(
            org.pytorch.serve.grpc.inference.PredictionsRequest request) {
        return ClientCalls.blockingUnaryCall(getChannel(), METHOD_DESCRIPTOR, getCallOptions(), request);
    }
}