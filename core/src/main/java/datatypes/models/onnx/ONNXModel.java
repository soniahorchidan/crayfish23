package datatypes.models.onnx;

import ai.onnxruntime.*;
import datatypes.datapoints.CrayfishInputData;
import datatypes.datapoints.CrayfishPrediction;
import datatypes.models.CrayfishModel;
import org.apache.commons.lang3.ArrayUtils;
import utils.CrayfishUtils;

import java.io.File;
import java.io.Serializable;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class used to load and apply a model saved in ONNX format.
 */
// TODO: do we need to close the resources?
// https://labs.oracle.com/pls/apex/f?p=LABS:0:107525856594211:APPLICATION_PROCESS=GETDOC_INLINE:::DOC_ID:2880
public class ONNXModel extends CrayfishModel implements Serializable {
    private transient OrtSession session = null;
    private transient OrtEnvironment env = null;
    private transient long[] inputShapeSizes;
    private byte[] modelContent;

    public void loadModel(String modelName, String modelPath) throws Exception {
        // Assumes there is only one  file containing the model
        // TODO: fix
        File modelLocation = new File(modelPath);
        File modelFilePath = null;

        // Pick the first .onnx file found
        for (File f : modelLocation.listFiles()) {
            if (f.isFile() && f.getName().contains("onnx")) {
                modelFilePath = f;
                break;
            }
        }

        if (modelFilePath == null) throw new RuntimeException("Model not found!");
        this.modelContent = Files.readAllBytes(modelFilePath.toPath());
    }

    @Override
    public void build() throws Exception {
        // Get the current ONNX runtime environment if not specified
        OrtSession.SessionOptions sessionOptions = getSessionOptions();

        // Load the ONNX model
        this.session = env.createSession(modelContent, sessionOptions);

        // Retrieve information about the model
        NodeInfo inputInfo = this.session.getInputInfo().values().iterator().next();
        TensorInfo tensorInfo = (TensorInfo) inputInfo.getInfo();
        this.inputShapeSizes = tensorInfo.getShape();
    }

    private OrtSession.SessionOptions getSessionOptions() throws OrtException {
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
        sessionOptions.setInterOpNumThreads(1);
        sessionOptions.setIntraOpNumThreads(1);
        return sessionOptions;
    }

    @Override
    public CrayfishPrediction apply(CrayfishInputData inputBatch) throws Exception {
        // TODO: refactor so that it does not have to be in all model implementations
        if (inputBatch == null || inputBatch.get() == null) return null;

        ArrayList<ArrayList<Float>> input = inputBatch.get();
        int batchSize = input.size();
        ArrayList<Float> flattenInput = new ArrayList<>(
                input.stream().flatMap(List::stream).collect(Collectors.toList()));
        // onnx models have the first dimension set to -1 to allow for arbitrary batching
        if (this.inputShapeSizes[0] == -1) this.inputShapeSizes[0] = batchSize;
        OnnxTensor inputONNXTensor = OnnxTensor.createTensor(this.env, FloatBuffer.wrap(
                ArrayUtils.toPrimitive(flattenInput.toArray(new Float[0]), 0.0F)), this.inputShapeSizes);
        Map<String, OnnxTensor> onnxInputs = new HashMap<>();
        String modelInputNames = this.session.getInputNames().iterator().next(); // assumes only one input
        onnxInputs.put(modelInputNames, inputONNXTensor);
        // Run the inputs through the ONNX model
        OnnxValue onnxValueResults = this.session.run(onnxInputs).get(0);
        float[][] onnxOutput = (float[][]) onnxValueResults.getValue();
        return new CrayfishPrediction(CrayfishUtils.convertToArrayListBatch(onnxOutput));
    }

    @Override
    public String toString() {
        return "ONNXModel{" + "session=" + session + ", env=" + env + ", inputShapeSizes=" +
               Arrays.toString(inputShapeSizes) + '}';
    }
}
