package datatypes.models.dl4j;

import datatypes.datapoints.CrayfishInputData;
import datatypes.datapoints.CrayfishPrediction;
import datatypes.models.CrayfishModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import utils.CrayfishUtils;

import java.io.File;
import java.io.Serializable;

/**
 * Class used to load and apply a model saved in ND4J format.
 */
public class DL4JModel extends CrayfishModel implements Serializable {
    private static final Logger logger = LogManager.getLogger(datatypes.models.dl4j.DL4JModel.class);
    private MultiLayerNetwork model;

    public DL4JModel() {
        super();
        System.setProperty("org.nd4j.parallelism.threads", "1");
    }

    @Override
    public void loadModel(String modelName, String path) throws Exception {
        File modelLocation = new File(path);
        File modelFilePath = null;

        // Pick the first .h5 file found
        for (File f : modelLocation.listFiles()) {
            if (f.isFile() && f.getName().contains("h5")) {
                modelFilePath = f;
                break;
            }
        }

        if (modelFilePath == null) throw new RuntimeException("Model not found!");
        this.model = KerasModelImport.importKerasSequentialModelAndWeights(modelFilePath.getAbsolutePath());
    }

    public void build() throws Exception {}

    @Override
    public CrayfishPrediction apply(CrayfishInputData input) throws Exception {
        if (input == null || input.get() == null) return null;
        INDArray inputINDArray = CrayfishUtils.convertBatchToINDArray(input.get());
        INDArray outputINDArray = this.model.output(inputINDArray.transpose());
        return new CrayfishPrediction(CrayfishUtils.convertINDArrayBatchToArrayList(outputINDArray));
    }


    @Override
    public String toString() {
        return "DL4JModel{" + "model=" + model + '}';
    }
}