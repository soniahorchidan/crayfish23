// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: tensorflow/core/framework/function.proto

package org.tensorflow.framework;

/**
 * <pre>
 * GradientDef defines the gradient function of a function defined in
 * a function library.
 * A gradient function g (specified by gradient_func) for a function f
 * (specified by function_name) must follow the following:
 * The function 'f' must be a numerical function which takes N inputs
 * and produces M outputs. Its gradient function 'g', which is a
 * function taking N + M inputs and produces N outputs.
 * I.e. if we have
 *    (y1, y2, ..., y_M) = f(x1, x2, ..., x_N),
 * then, g is
 *    (dL/dx1, dL/dx2, ..., dL/dx_N) = g(x1, x2, ..., x_N,
 *                                      dL/dy1, dL/dy2, ..., dL/dy_M),
 * where L is a scalar-value function of (x1, x2, ..., xN) (e.g., the
 * loss function). dL/dx_i is the partial derivative of L with respect
 * to x_i.
 * </pre>
 * <p>
 * Protobuf type {@code tensorflow.GradientDef}
 */
public final class GradientDef extends
                               com.google.protobuf.GeneratedMessageV3 implements
                                                                      // @@protoc_insertion_point(message_implements:tensorflow.GradientDef)
                                                                              GradientDefOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use GradientDef.newBuilder() to construct.
    private GradientDef(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private GradientDef() {
        functionName_ = "";
        gradientFunc_ = "";
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
            UnusedPrivateParameter unused) {
        return new GradientDef();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
        return this.unknownFields;
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
        return org.tensorflow.framework.FunctionProtos.internal_static_tensorflow_GradientDef_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internalGetFieldAccessorTable() {
        return org.tensorflow.framework.FunctionProtos.internal_static_tensorflow_GradientDef_fieldAccessorTable
                .ensureFieldAccessorsInitialized(
                        org.tensorflow.framework.GradientDef.class, org.tensorflow.framework.GradientDef.Builder.class);
    }

    public static final int FUNCTION_NAME_FIELD_NUMBER = 1;
    @SuppressWarnings("serial")
    private volatile java.lang.Object functionName_ = "";

    /**
     * <pre>
     * The function name.
     * </pre>
     *
     * <code>string function_name = 1;</code>
     *
     * @return The functionName.
     */
    @java.lang.Override
    public java.lang.String getFunctionName() {
        java.lang.Object ref = functionName_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs =
                    (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            functionName_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * The function name.
     * </pre>
     *
     * <code>string function_name = 1;</code>
     *
     * @return The bytes for functionName.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
    getFunctionNameBytes() {
        java.lang.Object ref = functionName_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b =
                    com.google.protobuf.ByteString.copyFromUtf8(
                            (java.lang.String) ref);
            functionName_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int GRADIENT_FUNC_FIELD_NUMBER = 2;
    @SuppressWarnings("serial")
    private volatile java.lang.Object gradientFunc_ = "";

    /**
     * <pre>
     * The gradient function's name.
     * </pre>
     *
     * <code>string gradient_func = 2;</code>
     *
     * @return The gradientFunc.
     */
    @java.lang.Override
    public java.lang.String getGradientFunc() {
        java.lang.Object ref = gradientFunc_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs =
                    (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            gradientFunc_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * The gradient function's name.
     * </pre>
     *
     * <code>string gradient_func = 2;</code>
     *
     * @return The bytes for gradientFunc.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
    getGradientFuncBytes() {
        java.lang.Object ref = gradientFunc_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b =
                    com.google.protobuf.ByteString.copyFromUtf8(
                            (java.lang.String) ref);
            gradientFunc_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    private byte memoizedIsInitialized = -1;

    @java.lang.Override
    public final boolean isInitialized() {
        byte isInitialized = memoizedIsInitialized;
        if (isInitialized == 1) return true;
        if (isInitialized == 0) return false;

        memoizedIsInitialized = 1;
        return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
            throws java.io.IOException {
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(functionName_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, functionName_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(gradientFunc_)) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 2, gradientFunc_);
        }
        getUnknownFields().writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) return size;

        size = 0;
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(functionName_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, functionName_);
        }
        if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(gradientFunc_)) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, gradientFunc_);
        }
        size += getUnknownFields().getSerializedSize();
        memoizedSize = size;
        return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof org.tensorflow.framework.GradientDef)) {
            return super.equals(obj);
        }
        org.tensorflow.framework.GradientDef other = (org.tensorflow.framework.GradientDef) obj;

        if (!getFunctionName()
                .equals(other.getFunctionName())) return false;
        if (!getGradientFunc()
                .equals(other.getGradientFunc())) return false;
        if (!getUnknownFields().equals(other.getUnknownFields())) return false;
        return true;
    }

    @java.lang.Override
    public int hashCode() {
        if (memoizedHashCode != 0) {
            return memoizedHashCode;
        }
        int hash = 41;
        hash = (19 * hash) + getDescriptor().hashCode();
        hash = (37 * hash) + FUNCTION_NAME_FIELD_NUMBER;
        hash = (53 * hash) + getFunctionName().hashCode();
        hash = (37 * hash) + GRADIENT_FUNC_FIELD_NUMBER;
        hash = (53 * hash) + getGradientFunc().hashCode();
        hash = (29 * hash) + getUnknownFields().hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static org.tensorflow.framework.GradientDef parseFrom(
            java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static org.tensorflow.framework.GradientDef parseFrom(
            java.nio.ByteBuffer data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static org.tensorflow.framework.GradientDef parseFrom(
            com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static org.tensorflow.framework.GradientDef parseFrom(
            com.google.protobuf.ByteString data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static org.tensorflow.framework.GradientDef parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static org.tensorflow.framework.GradientDef parseFrom(
            byte[] data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static org.tensorflow.framework.GradientDef parseFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static org.tensorflow.framework.GradientDef parseFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static org.tensorflow.framework.GradientDef parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input);
    }

    public static org.tensorflow.framework.GradientDef parseDelimitedFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static org.tensorflow.framework.GradientDef parseFrom(
            com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static org.tensorflow.framework.GradientDef parseFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {return newBuilder();}

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(org.tensorflow.framework.GradientDef prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @java.lang.Override
    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE
                ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
            com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    /**
     * <pre>
     * GradientDef defines the gradient function of a function defined in
     * a function library.
     * A gradient function g (specified by gradient_func) for a function f
     * (specified by function_name) must follow the following:
     * The function 'f' must be a numerical function which takes N inputs
     * and produces M outputs. Its gradient function 'g', which is a
     * function taking N + M inputs and produces N outputs.
     * I.e. if we have
     *    (y1, y2, ..., y_M) = f(x1, x2, ..., x_N),
     * then, g is
     *    (dL/dx1, dL/dx2, ..., dL/dx_N) = g(x1, x2, ..., x_N,
     *                                      dL/dy1, dL/dy2, ..., dL/dy_M),
     * where L is a scalar-value function of (x1, x2, ..., xN) (e.g., the
     * loss function). dL/dx_i is the partial derivative of L with respect
     * to x_i.
     * </pre>
     * <p>
     * Protobuf type {@code tensorflow.GradientDef}
     */
    public static final class Builder extends
                                      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
                                                                                              // @@protoc_insertion_point(builder_implements:tensorflow.GradientDef)
                                                                                                      org.tensorflow.framework.GradientDefOrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
            return org.tensorflow.framework.FunctionProtos.internal_static_tensorflow_GradientDef_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
            return org.tensorflow.framework.FunctionProtos.internal_static_tensorflow_GradientDef_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(
                            org.tensorflow.framework.GradientDef.class,
                            org.tensorflow.framework.GradientDef.Builder.class);
        }

        // Construct using org.tensorflow.framework.GradientDef.newBuilder()
        private Builder() {

        }

        private Builder(
                com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
            super(parent);

        }

        @java.lang.Override
        public Builder clear() {
            super.clear();
            bitField0_ = 0;
            functionName_ = "";
            gradientFunc_ = "";
            return this;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
            return org.tensorflow.framework.FunctionProtos.internal_static_tensorflow_GradientDef_descriptor;
        }

        @java.lang.Override
        public org.tensorflow.framework.GradientDef getDefaultInstanceForType() {
            return org.tensorflow.framework.GradientDef.getDefaultInstance();
        }

        @java.lang.Override
        public org.tensorflow.framework.GradientDef build() {
            org.tensorflow.framework.GradientDef result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @java.lang.Override
        public org.tensorflow.framework.GradientDef buildPartial() {
            org.tensorflow.framework.GradientDef result = new org.tensorflow.framework.GradientDef(this);
            if (bitField0_ != 0) {
                buildPartial0(result);
            }
            onBuilt();
            return result;
        }

        private void buildPartial0(org.tensorflow.framework.GradientDef result) {
            int from_bitField0_ = bitField0_;
            if (((from_bitField0_ & 0x00000001) != 0)) {
                result.functionName_ = functionName_;
            }
            if (((from_bitField0_ & 0x00000002) != 0)) {
                result.gradientFunc_ = gradientFunc_;
            }
        }

        @java.lang.Override
        public Builder clone() {
            return super.clone();
        }

        @java.lang.Override
        public Builder setField(
                com.google.protobuf.Descriptors.FieldDescriptor field,
                java.lang.Object value) {
            return super.setField(field, value);
        }

        @java.lang.Override
        public Builder clearField(
                com.google.protobuf.Descriptors.FieldDescriptor field) {
            return super.clearField(field);
        }

        @java.lang.Override
        public Builder clearOneof(
                com.google.protobuf.Descriptors.OneofDescriptor oneof) {
            return super.clearOneof(oneof);
        }

        @java.lang.Override
        public Builder setRepeatedField(
                com.google.protobuf.Descriptors.FieldDescriptor field,
                int index, java.lang.Object value) {
            return super.setRepeatedField(field, index, value);
        }

        @java.lang.Override
        public Builder addRepeatedField(
                com.google.protobuf.Descriptors.FieldDescriptor field,
                java.lang.Object value) {
            return super.addRepeatedField(field, value);
        }

        @java.lang.Override
        public Builder mergeFrom(com.google.protobuf.Message other) {
            if (other instanceof org.tensorflow.framework.GradientDef) {
                return mergeFrom((org.tensorflow.framework.GradientDef) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(org.tensorflow.framework.GradientDef other) {
            if (other == org.tensorflow.framework.GradientDef.getDefaultInstance()) return this;
            if (!other.getFunctionName().isEmpty()) {
                functionName_ = other.functionName_;
                bitField0_ |= 0x00000001;
                onChanged();
            }
            if (!other.getGradientFunc().isEmpty()) {
                gradientFunc_ = other.gradientFunc_;
                bitField0_ |= 0x00000002;
                onChanged();
            }
            this.mergeUnknownFields(other.getUnknownFields());
            onChanged();
            return this;
        }

        @java.lang.Override
        public final boolean isInitialized() {
            return true;
        }

        @java.lang.Override
        public Builder mergeFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            if (extensionRegistry == null) {
                throw new java.lang.NullPointerException();
            }
            try {
                boolean done = false;
                while (!done) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        case 10: {
                            functionName_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000001;
                            break;
                        } // case 10
                        case 18: {
                            gradientFunc_ = input.readStringRequireUtf8();
                            bitField0_ |= 0x00000002;
                            break;
                        } // case 18
                        default: {
                            if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                                done = true; // was an endgroup tag
                            }
                            break;
                        } // default:
                    } // switch (tag)
                } // while (!done)
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw e.unwrapIOException();
            } finally {
                onChanged();
            } // finally
            return this;
        }

        private int bitField0_;

        private java.lang.Object functionName_ = "";

        /**
         * <pre>
         * The function name.
         * </pre>
         *
         * <code>string function_name = 1;</code>
         *
         * @return The functionName.
         */
        public java.lang.String getFunctionName() {
            java.lang.Object ref = functionName_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                functionName_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <pre>
         * The function name.
         * </pre>
         *
         * <code>string function_name = 1;</code>
         *
         * @return The bytes for functionName.
         */
        public com.google.protobuf.ByteString
        getFunctionNameBytes() {
            java.lang.Object ref = functionName_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                functionName_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * The function name.
         * </pre>
         *
         * <code>string function_name = 1;</code>
         *
         * @param value The functionName to set.
         * @return This builder for chaining.
         */
        public Builder setFunctionName(
                java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            functionName_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The function name.
         * </pre>
         *
         * <code>string function_name = 1;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearFunctionName() {
            functionName_ = getDefaultInstance().getFunctionName();
            bitField0_ = (bitField0_ & ~0x00000001);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The function name.
         * </pre>
         *
         * <code>string function_name = 1;</code>
         *
         * @param value The bytes for functionName to set.
         * @return This builder for chaining.
         */
        public Builder setFunctionNameBytes(
                com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            functionName_ = value;
            bitField0_ |= 0x00000001;
            onChanged();
            return this;
        }

        private java.lang.Object gradientFunc_ = "";

        /**
         * <pre>
         * The gradient function's name.
         * </pre>
         *
         * <code>string gradient_func = 2;</code>
         *
         * @return The gradientFunc.
         */
        public java.lang.String getGradientFunc() {
            java.lang.Object ref = gradientFunc_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                gradientFunc_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <pre>
         * The gradient function's name.
         * </pre>
         *
         * <code>string gradient_func = 2;</code>
         *
         * @return The bytes for gradientFunc.
         */
        public com.google.protobuf.ByteString
        getGradientFuncBytes() {
            java.lang.Object ref = gradientFunc_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                gradientFunc_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * The gradient function's name.
         * </pre>
         *
         * <code>string gradient_func = 2;</code>
         *
         * @param value The gradientFunc to set.
         * @return This builder for chaining.
         */
        public Builder setGradientFunc(
                java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }
            gradientFunc_ = value;
            bitField0_ |= 0x00000002;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The gradient function's name.
         * </pre>
         *
         * <code>string gradient_func = 2;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearGradientFunc() {
            gradientFunc_ = getDefaultInstance().getGradientFunc();
            bitField0_ = (bitField0_ & ~0x00000002);
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The gradient function's name.
         * </pre>
         *
         * <code>string gradient_func = 2;</code>
         *
         * @param value The bytes for gradientFunc to set.
         * @return This builder for chaining.
         */
        public Builder setGradientFuncBytes(
                com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);
            gradientFunc_ = value;
            bitField0_ |= 0x00000002;
            onChanged();
            return this;
        }

        @java.lang.Override
        public final Builder setUnknownFields(
                final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.setUnknownFields(unknownFields);
        }

        @java.lang.Override
        public final Builder mergeUnknownFields(
                final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.mergeUnknownFields(unknownFields);
        }


        // @@protoc_insertion_point(builder_scope:tensorflow.GradientDef)
    }

    // @@protoc_insertion_point(class_scope:tensorflow.GradientDef)
    private static final org.tensorflow.framework.GradientDef DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new org.tensorflow.framework.GradientDef();
    }

    public static org.tensorflow.framework.GradientDef getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<GradientDef>
            PARSER = new com.google.protobuf.AbstractParser<GradientDef>() {
        @java.lang.Override
        public GradientDef parsePartialFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            Builder builder = newBuilder();
            try {
                builder.mergeFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                throw e.setUnfinishedMessage(builder.buildPartial());
            } catch (com.google.protobuf.UninitializedMessageException e) {
                throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
            } catch (java.io.IOException e) {
                throw new com.google.protobuf.InvalidProtocolBufferException(e)
                        .setUnfinishedMessage(builder.buildPartial());
            }
            return builder.buildPartial();
        }
    };

    public static com.google.protobuf.Parser<GradientDef> parser() {
        return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<GradientDef> getParserForType() {
        return PARSER;
    }

    @java.lang.Override
    public org.tensorflow.framework.GradientDef getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

}
