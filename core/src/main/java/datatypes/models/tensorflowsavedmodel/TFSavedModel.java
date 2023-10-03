package datatypes.models.tensorflowsavedmodel;

import datatypes.datapoints.CrayfishInputData;
import datatypes.datapoints.CrayfishPrediction;
import datatypes.models.CrayfishModel;
import org.apache.commons.lang3.ArrayUtils;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Shape;
import org.tensorflow.Tensor;
import org.tensorflow.proto.framework.*;
import utils.CrayfishUtils;

import java.io.Serializable;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class used to load and apply a model saved in TensorFlow SavedModel format.
 */
public class TFSavedModel extends CrayfishModel implements Serializable {
    private transient Session session = null; // TODO: close session?
    private transient String inputName;
    private transient long[] inputShapeSizes;
    private transient String outputName;
    //private TFSavedModelStruct content;
    private String modelLocation; //"./tmp/" + UUID.randomUUID();

    @Override
    public void loadModel(String modelName, String modelPath) throws Exception {
        //File modelLocation = new File(modelPath);
        //this.content = new TFSavedModelStruct(modelLocation);
        this.modelLocation = modelPath;
    }

    @Override
    public void build() throws Exception {
        MetaGraphDef metadata;
        ConfigProto config = ConfigProto.newBuilder().setInterOpParallelismThreads(1).setIntraOpParallelismThreads(1)
                                        .build();
        SavedModelBundle.Loader loader = SavedModelBundle.loader(modelLocation).withTags("serve")
                                                         .withConfigProto(config.toByteArray());
        // TODO: call close()
        SavedModelBundle savedModelBundle = loader.load();
        session = savedModelBundle.session();
        // Retrieve input and output names for the tf graph
        metadata = MetaGraphDef.parseFrom(savedModelBundle.metaGraphDef());
        Map<String, Shape> nameToInput = getInputToShape(metadata);
        this.inputName = nameToInput.keySet().iterator().next();
        Shape inputShape = nameToInput.get(this.inputName);
        this.inputShapeSizes = new long[inputShape.numDimensions()];
        for (int i = 0; i < inputShape.numDimensions(); i++)
            inputShapeSizes[i] = inputShape.size(i);

        Map<String, Shape> nameToOutput = getOutputToShape(metadata);
        this.outputName = nameToOutput.keySet().iterator().next();
    }

    @Override
    public CrayfishPrediction apply(CrayfishInputData inputBatch) throws Exception {
        if (inputBatch == null || inputBatch.get() == null) return null;

        int batchSize = inputBatch.get().size();
        // tf models have the first dimension set to -1 to allow for arbitrary batching
        if (inputShapeSizes[0] == -1) inputShapeSizes[0] = batchSize;
        ArrayList<Float> flattenInput = new ArrayList<>(
                inputBatch.get().stream().flatMap(List::stream).collect(Collectors.toList()));
        // Run the inputs through the Tensorflow model
        Tensor inputTensor = Tensor.create(inputShapeSizes, FloatBuffer.wrap(
                ArrayUtils.toPrimitive(flattenInput.toArray(new Float[0]), 0.0F)));
        Tensor modelResult = session.runner().feed(inputName, inputTensor).fetch(outputName).run().get(0);
        float[][] outputArr = (float[][]) modelResult.copyTo(
                new float[(int) modelResult.shape()[0]][(int) modelResult.shape()[1]]);

        return new CrayfishPrediction(CrayfishUtils.convertToArrayListBatch(outputArr));
    }


    private Map<String, Shape> getInputToShape(MetaGraphDef metadata) {
        Map<String, Shape> result = new HashMap<>();
        SignatureDef servingDefault = getServingSignature(metadata);
        for (Map.Entry<String, TensorInfo> entry : servingDefault.getInputsMap().entrySet()) {
            TensorShapeProto shapeProto = entry.getValue().getTensorShape();
            List<TensorShapeProto.Dim> dimensions = shapeProto.getDimList();
            long firstDimension = dimensions.get(0).getSize();
            long[] remainingDimensions = dimensions.stream().skip(1).mapToLong(TensorShapeProto.Dim::getSize).toArray();
            Shape shape = Shape.make(firstDimension, remainingDimensions);
            result.put(entry.getValue().getName(), shape);
        }
        return result;
    }

    private Map<String, Shape> getOutputToShape(MetaGraphDef metadata) {
        Map<String, Shape> result = new HashMap<>();
        SignatureDef servingDefault = getServingSignature(metadata);
        for (Map.Entry<String, TensorInfo> entry : servingDefault.getOutputsMap().entrySet()) {
            TensorShapeProto shapeProto = entry.getValue().getTensorShape();
            List<TensorShapeProto.Dim> dimensions = shapeProto.getDimList();
            long firstDimension = dimensions.get(0).getSize();
            long[] remainingDimensions = dimensions.stream().skip(1).mapToLong(TensorShapeProto.Dim::getSize).toArray();
            Shape shape = Shape.make(firstDimension, remainingDimensions);
            result.put(entry.getValue().getName(), shape);
        }
        return result;
    }

    private SignatureDef getServingSignature(MetaGraphDef metadata) {
        return metadata.getSignatureDefOrDefault("serving_default", getFirstSignature(metadata));
    }

    private SignatureDef getFirstSignature(MetaGraphDef metadata) {
        Map<String, SignatureDef> nameToSignature = metadata.getSignatureDefMap();
        if (nameToSignature.isEmpty()) return null;
        return nameToSignature.get(nameToSignature.keySet().iterator().next());
    }

    @Override
    public String toString() {
        return "TensorflowSavedModel{" + "session=" + session + ", inputName='" + inputName + '\'' +
               ", inputShapeSizes=" + Arrays.toString(inputShapeSizes) + ", outputName='" + outputName + '\'' + '}';
    }
}