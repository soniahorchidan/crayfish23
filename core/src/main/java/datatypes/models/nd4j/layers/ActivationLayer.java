package datatypes.models.nd4j.layers;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.io.Serializable;

public class ActivationLayer extends Layer<String> implements Serializable {
    private String activationFunc;

    public ActivationLayer(int layerNum) {
        super(layerNum);
    }

    public void set(String activation) {
        this.activationFunc = activation;
    }

    @Override
    public String getType() {
        return "ACTIVATION";
    }

    @Override
    public INDArray apply(INDArray input) throws Exception {
        switch (activationFunc) {
            case "RELU": {
                return Transforms.relu(input);
            }
            case "SOFTMAX": {
                return Transforms.softmax(input);
            }
            default: {
                throw new Exception("Unknown activation function " + activationFunc + "!");
            }
        }
    }

    @Override
    public String toString() {
        return "ActivationLayer{" +
               "activationFunc='" + activationFunc + '\'' +
               '}';
    }
}
