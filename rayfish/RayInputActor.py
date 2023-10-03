import json

import ray


@ray.remote
class InputActor(object):
    def __init__(self, kafka, input_topic, next_actor):
        from confluent_kafka import Consumer
        self.consumer = Consumer(kafka)
        self.consumer.subscribe([input_topic])
        self.next_actor = next_actor

    def forward(self):
        while True:
            msg = self.consumer.poll(1.)
            if msg is None:
                continue
            if msg.error():
                continue
            obj = json.loads(msg.value())
            # print("InputActor: forward", obj)
            self.next_actor.forward.remote(obj)
