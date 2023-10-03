class CrayfishPrediction:
    def __init__(self, prediction):
        self.prediction = prediction

    def get(self):
        return self.prediction

    def toString(self):
        return "Prediction{prediction=" + self.prediction + "}"
