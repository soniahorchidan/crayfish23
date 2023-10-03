class CrayfishInputData:
    def __init__(self, datapointBatch):
        self.datapointBatch = datapointBatch

    def get(self):
        return self.datapointBatch

    def toString(self):
        return "DataBatch{datapointBatch=" + self.datapointBatch + "}"
