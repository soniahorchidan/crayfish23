import json

import ray


@ray.remote
class OutputActor(object):
    def __init__(self, kafka, output_topic):
        from confluent_kafka import Producer
        self.producer = Producer(kafka)
        self.topic = output_topic

    def forward(self, msg):
        # print("OutputActor: got msg", msg)
        out = json.dumps(msg).replace("\\\"", "\"")[1:-1]
        self.producer.produce(self.topic, key=None, value=out)
        self.producer.poll()
