import numpy as np
import onnxruntime as rt
import ray
import json


@ray.remote
class EmbeddedScoringActor(object):
    def __init__(self, batch_size, input_size, next_actor):
        model_path = '/home/user/crayfish/resources/embedded/ffnn/onnx/model-1/model-ffnn.onnx'
        options = rt.SessionOptions()
        options.inter_op_num_threads = 1
        options.intra_op_num_threads = 1
        self.model = rt.InferenceSession(model_path, sess_options=options)
        self.input_name = self.model.get_inputs()[0].name
        self.label_name = self.model.get_outputs()[0].name

        self.input_shape = [0]
        self.input_shape = (batch_size, np.prod(input_size))
        self.next_actor = next_actor

    def forward(self, msg):
        # print("EmbeddedScoringActor: forward", msg)
        minibatch = np.array(msg['dataPoint'].split(',')[:-1]).astype(np.float32)
        reshaped_array = minibatch.reshape((self.input_shape[0], 1, self.input_shape[1]))
        prediction = self.model.run([self.label_name], {self.input_name: reshaped_array})[0]
        msg["prediction"] = str(prediction)
        self.next_actor.forward.remote(json.dumps(msg))
