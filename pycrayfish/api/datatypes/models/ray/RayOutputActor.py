import socket

import ray as serve
from confluent_kafka import Producer


@serve.deployment
class RayOutputActor:
    def __init__(self, kafka, sink):
        producer_config = {'bootstrap.servers': kafka.BOOTSTRAP_SERVER,
                           'client.id': socket.gethostname()}
        self.producer = Producer(producer_config)
        self.sink = sink
        self.run = False

    def start(self):
        self.run = True

    def process(self, input):
        if self.run:
            self.producer.produce(self.sink, input)
            self.producer.poll(0)

    def stop(self):
        self.run = False

    def destroy(self):
        self.producer.flush(30)
