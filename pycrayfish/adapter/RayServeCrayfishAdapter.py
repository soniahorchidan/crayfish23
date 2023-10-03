from jproperties import Properties

from pycrayfish.api.Crayfish import Crayfish
from pycrayfish.api.datatypes.models.ray import RayInputActor, RayScoringActor, RayOutputActor


# TODO: implement properly; this is only a skeleton
class RayServeCrayfishAdapter(Crayfish):

    def __init__(self, model_endpoint, global_config_path, experiment_config_path):

        global_config = Properties()
        with open(global_config_path, 'rb') as config_file:
            global_config.load(config_file)

        self.model_endpoint = model_endpoint
        self.BOOTSTRAP_SERVER = global_config.get('kafka.bootstrap.servers').data
        self.INPUT_DATA_TOPIC = global_config.get('kafka.input.data.topic').data
        self.OUTPUT_TOPIC = global_config.get('kafka.output.topic').data

        experiment_config = Properties()
        with open(experiment_config, 'rb') as config_file:
            experiment_config.load(config_file)

        self.EMBEDDED_MODEL_REPLICAS = int(experiment_config.get('model_replicas').data)

        try:
            self.ASYNC_OPERATOR_TIMEOUT = int(experiment_config.get('async_op_capacity').data)
        except KeyError as _:
            self.ASYNC_OPERATOR_TIMEOUT = 500000

        self.input_actor = RayInputActor.remote()
        self.scoring_actor = RayScoringActor.remote()
        self.scoring_actor.build()  # params...
        self.output_actor = RayOutputActor.remote()

    def stream_builder(self):
        pass

    def input_op(self, stream_builder, next_op):
        self.input_actor.set_next_op_handler(next_op)
        self.input_actor.start() # TODO: why do we need both calls?
        self.input_actor.process()

    def scoring_op(self, input, next_op):
        self.scoring_actor.set_next_op_handler(next_op)

    def output_op(self, output):
        pass

    def start(self) -> None:
        self.input_actor.deploy()
        self.scoring_actor.deploy()
        self.output_actor.deploy()
