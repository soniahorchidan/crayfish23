from crayfish.pycrayfish.api.datatypes.models.CrayfishModel import CrayfishModel
from crayfish.pycrayfish.api.datatypes.datapoints.data.CrayfishInputData import CrayfishInputData
from crayfish.pycrayfish.api.datatypes.datapoints.data.CrayfishPrediction import CrayfishPrediction
from crayfish.pycrayfish.api.request.InferenceRequest import InferenceRequest
import torch
import tensorflow as tf


class PySparkModel(CrayfishModel):
    def __init__(self):
        self.model = None
        self.location = None

    def loadModel(self, location):
        self.location = location

    def build(self, model_format) -> None:
        if model_format == 'tf':
            self.model = tf.keras.models.load_model(self.location)
        elif model_format == 'torch':
            self.model = torch.jit.load(self.location)

    def apply(self, input: CrayfishInputData) -> CrayfishPrediction:
        pass # need to implement apply which inferences the model


