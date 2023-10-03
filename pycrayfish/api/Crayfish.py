from abc import ABC, abstractmethod


class Crayfish(ABC):
    @abstractmethod
    def __init__(self, global_config_path, experiment_config_path, mode, endpoint):
        self.input_actor = None
        self.scoring_actor = None
        self.output_actor = None
        raise NotImplementedError(
            'Constructor Not Implemented. Please initialize input_actor, scoring_actor, and output_actor.')

    @abstractmethod
    def stream_builder(self):
        raise NotImplementedError('inputOP() Not Implemented')

    @abstractmethod
    def input_op(self, stream_builder, next_op):
        raise NotImplementedError('inputOP() Not Implemented')

    @abstractmethod
    def scoring_op(self, input, next_op):
        raise NotImplementedError('scoringOP() Not Implemented')

    @abstractmethod
    def output_op(self, output):
        raise NotImplementedError('outputOP() Not Implemented')

    @abstractmethod
    def start(self, stream_builder) -> None:
        raise NotImplementedError('start() Not Implemented')

    def run(self) -> None:
        stream_builder = self.stream_builder()
        input_stream = self.input_op(stream_builder, self.scoring_actor)
        output_stream = self.scoring_op(input_stream, self.output_actor)
        self.output_op(output_stream)
        self.start(stream_builder)
