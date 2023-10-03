import time
from CrayfishPrediction import CrayfishPrediction
from CrayfishInputData import CrayfishInputData


def isTerminationToken():
    return False


class CrayfishDataBatch:

    def __init__(self, datapointBatch):
        self.predictions = None
        self.datapointBatch = datapointBatch
        self.creationTimestamp = time.time_ns() // 1_000_000  # millisecond

    def setPrediction(self, predictions):
        self.predictions = CrayfishPrediction(prediction=predictions)

    def getDatapointBatch(self):
        return CrayfishInputData(self.datapointBatch)

    def getCreationTimestamp(self):
        return self.creationTimestamp

    def getPredictions(self):
        return self.predictions

    def toString(self):
        return "CrayfishBatch{datapointBatch=" + \
               self.datapointBatch.toString() + \
               ", predictions=" + self.predictions.toString() + \
               "}"
