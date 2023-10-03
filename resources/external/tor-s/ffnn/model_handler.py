import ast
import os
from abc import ABC

import numpy as np
import torch
import yaml
from ts.torch_handler.base_handler import BaseHandler

torch.set_num_threads(1)

class CrayfishFFNNTorchServeModel(BaseHandler, ABC):

    def __init__(self):
        super(CrayfishFFNNTorchServeModel, self).__init__()

    def initialize(self, ctx):
        manifest = ctx.manifest
        properties = ctx.system_properties
        self.device = torch.device("cpu")
        model_dir = properties.get("model_dir")
        serialized_file = manifest['model']['serializedFile']
        model_path = os.path.join(model_dir, serialized_file)
        with open("model-config.yaml", "r") as f:
            try:
                input_shape = str(yaml.safe_load(f)['input.shape'])
                self.input_shape = [int(x) for x in input_shape.split(', ')]
            except yaml.YAMLError as exc:
                print(exc)
        self.model = torch.jit.load(model_path)
        self.model.to(self.device)
        self.model.eval()

    def preprocess(self, requests):
        processed = []
        for req in requests:
            datapoint = ast.literal_eval(req['body'].decode("utf-8"))
            datapoint = np.array(datapoint).astype(np.float32)
            processed.append(datapoint)
        return np.array(processed)

    def inference(self, data):
        ts_batch_size = data.shape[0]
        self.input_shape[0] = data.shape[1]
        input_batch = data.reshape(self.input_shape)
        input_batch = torch.from_numpy(input_batch)
        with torch.no_grad():
            input_batch = input_batch.to(self.device)
            out = self.model.forward(input_batch)
            out = out.reshape([ts_batch_size, -1])
            return [str(o) for o in out]

    def postprocess(self, inference_output):
        return inference_output
