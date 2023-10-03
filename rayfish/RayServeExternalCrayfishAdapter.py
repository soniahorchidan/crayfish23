import ray

from RayInputActor import InputActor
from RayOutputActor import OutputActor
from RayScoringExternalActor import ExternalScoringActor


class RayServeExternalCrayfishAdapter:
    def __init__(self, conf):
        self.kafka_producer_config = conf.get_kafka_producer_config()
        self.kafka_consumer_config = conf.get_kafka_consumer_config()
        self.input_topic = conf.get_kafka_input_topic()
        self.output_topic = conf.get_kafka_output_topic()
        self.exp_config = conf.get_experiment_config()
        self.model_config = conf.get_model_config()
        self.model_format = conf.get_model_format()

    def run(self):
        if self.model_format == 'tf-serving':
            # NOTE: load tf-savedmodel model
            model_path = self.model_config["model.path.tf-savedmodel"]
        else:
            print("Model format not supported")
            exit(0)

        model_path = '.' + model_path  # TODO fix the relative path issue

        model_replicas = int(self.exp_config["model_replicas"])
        cpus_num = 0.33 * model_replicas
        if cpus_num < 1:
            cpus_per_actor = cpus_num
        else:
            cpus_per_actor = int(cpus_num)

        # Deploy classic Ray actors and create DA
        output_actors = []
        scoring_actors = []
        input_actors = []
        for _ in range(model_replicas):
            output_actor = OutputActor.options(num_cpus=cpus_per_actor).remote(self.kafka_producer_config,
                                                                               self.output_topic)
            output_actors.append(output_actor)

            scoring_actor = ExternalScoringActor.options(num_cpus=cpus_per_actor).remote(output_actor)
            scoring_actors.append(scoring_actor)

            input_actor = InputActor.options(num_cpus=cpus_per_actor).remote(self.kafka_consumer_config,
                                                                             self.input_topic, scoring_actor)
            input_actors.append(input_actor)

        try:
            refs = [input_actor.forward.remote() for input_actor in input_actors]
            ray.get(refs)
        except KeyboardInterrupt:
            for input_actor in input_actors:
                input_actor.stop.remote()
            for output_actor in output_actors:
                output_actor.stop.remote()
            for scoring_actor in scoring_actors:
                scoring_actor.stop.remote()
