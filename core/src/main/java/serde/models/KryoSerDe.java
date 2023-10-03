package serde.models;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nd4j.linalg.factory.Nd4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import serde.data.INDArraySerializer;

import java.io.ByteArrayOutputStream;

public class KryoSerDe {
    private static final Logger logger = LogManager.getLogger(KryoSerDe.class);

    public static byte[] serialize(String s, Object object) {return KryoUtils.serialize(object);}

    public static Object deserialize(String s, byte[] bytes) {
        return KryoUtils.deserialize(bytes);
    }

    private static class KryoUtils {

        private static final KryoFactory factory = new KryoFactory() {

            public Kryo create() {
                Kryo kryo = new Kryo();
                try {
                    kryo.register(Nd4j.getBackend().getNDArrayClass(), new INDArraySerializer());
                    kryo.setRegistrationRequired(false);

                    ((Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy()).setFallbackInstantiatorStrategy(
                            new StdInstantiatorStrategy());
                } catch (Exception e) {
                    logger.error("Exception occurred", e);
                }
                return kryo;
            }
        };

        private static final KryoPool pool = new KryoPool.Builder(factory).softReferences().build();


        public static byte[] serialize(final Object obj) {

            return pool.run(new KryoCallback<byte[]>() {

                @Override
                public byte[] execute(Kryo kryo) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    Output output = new Output(stream);
                    kryo.writeClassAndObject(output, obj);
                    output.close();
                    return stream.toByteArray();
                }

            });
        }

        @SuppressWarnings("unchecked")
        public static <V> V deserialize(final byte[] objectData) {

            return (V) pool.run(new KryoCallback<V>() {

                @Override
                public V execute(Kryo kryo) {
                    Input input = new Input(objectData);
                    return (V) kryo.readClassAndObject(input);
                }

            });
        }
    }


}