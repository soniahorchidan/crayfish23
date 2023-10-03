// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: tensorflow/core/framework/attr_value.proto

package org.tensorflow.framework;

public interface AttrValueOrBuilder extends
                                    // @@protoc_insertion_point(interface_extends:tensorflow.AttrValue)
                                            com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * "string"
     * </pre>
     *
     * <code>bytes s = 2;</code>
     *
     * @return Whether the s field is set.
     */
    boolean hasS();

    /**
     * <pre>
     * "string"
     * </pre>
     *
     * <code>bytes s = 2;</code>
     *
     * @return The s.
     */
    com.google.protobuf.ByteString getS();

    /**
     * <pre>
     * "int"
     * </pre>
     *
     * <code>int64 i = 3;</code>
     *
     * @return Whether the i field is set.
     */
    boolean hasI();

    /**
     * <pre>
     * "int"
     * </pre>
     *
     * <code>int64 i = 3;</code>
     *
     * @return The i.
     */
    long getI();

    /**
     * <pre>
     * "float"
     * </pre>
     *
     * <code>float f = 4;</code>
     *
     * @return Whether the f field is set.
     */
    boolean hasF();

    /**
     * <pre>
     * "float"
     * </pre>
     *
     * <code>float f = 4;</code>
     *
     * @return The f.
     */
    float getF();

    /**
     * <pre>
     * "bool"
     * </pre>
     *
     * <code>bool b = 5;</code>
     *
     * @return Whether the b field is set.
     */
    boolean hasB();

    /**
     * <pre>
     * "bool"
     * </pre>
     *
     * <code>bool b = 5;</code>
     *
     * @return The b.
     */
    boolean getB();

    /**
     * <pre>
     * "type"
     * </pre>
     *
     * <code>.tensorflow.DataType type = 6;</code>
     *
     * @return Whether the type field is set.
     */
    boolean hasType();

    /**
     * <pre>
     * "type"
     * </pre>
     *
     * <code>.tensorflow.DataType type = 6;</code>
     *
     * @return The enum numeric value on the wire for type.
     */
    int getTypeValue();

    /**
     * <pre>
     * "type"
     * </pre>
     *
     * <code>.tensorflow.DataType type = 6;</code>
     *
     * @return The type.
     */
    org.tensorflow.framework.DataType getType();

    /**
     * <pre>
     * "shape"
     * </pre>
     *
     * <code>.tensorflow.TensorShapeProto shape = 7;</code>
     *
     * @return Whether the shape field is set.
     */
    boolean hasShape();

    /**
     * <pre>
     * "shape"
     * </pre>
     *
     * <code>.tensorflow.TensorShapeProto shape = 7;</code>
     *
     * @return The shape.
     */
    org.tensorflow.framework.TensorShapeProto getShape();

    /**
     * <pre>
     * "shape"
     * </pre>
     *
     * <code>.tensorflow.TensorShapeProto shape = 7;</code>
     */
    org.tensorflow.framework.TensorShapeProtoOrBuilder getShapeOrBuilder();

    /**
     * <pre>
     * "tensor"
     * </pre>
     *
     * <code>.tensorflow.TensorProto tensor = 8;</code>
     *
     * @return Whether the tensor field is set.
     */
    boolean hasTensor();

    /**
     * <pre>
     * "tensor"
     * </pre>
     *
     * <code>.tensorflow.TensorProto tensor = 8;</code>
     *
     * @return The tensor.
     */
    org.tensorflow.framework.TensorProto getTensor();

    /**
     * <pre>
     * "tensor"
     * </pre>
     *
     * <code>.tensorflow.TensorProto tensor = 8;</code>
     */
    org.tensorflow.framework.TensorProtoOrBuilder getTensorOrBuilder();

    /**
     * <pre>
     * any "list(...)"
     * </pre>
     *
     * <code>.tensorflow.AttrValue.ListValue list = 1;</code>
     *
     * @return Whether the list field is set.
     */
    boolean hasList();

    /**
     * <pre>
     * any "list(...)"
     * </pre>
     *
     * <code>.tensorflow.AttrValue.ListValue list = 1;</code>
     *
     * @return The list.
     */
    org.tensorflow.framework.AttrValue.ListValue getList();

    /**
     * <pre>
     * any "list(...)"
     * </pre>
     *
     * <code>.tensorflow.AttrValue.ListValue list = 1;</code>
     */
    org.tensorflow.framework.AttrValue.ListValueOrBuilder getListOrBuilder();

    /**
     * <pre>
     * "func" represents a function. func.name is a function's name or
     * a primitive op's name. func.attr.first is the name of an attr
     * defined for that function. func.attr.second is the value for
     * that attr in the instantiation.
     * </pre>
     *
     * <code>.tensorflow.NameAttrList func = 10;</code>
     *
     * @return Whether the func field is set.
     */
    boolean hasFunc();

    /**
     * <pre>
     * "func" represents a function. func.name is a function's name or
     * a primitive op's name. func.attr.first is the name of an attr
     * defined for that function. func.attr.second is the value for
     * that attr in the instantiation.
     * </pre>
     *
     * <code>.tensorflow.NameAttrList func = 10;</code>
     *
     * @return The func.
     */
    org.tensorflow.framework.NameAttrList getFunc();

    /**
     * <pre>
     * "func" represents a function. func.name is a function's name or
     * a primitive op's name. func.attr.first is the name of an attr
     * defined for that function. func.attr.second is the value for
     * that attr in the instantiation.
     * </pre>
     *
     * <code>.tensorflow.NameAttrList func = 10;</code>
     */
    org.tensorflow.framework.NameAttrListOrBuilder getFuncOrBuilder();

    /**
     * <pre>
     * This is a placeholder only used in nodes defined inside a
     * function.  It indicates the attr value will be supplied when
     * the function is instantiated.  For example, let us suppose a
     * node "N" in function "FN". "N" has an attr "A" with value
     * placeholder = "foo". When FN is instantiated with attr "foo"
     * set to "bar", the instantiated node N's attr A will have been
     * given the value "bar".
     * </pre>
     *
     * <code>string placeholder = 9;</code>
     *
     * @return Whether the placeholder field is set.
     */
    boolean hasPlaceholder();

    /**
     * <pre>
     * This is a placeholder only used in nodes defined inside a
     * function.  It indicates the attr value will be supplied when
     * the function is instantiated.  For example, let us suppose a
     * node "N" in function "FN". "N" has an attr "A" with value
     * placeholder = "foo". When FN is instantiated with attr "foo"
     * set to "bar", the instantiated node N's attr A will have been
     * given the value "bar".
     * </pre>
     *
     * <code>string placeholder = 9;</code>
     *
     * @return The placeholder.
     */
    java.lang.String getPlaceholder();

    /**
     * <pre>
     * This is a placeholder only used in nodes defined inside a
     * function.  It indicates the attr value will be supplied when
     * the function is instantiated.  For example, let us suppose a
     * node "N" in function "FN". "N" has an attr "A" with value
     * placeholder = "foo". When FN is instantiated with attr "foo"
     * set to "bar", the instantiated node N's attr A will have been
     * given the value "bar".
     * </pre>
     *
     * <code>string placeholder = 9;</code>
     *
     * @return The bytes for placeholder.
     */
    com.google.protobuf.ByteString
    getPlaceholderBytes();

    public org.tensorflow.framework.AttrValue.ValueCase getValueCase();
}
