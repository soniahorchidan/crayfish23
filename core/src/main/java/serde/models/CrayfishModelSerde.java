package serde.models;

import datatypes.models.CrayfishModel;

import java.io.Serializable;

public class CrayfishModelSerde<M extends CrayfishModel> implements Serializable {
    /**
     * Default Kryo serialization.
     */
    public byte[] serialize(String s, M model) {
        return KryoSerDe.serialize(s, model);
    }

    /**
     * Default Kryo deserialization.
     */
    public M deserialize(String s, byte[] in) {
        return (M) KryoSerDe.deserialize(s, in);
    }
}
