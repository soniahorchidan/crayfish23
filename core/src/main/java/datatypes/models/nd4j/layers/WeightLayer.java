package datatypes.models.nd4j.layers;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Arrays;

public class WeightLayer extends Layer<INDArray> {
    private INDArray weights;

    public WeightLayer(int layerNum) {
        super(layerNum);
    }

    public void set(INDArray weights) {
        this.weights = weights;
    }

    @Override
    public String getType() {
        return "WEIGHT";
    }

    @Override
    public INDArray apply(INDArray input) {
        return weights.mmul(input);
    }

    @Override
    public String toString() {
        return "WeightLayer{" +
               "shape=" + Arrays.toString(this.weights.shape()) +
               ", datatype=" + this.weights.dataType().name() +
               '}';
    }

    public INDArray getWeights() {
        return weights;
    }
}
