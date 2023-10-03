import os
from argparse import ArgumentParser

import ray
from ray import serve

from RayServeEmbeddedCrayfishAdapter import RayServeEmbeddedCrayfishAdapter
from RayServeExternalCrayfishAdapter import RayServeExternalCrayfishAdapter
from config import ConfigParser


def driver():
    parser = ArgumentParser()
    parser.add_argument('-mf', type=str, required=True)  # MODEL_FORMAT
    parser.add_argument('-mn', type=str, required=True)  # MODEL_NAME
    parser.add_argument('-mconf', type=str, required=True)  # MODEL_CONFIG
    parser.add_argument('-gconf', type=str, required=True)  # GLOBAL_CONFIGS
    parser.add_argument('-econf', type=str, required=True)  # EXP_CONFIGS
    conf = ConfigParser(parser.parse_args())

    # os.environ["RAY_LOG_TO_STDERR"] = "1"
    memory_bound = 1024*1024*1024*120
    ray.init(_memory=memory_bound, address="auto", namespace="serve", log_to_driver=True)

    model_format = conf.get_model_format()
    adapter = None
    if model_format == 'onnx':
        adapter = RayServeEmbeddedCrayfishAdapter(conf)
    elif model_format == 'tf-serving':
        adapter = RayServeExternalCrayfishAdapter(conf)
    else:
        print("Model format not supported!")
        exit(0)

    adapter.run()
    serve.shutdown()


driver()
