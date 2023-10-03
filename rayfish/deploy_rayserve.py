import os
import ray
from argparse import ArgumentParser
from ray import serve

from RayServeInstance import RayServeInstance
from config import ConfigParser


def driver():
    parser = ArgumentParser()
    parser.add_argument('-mf', type=str, required=True)  # MODEL_FORMAT
    parser.add_argument('-mconf', type=str, required=True)  # MODEL_CONFIG
    parser.add_argument('-econf', type=str, required=True)  # EXP_CONFIGS
    conf = ConfigParser(parser.parse_args())
    model_config = conf.get_model_config()
    exp_config = conf.get_experiment_config()
    model_format = conf.get_model_format()
    # model_path = '.' + model_path  # TODO fix the relative path issue

    model_type = model_config["model.name"]
    input_shape = [int(d) for d in model_config['input.shape'].split(',')]
    batch_size = int(exp_config["batch_size"])
    model_replicas = int(exp_config["model_replicas"])

    os.environ["RAY_LOG_TO_STDERR"] = "1"
    ray.init(address="auto", namespace="serve", log_to_driver=False)
    serve.start(detached=True, http_options={"host": "0.0.0.0"})

    cpus_num = 0.33 * model_replicas
    if cpus_num < 1:
        cpus_per_actor = cpus_num
    else:
        cpus_per_actor = int(cpus_num)

    RayServeInstance.options(num_replicas=model_replicas,
                             ray_actor_options={"num_cpus": cpus_per_actor, "num_gpus": 0},
                             name='serve_instance') \
        .deploy("", batch_size, input_shape, model_type, model_format, None)


driver()
