from abc import ABC, abstractmethod
from crayfish.pycrayfish.api.datatypes.datapoints.data.CrayfishPrediction import CrayfishPrediction
from crayfish.pycrayfish.api.datatypes.datapoints.data.CrayfishInputData import CrayfishInputData


class CrayfishModel(ABC):

    @abstractmethod
    def loadModel(self, location):
        raise NotImplementedError('loadModel(location) Not Implemented')

    @abstractmethod
    def build(self, model_format) -> None:
        """ Build the model from the pretrained model weight
            currently tensorflow saved model and torch models are supported

        Keyword arguments:
        model_format -- tensorflow or torch

        """
        raise NotImplementedError('build() Not Implemented')

    @abstractmethod
    def apply(self, input: CrayfishInputData) -> CrayfishPrediction:
        """ Applies the model on the data point received from the input stream.

        Keyword arguments:
        input -- the input data point or batch

        Return:
        the inference result

        """
        raise NotImplementedError('apply() Not Implemented')
