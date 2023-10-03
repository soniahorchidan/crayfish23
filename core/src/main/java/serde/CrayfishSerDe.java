package serde;

public abstract class CrayfishSerDe<IN, OUT> {
    public abstract IN deserialize(OUT des);

    public abstract OUT serialize(IN inputBatch);
}
