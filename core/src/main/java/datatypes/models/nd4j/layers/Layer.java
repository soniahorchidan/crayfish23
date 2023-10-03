package datatypes.models.nd4j.layers;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.Serializable;

public abstract class Layer<T> implements Serializable {
    private final int layerNum;

    public Layer() {
        this.layerNum = 0;
    }

    protected Layer(int layerNum) {
        this.layerNum = layerNum;
    }

    public int getLayerNum() {
        return this.layerNum;
    }

    public abstract INDArray apply(INDArray input) throws Exception;

    public abstract void set(T content);

    // TODO: make pretty
    public abstract String getType();
}
