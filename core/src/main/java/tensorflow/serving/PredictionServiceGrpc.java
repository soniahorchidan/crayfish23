package tensorflow.serving;

import java.io.Serializable;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;

public class PredictionServiceGrpc {

    private PredictionServiceGrpc() {}

    public static final String SERVICE_NAME = "tensorflow.serving.PredictionService";

    // Static method descriptors that strictly reflect the proto.
    public static final io.grpc.MethodDescriptor<tensorflow.serving.Classification.ClassificationRequest, tensorflow.serving.Classification.ClassificationResponse> METHOD_CLASSIFY = io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            generateFullMethodName("tensorflow.serving.PredictionService", "Classify"),
            io.grpc.protobuf.ProtoUtils.marshaller(
                    tensorflow.serving.Classification.ClassificationRequest.getDefaultInstance()),
            io.grpc.protobuf.ProtoUtils.marshaller(
                    tensorflow.serving.Classification.ClassificationResponse.getDefaultInstance()));

    public static final io.grpc.MethodDescriptor<tensorflow.serving.RegressionOuterClass.RegressionRequest, tensorflow.serving.RegressionOuterClass.RegressionResponse> METHOD_REGRESS = io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            generateFullMethodName("tensorflow.serving.PredictionService", "Regress"),
            io.grpc.protobuf.ProtoUtils.marshaller(
                    tensorflow.serving.RegressionOuterClass.RegressionRequest.getDefaultInstance()),
            io.grpc.protobuf.ProtoUtils.marshaller(
                    tensorflow.serving.RegressionOuterClass.RegressionResponse.getDefaultInstance()));

    public static final io.grpc.MethodDescriptor<tensorflow.serving.Predict.PredictRequest, tensorflow.serving.Predict.PredictResponse> METHOD_PREDICT = io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            generateFullMethodName("tensorflow.serving.PredictionService", "Predict"),
            io.grpc.protobuf.ProtoUtils.marshaller(tensorflow.serving.Predict.PredictRequest.getDefaultInstance()),
            io.grpc.protobuf.ProtoUtils.marshaller(tensorflow.serving.Predict.PredictResponse.getDefaultInstance()));

    public static final io.grpc.MethodDescriptor<tensorflow.serving.GetModelMetadata.GetModelMetadataRequest, tensorflow.serving.GetModelMetadata.GetModelMetadataResponse> METHOD_GET_MODEL_METADATA = io.grpc.MethodDescriptor.create(
            io.grpc.MethodDescriptor.MethodType.UNARY,
            generateFullMethodName("tensorflow.serving.PredictionService", "GetModelMetadata"),
            io.grpc.protobuf.ProtoUtils.marshaller(
                    tensorflow.serving.GetModelMetadata.GetModelMetadataRequest.getDefaultInstance()),
            io.grpc.protobuf.ProtoUtils.marshaller(
                    tensorflow.serving.GetModelMetadata.GetModelMetadataResponse.getDefaultInstance()));

    public static PredictionServiceStub newStub(io.grpc.Channel channel) {
        return new PredictionServiceStub(channel);
    }

    public static PredictionServiceBlockingStub newBlockingStub(io.grpc.Channel channel) {
        return new PredictionServiceBlockingStub(channel);
    }

    public static PredictionServiceFutureStub newFutureStub(io.grpc.Channel channel) {
        return new PredictionServiceFutureStub(channel);
    }

    public static interface PredictionService {

        public void classify(tensorflow.serving.Classification.ClassificationRequest request,
                             io.grpc.stub.StreamObserver<tensorflow.serving.Classification.ClassificationResponse> responseObserver);

        public void regress(tensorflow.serving.RegressionOuterClass.RegressionRequest request,
                            io.grpc.stub.StreamObserver<tensorflow.serving.RegressionOuterClass.RegressionResponse> responseObserver);

        public void predict(tensorflow.serving.Predict.PredictRequest request,
                            io.grpc.stub.StreamObserver<tensorflow.serving.Predict.PredictResponse> responseObserver);

        public void getModelMetadata(tensorflow.serving.GetModelMetadata.GetModelMetadataRequest request,
                                     io.grpc.stub.StreamObserver<tensorflow.serving.GetModelMetadata.GetModelMetadataResponse> responseObserver);
    }

    public static interface PredictionServiceBlockingClient {

        public tensorflow.serving.Classification.ClassificationResponse classify(
                tensorflow.serving.Classification.ClassificationRequest request);

        public tensorflow.serving.RegressionOuterClass.RegressionResponse regress(
                tensorflow.serving.RegressionOuterClass.RegressionRequest request);

        public tensorflow.serving.Predict.PredictResponse predict(tensorflow.serving.Predict.PredictRequest request);

        public tensorflow.serving.GetModelMetadata.GetModelMetadataResponse getModelMetadata(
                tensorflow.serving.GetModelMetadata.GetModelMetadataRequest request);
    }

    public static interface PredictionServiceFutureClient {

        public com.google.common.util.concurrent.ListenableFuture<tensorflow.serving.Classification.ClassificationResponse> classify(
                tensorflow.serving.Classification.ClassificationRequest request);

        public com.google.common.util.concurrent.ListenableFuture<tensorflow.serving.RegressionOuterClass.RegressionResponse> regress(
                tensorflow.serving.RegressionOuterClass.RegressionRequest request);

        public com.google.common.util.concurrent.ListenableFuture<tensorflow.serving.Predict.PredictResponse> predict(
                tensorflow.serving.Predict.PredictRequest request);

        public com.google.common.util.concurrent.ListenableFuture<tensorflow.serving.GetModelMetadata.GetModelMetadataResponse> getModelMetadata(
                tensorflow.serving.GetModelMetadata.GetModelMetadataRequest request);
    }

    public static class PredictionServiceStub extends io.grpc.stub.AbstractStub<PredictionServiceStub>
            implements PredictionService {
        private PredictionServiceStub(io.grpc.Channel channel) {
            super(channel);
        }

        private PredictionServiceStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected PredictionServiceStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new PredictionServiceStub(channel, callOptions);
        }

        @java.lang.Override
        public void classify(tensorflow.serving.Classification.ClassificationRequest request,
                             io.grpc.stub.StreamObserver<tensorflow.serving.Classification.ClassificationResponse> responseObserver) {
            asyncUnaryCall(getChannel().newCall(METHOD_CLASSIFY, getCallOptions()), request, responseObserver);
        }

        @java.lang.Override
        public void regress(tensorflow.serving.RegressionOuterClass.RegressionRequest request,
                            io.grpc.stub.StreamObserver<tensorflow.serving.RegressionOuterClass.RegressionResponse> responseObserver) {
            asyncUnaryCall(getChannel().newCall(METHOD_REGRESS, getCallOptions()), request, responseObserver);
        }

        @java.lang.Override
        public void predict(tensorflow.serving.Predict.PredictRequest request,
                            io.grpc.stub.StreamObserver<tensorflow.serving.Predict.PredictResponse> responseObserver) {
            asyncUnaryCall(getChannel().newCall(METHOD_PREDICT, getCallOptions()), request, responseObserver);
        }

        @java.lang.Override
        public void getModelMetadata(tensorflow.serving.GetModelMetadata.GetModelMetadataRequest request,
                                     io.grpc.stub.StreamObserver<tensorflow.serving.GetModelMetadata.GetModelMetadataResponse> responseObserver) {
            asyncUnaryCall(getChannel().newCall(METHOD_GET_MODEL_METADATA, getCallOptions()), request,
                           responseObserver);
        }
    }

    public static class PredictionServiceBlockingStub extends io.grpc.stub.AbstractStub<PredictionServiceBlockingStub>
            implements PredictionServiceBlockingClient, Serializable {
        private PredictionServiceBlockingStub(io.grpc.Channel channel) {
            super(channel);
        }

        private PredictionServiceBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected PredictionServiceBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new PredictionServiceBlockingStub(channel, callOptions);
        }

        @java.lang.Override
        public tensorflow.serving.Classification.ClassificationResponse classify(
                tensorflow.serving.Classification.ClassificationRequest request) {
            return blockingUnaryCall(getChannel().newCall(METHOD_CLASSIFY, getCallOptions()), request);
        }

        @java.lang.Override
        public tensorflow.serving.RegressionOuterClass.RegressionResponse regress(
                tensorflow.serving.RegressionOuterClass.RegressionRequest request) {
            return blockingUnaryCall(getChannel().newCall(METHOD_REGRESS, getCallOptions()), request);
        }

        @java.lang.Override
        public tensorflow.serving.Predict.PredictResponse predict(tensorflow.serving.Predict.PredictRequest request) {
            return blockingUnaryCall(getChannel().newCall(METHOD_PREDICT, getCallOptions()), request);
        }

        @java.lang.Override
        public tensorflow.serving.GetModelMetadata.GetModelMetadataResponse getModelMetadata(
                tensorflow.serving.GetModelMetadata.GetModelMetadataRequest request) {
            return blockingUnaryCall(getChannel().newCall(METHOD_GET_MODEL_METADATA, getCallOptions()), request);
        }
    }

    public static class PredictionServiceFutureStub extends io.grpc.stub.AbstractStub<PredictionServiceFutureStub>
            implements PredictionServiceFutureClient {
        private PredictionServiceFutureStub(io.grpc.Channel channel) {
            super(channel);
        }

        private PredictionServiceFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected PredictionServiceFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new PredictionServiceFutureStub(channel, callOptions);
        }

        @java.lang.Override
        public com.google.common.util.concurrent.ListenableFuture<tensorflow.serving.Classification.ClassificationResponse> classify(
                tensorflow.serving.Classification.ClassificationRequest request) {
            return futureUnaryCall(getChannel().newCall(METHOD_CLASSIFY, getCallOptions()), request);
        }

        @java.lang.Override
        public com.google.common.util.concurrent.ListenableFuture<tensorflow.serving.RegressionOuterClass.RegressionResponse> regress(
                tensorflow.serving.RegressionOuterClass.RegressionRequest request) {
            return futureUnaryCall(getChannel().newCall(METHOD_REGRESS, getCallOptions()), request);
        }

        @java.lang.Override
        public com.google.common.util.concurrent.ListenableFuture<tensorflow.serving.Predict.PredictResponse> predict(
                tensorflow.serving.Predict.PredictRequest request) {
            return futureUnaryCall(getChannel().newCall(METHOD_PREDICT, getCallOptions()), request);
        }

        @java.lang.Override
        public com.google.common.util.concurrent.ListenableFuture<tensorflow.serving.GetModelMetadata.GetModelMetadataResponse> getModelMetadata(
                tensorflow.serving.GetModelMetadata.GetModelMetadataRequest request) {
            return futureUnaryCall(getChannel().newCall(METHOD_GET_MODEL_METADATA, getCallOptions()), request);
        }
    }

    public static io.grpc.ServerServiceDefinition bindService(final PredictionService serviceImpl) {
        return io.grpc.ServerServiceDefinition.builder(SERVICE_NAME).addMethod(METHOD_CLASSIFY, asyncUnaryCall(
                new io.grpc.stub.ServerCalls.UnaryMethod<tensorflow.serving.Classification.ClassificationRequest, tensorflow.serving.Classification.ClassificationResponse>() {
                    @java.lang.Override
                    public void invoke(tensorflow.serving.Classification.ClassificationRequest request,
                                       io.grpc.stub.StreamObserver<tensorflow.serving.Classification.ClassificationResponse> responseObserver) {
                        serviceImpl.classify(request, responseObserver);
                    }
                })).addMethod(METHOD_REGRESS, asyncUnaryCall(
                new io.grpc.stub.ServerCalls.UnaryMethod<tensorflow.serving.RegressionOuterClass.RegressionRequest, tensorflow.serving.RegressionOuterClass.RegressionResponse>() {
                    @java.lang.Override
                    public void invoke(tensorflow.serving.RegressionOuterClass.RegressionRequest request,
                                       io.grpc.stub.StreamObserver<tensorflow.serving.RegressionOuterClass.RegressionResponse> responseObserver) {
                        serviceImpl.regress(request, responseObserver);
                    }
                })).addMethod(METHOD_PREDICT, asyncUnaryCall(
                new io.grpc.stub.ServerCalls.UnaryMethod<tensorflow.serving.Predict.PredictRequest, tensorflow.serving.Predict.PredictResponse>() {
                    @java.lang.Override
                    public void invoke(tensorflow.serving.Predict.PredictRequest request,
                                       io.grpc.stub.StreamObserver<tensorflow.serving.Predict.PredictResponse> responseObserver) {
                        serviceImpl.predict(request, responseObserver);
                    }
                })).addMethod(METHOD_GET_MODEL_METADATA, asyncUnaryCall(
                new io.grpc.stub.ServerCalls.UnaryMethod<tensorflow.serving.GetModelMetadata.GetModelMetadataRequest, tensorflow.serving.GetModelMetadata.GetModelMetadataResponse>() {
                    @java.lang.Override
                    public void invoke(tensorflow.serving.GetModelMetadata.GetModelMetadataRequest request,
                                       io.grpc.stub.StreamObserver<tensorflow.serving.GetModelMetadata.GetModelMetadataResponse> responseObserver) {
                        serviceImpl.getModelMetadata(request, responseObserver);
                    }
                })).build();
    }
}
