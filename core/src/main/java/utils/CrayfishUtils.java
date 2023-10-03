package utils;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.ArrayUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CrayfishUtils {
    public static Configuration readConfiguration(String expConfigPath) throws ConfigurationException {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<PropertiesConfiguration>(
                PropertiesConfiguration.class).configure(
                params.fileBased().setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                      .setFile(new File(expConfigPath)));
        return builder.getConfiguration();
    }

    public static int[] splitIntoParts(int whole, int parts) {
        int[] arr = new int[parts];
        for (int i = 0; i < arr.length; i++)
            whole -= arr[i] = (whole + parts - i - 1) / (parts - i);
        return arr;
    }

    public static INDArray convertArrayListToINDArray(ArrayList<Float> list) {
        float[] data = ArrayUtils.toPrimitive((list.toArray(new Float[list.size()])));
        return Nd4j.create(data);
    }

    public static ArrayList<Float> convertINDArrayToArrayList(INDArray list) {
        if (list == null)
            return null;
        return convertToArrayList(list.toFloatVector());
    }

    public static ArrayList<ArrayList<Float>> convertINDArrayBatchToArrayList(INDArray batch) {
        if (batch == null)
            return null;
        return convertToArrayListBatch(batch.toFloatMatrix());
    }

    public static ArrayList<Float> convertToArrayList(float[] inp) {
        ArrayList<Float> srcEmb = new ArrayList<>();
        for (float e : inp) {
            srcEmb.add(e);
        }
        return srcEmb;
    }

    public static ArrayList<ArrayList<Float>> convertToArrayListBatch(float[][] batch) {
        ArrayList<ArrayList<Float>> srcEmb = new ArrayList<>();
        for (float[] e : batch) {
            ArrayList<Float> batchResult = new ArrayList<>();
            for (float ee : e) {
                batchResult.add(ee);
            }
            srcEmb.add(batchResult);
        }
        return srcEmb;
    }

    public static INDArray convertBatchToINDArray(ArrayList<ArrayList<Float>> input) {
        int inputSize = input.get(0).size();
        int batchSize = input.size();
        ArrayList<Float> in = new ArrayList<>();
        for (ArrayList<Float> batch : input) {
            in.addAll(batch);
        }
        INDArray inputINDArray = CrayfishUtils.convertArrayListToINDArray(in);
        inputINDArray = inputINDArray.reshape(new int[]{inputSize, batchSize});
        return inputINDArray;
    }

    public static abstract class Either<L, R> {
        private L left;
        private R right;

        public static final class Left<L, R> extends Either<L, R> {
            public Left(L left) {
                super(left, null);
            }
        }

        public static final class Right<L, R> extends Either<L, R> {
            public Right(R right) {
                super(null, right);
            }
        }

        Either(L left, R right) {
            this.left = left;
            this.right = right;
        }

        public static <L, R> Either<L, R> ofLeft(L left) {
            return new Left<>(left);
        }

        public static <L, R> Either<L, R> ofRight(R right) {
            return new Right<>(right);
        }

        public static <L, R> Either<L, R> predicate(Supplier<Boolean> predicate, Supplier<L> left, Supplier<R> right) {
            return !predicate.get() ? ofLeft(left.get()) : ofRight(right.get());
        }

        public static <L, R> Either<L, R> predicate(boolean predicate, L left, R right) {
            return predicate(() -> predicate, () -> left, () -> right);
        }

        public Optional<L> left() {
            return Optional.ofNullable(left);
        }

        public Optional<R> right() {
            return Optional.ofNullable(right);
        }

        public boolean isLeft() {
            return left().isPresent();
        }

        public boolean isRight() {
            return right().isPresent();
        }

        public <R2> Either<L, R2> flatMap(Function<R, Either<L, R2>> f) {
            if (isLeft()) {
                return Either.ofLeft(left);
            }

            return f.apply(right);
        }

        public <R2> Either<L, R2> map(Function<R, R2> f) {
            return flatMap(r -> Either.ofRight(f.apply(r)));
        }

        public void ifLeft(Consumer<L> f) {
            if (isLeft()) {
                f.accept(left);
            }
        }

        public void ifRight(Consumer<R> f) {
            if (isRight()) {
                f.accept(right);
            }
        }

        public L leftOrElse(L alternative) {
            return isLeft() ? left : alternative;
        }

        public R rightOrElse(R alternative) {
            return isRight() ? right : alternative;
        }
    }
}
