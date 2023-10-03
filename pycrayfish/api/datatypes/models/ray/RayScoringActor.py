from typing import List

import numpy as np
import ray as serve
import tensorflow as tf
import torch

from pycrayfish.api.datatypes.models.CrayfishModel import CrayfishModel


@serve.deployment(route_prefix="/model")
class RayScoringActor(CrayfishModel):
    def __init__(self):
        self.input_shape = None
        self.location = None
        self.model = None
        self.next_op_handler = None

    def loadModel(self, location):
        self.location = location

    # TODO: can we move this to the constructor?
    def build(self, input_size, batch_size, model_name, model_format):
        if model_format == 'tf':
            self.model = tf.keras.models.load_model(self.location)
        elif model_format == 'torch':
            self.model = torch.jit.load(self.location)
        if model_name == 'ffnn':
            self.input_shape = (batch_size, np.prod(input_size))  # required shape for FFNN
        else:
            self.input_shape = (batch_size,) + tuple(input_size[::-1])  # required shape for RESNET50 and VGG19

    @serve.batch
    async def process(self, requests: List) -> List:
        if self.next_op_handler:
            batch_result = []
            for request in requests:
                input_array = np.array((await request.json())["array"])
                reshaped_array = input_array.reshape(self.input_shape)
                # TODO: call apply somewhere here
                pred = self.model(reshaped_array)
                batch_result.append({"prediction": pred.numpy(), })
            self.next_op_handler.process(batch_result)

    # def apply(self, input: CrayfishInputData) -> CrayfishPrediction:
    #     pass

    def set_next_op_handler(self, handler):
        self.next_op_handler = handler

    # TODO: why is this needed?
    async def __call__(self, starlette_request):
        return self.process(starlette_request)
