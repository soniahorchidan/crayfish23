import json
import random

import numpy as np
import tensorflow as tf
from ray import serve
from starlette.requests import Request


@serve.deployment(route_prefix="/model")
class RayServeInstance(object):
    def __init__(self, model_path, batch_size, input_size, model_type, model_format, next_actors):
        self.model_format = model_format
        if model_format == 'tf-serving':
            tf.config.threading.set_inter_op_parallelism_threads(1)
            tf.config.threading.set_intra_op_parallelism_threads(1)
            self.model = tf.saved_model.load(
                '/home/user/crayfish/resources/embedded/ffnn/tf-savedmodel/model-1')

        self.input_shape = [0]
        if model_type == 'ffnn':
            self.input_shape = (batch_size, np.prod(input_size))
        else:
            self.input_shape = (batch_size,) + tuple(input_size[::-1])
        self.next_actors = next_actors

    def forward(self, msg):
        scored = self.score(msg)
        # load balancing
        next_actor = random.choice(self.next_actors)
        next_actor.forward.remote(scored)

    async def __call__(self, request: Request) -> str:
        data = await request.json()
        return self.score(data)

    def score(self, obj):
        minibatch = np.array(obj['dataPoint'].split(',')[:-1]).astype(np.float32)
        if self.model_format == 'tf-serving':
            reshaped_array = minibatch.reshape(self.input_shape)
            prediction = self.model(reshaped_array)
            obj["prediction"] = str(prediction)
        else:
            exit(0)
        return json.dumps(obj)
