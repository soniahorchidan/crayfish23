package serde.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// Source: https://github.com/eclipse/deeplearning4j/blob/752ffe4e74d064b846729af09a48ee328c2514a4/nd4j/nd4j-serde/nd4j-kryo/src/main/java/org/nd4j/kryo/Nd4jSerializer.java#L34
public class INDArraySerializer extends Serializer<INDArray> {
    /**
     * Writes the bytes for the object to the output.
     * <p>
     * This method should not be called directly, instead this serializer can be passed to {@link Kryo} write methods that accept a
     * serialier.
     *
     * @param kryo
     * @param output
     * @param object May be null if {@link #getAcceptsNull()} is true.
     */
    @Override
    public void write(Kryo kryo, Output output, INDArray object) {
        DataOutputStream dos = new DataOutputStream(output);
        try {
            Nd4j.write(object, dos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //Note: output should NOT be closed manually here - may be needed elsewhere (and closing here will cause serialization to fail)
    }

    /**
     * Reads bytes and returns a new object of the specified concrete opType.
     * <p>
     * Before Kryo can be used to read child objects, {@link Kryo#reference(Object)} must be called with the parent object to
     * ensure it can be referenced by the child objects. Any serializer that uses {@link Kryo} to read a child object may need to
     * be reentrant.
     * <p>
     * This method should not be called directly, instead this serializer can be passed to {@link Kryo} read methods that accept a
     * serialier.
     *
     * @param kryo
     * @param input
     * @param aClass
     * @return May be null if {@link #getAcceptsNull()} is true.
     */
    @Override
    public INDArray read(Kryo kryo, Input input, Class<INDArray> aClass) {
        DataInputStream dis = new DataInputStream(input);
        return Nd4j.read(dis);
        //Note: input should NOT be closed manually here - may be needed elsewhere (and closing here will cause serialization to fail)
    }
}