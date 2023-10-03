// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: request/grpc/proto/torchserve/inference.proto

package org.pytorch.serve.grpc.inference;

public final class Inference {
    private Inference() {}

    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions(
                (com.google.protobuf.ExtensionRegistryLite) registry);
    }

    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_org_pytorch_serve_grpc_inference_PredictionsRequest_descriptor;
    static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_org_pytorch_serve_grpc_inference_PredictionsRequest_fieldAccessorTable;
    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_org_pytorch_serve_grpc_inference_PredictionsRequest_InputEntry_descriptor;
    static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_org_pytorch_serve_grpc_inference_PredictionsRequest_InputEntry_fieldAccessorTable;
    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_org_pytorch_serve_grpc_inference_PredictionResponse_descriptor;
    static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_org_pytorch_serve_grpc_inference_PredictionResponse_fieldAccessorTable;
    static final com.google.protobuf.Descriptors.Descriptor
            internal_static_org_pytorch_serve_grpc_inference_TorchServeHealthResponse_descriptor;
    static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            internal_static_org_pytorch_serve_grpc_inference_TorchServeHealthResponse_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor
    getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor
            descriptor;

    static {
        java.lang.String[] descriptorData = {
                "\n-request/grpc/proto/torchserve/inferenc" +
                "e.proto\022 org.pytorch.serve.grpc.inferenc" +
                "e\032\033google/protobuf/empty.proto\"\275\001\n\022Predi" +
                "ctionsRequest\022\022\n\nmodel_name\030\001 \001(\t\022\025\n\rmod" +
                "el_version\030\002 \001(\t\022N\n\005input\030\003 \003(\0132?.org.py" +
                "torch.serve.grpc.inference.PredictionsRe" +
                "quest.InputEntry\032,\n\nInputEntry\022\013\n\003key\030\001 " +
                "\001(\t\022\r\n\005value\030\002 \001(\014:\0028\001\"(\n\022PredictionResp" +
                "onse\022\022\n\nprediction\030\001 \001(\014\"*\n\030TorchServeHe" +
                "althResponse\022\016\n\006health\030\001 \001(\t2\361\001\n\024Inferen" +
                "ceAPIsService\022\\\n\004Ping\022\026.google.protobuf." +
                "Empty\032:.org.pytorch.serve.grpc.inference" +
                ".TorchServeHealthResponse\"\000\022{\n\013Predictio" +
                "ns\0224.org.pytorch.serve.grpc.inference.Pr" +
                "edictionsRequest\0324.org.pytorch.serve.grp" +
                "c.inference.PredictionResponse\"\000B\002P\001b\006pr" +
                "oto3"
        };
        descriptor = com.google.protobuf.Descriptors.FileDescriptor
                .internalBuildGeneratedFileFrom(descriptorData,
                                                new com.google.protobuf.Descriptors.FileDescriptor[]{
                                                        com.google.protobuf.EmptyProto.getDescriptor(),
                                                });
        internal_static_org_pytorch_serve_grpc_inference_PredictionsRequest_descriptor =
                getDescriptor().getMessageTypes().get(0);
        internal_static_org_pytorch_serve_grpc_inference_PredictionsRequest_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                internal_static_org_pytorch_serve_grpc_inference_PredictionsRequest_descriptor,
                new java.lang.String[]{"ModelName", "ModelVersion", "Input",});
        internal_static_org_pytorch_serve_grpc_inference_PredictionsRequest_InputEntry_descriptor =
                internal_static_org_pytorch_serve_grpc_inference_PredictionsRequest_descriptor.getNestedTypes().get(0);
        internal_static_org_pytorch_serve_grpc_inference_PredictionsRequest_InputEntry_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                internal_static_org_pytorch_serve_grpc_inference_PredictionsRequest_InputEntry_descriptor,
                new java.lang.String[]{"Key", "Value",});
        internal_static_org_pytorch_serve_grpc_inference_PredictionResponse_descriptor =
                getDescriptor().getMessageTypes().get(1);
        internal_static_org_pytorch_serve_grpc_inference_PredictionResponse_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                internal_static_org_pytorch_serve_grpc_inference_PredictionResponse_descriptor,
                new java.lang.String[]{"Prediction",});
        internal_static_org_pytorch_serve_grpc_inference_TorchServeHealthResponse_descriptor =
                getDescriptor().getMessageTypes().get(2);
        internal_static_org_pytorch_serve_grpc_inference_TorchServeHealthResponse_fieldAccessorTable = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                internal_static_org_pytorch_serve_grpc_inference_TorchServeHealthResponse_descriptor,
                new java.lang.String[]{"Health",});
        com.google.protobuf.EmptyProto.getDescriptor();
    }

    // @@protoc_insertion_point(outer_class_scope)
}