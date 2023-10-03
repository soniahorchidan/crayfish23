package datatypes.models.nd4j;

import datatypes.datapoints.CrayfishInputData;
import datatypes.datapoints.CrayfishPrediction;
import datatypes.models.CrayfishModel;
import datatypes.models.nd4j.layers.ActivationLayer;
import datatypes.models.nd4j.layers.Layer;
import datatypes.models.nd4j.layers.WeightLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import utils.CrayfishUtils;

import java.io.File;
import java.io.Serializable;
import java.util.Vector;

/**
 * Class used to load and apply a model saved in ND4J format.
 */
// TODO: refactor generics
public class ND4JModel extends CrayfishModel implements Serializable {
    private static final long serialVersionUID = 1L;
    protected Vector<Layer> layers;
    private static final Logger logger = LogManager.getLogger(ND4JModel.class);

    public ND4JModel() {super();}

    @Override
    public void loadModel(String modelName, String path) throws Exception {
        // TODO: fix?
        File modelLocation = new File(path);
        File[] files = modelLocation.listFiles((dir1, name) -> name.endsWith(".npy") && name.contains("-"));

        int numLayers = files.length;
        this.layers = new Vector<>(numLayers);

        for (File file : files) {
            String[] split = file.getName().split("-");
            if (split.length < 2) throw new Exception("Invalid file format!");
            int layerNum = Integer.parseInt(split[0]);
            String layerType = split[1].toUpperCase();
            Layer layer = null;
            switch (layerType) {
                case "WEIGHT": {
                    layer = new WeightLayer(layerNum);
                    layer.set(Nd4j.createFromNpyFile(file));
                    break;
                }
                case "ACTIVATION": {
                    layer = new ActivationLayer(layerNum);
                    layer.set(split[2]);
                    break;
                }
                default: {
                    throw new Exception("Unknown layer type: " + layerType);
                }
            }
            this.layers.add(layer);
        }
        this.layers.sort((o1, o2) -> o1.getLayerNum() - o2.getLayerNum());

        for (int layerNum = 0; layerNum < layers.size(); layerNum++) {
            logger.info("Loaded layer at depth: " + layerNum + " ||| Layer details: " + layers.get(layerNum));
        }
    }

    public void build() throws Exception {}

    @Override
    public CrayfishPrediction apply(CrayfishInputData input) throws Exception {
        if (input == null || input.get() == null) return null;
        INDArray inputINDArray = CrayfishUtils.convertBatchToINDArray(input.get());
        INDArray outputINDArray = this.apply(inputINDArray);
        return new CrayfishPrediction(CrayfishUtils.convertINDArrayBatchToArrayList(outputINDArray));
    }

    private INDArray apply(INDArray input) throws Exception {
        INDArray result = input;
        for (Layer l : this.layers)
            result = l.apply(result);
        return result;
    }

    public Vector<Layer> getLayers() {
        return this.layers;
    }

    @Override
    public String toString() {
        return "ND4JModel{" + "layers=" + layers + '}';
    }
}