'''
Adapted from Confluence Example
'''
import socket

from ray import serve


@serve.deployment
def get_input(input):
    pass


@serve.deployment
class RayProducer:
    def __init__(self, kafka, sink):
        from confluent_kafka import Producer
        producer_config = {'bootstrap.servers': kafka.BOOTSTRAP_SERVER,
                           'client.id': socket.gethostname()}
        self.producer = Producer(producer_config)
        self.sink = sink

    def produce(self, input):
        self.producer.produce(self.sink, input)
        self.producer.poll(0)

    def destroy(self):
        self.producer.flush(30)


@serve.deployment
class RayConsumer(object):
    def __init__(self, kafka, source):
        from confluent_kafka import Consumer
        consumer_config = {
            'bootstrap.servers': kafka.BOOTSTRAP_SERVER,
            'group.id': 'ray',
            'enable.auto.commit': True,
            'auto.offset.reset': 'earliest',
        }
        self.consumer = Consumer(consumer_config)
        self.consumer.subscribe([source])

    def start(self):
        self.run = True
        while self.run:
            msg = self.consumer.poll(1.)
            if msg is None:
                continue
            if msg.error():
                print('Consumer Error: {}'.format(msg.error()))
                continue
            # NEED TO QUERY from deployment

    def stop(self):
        self.run = False

    def destroy(self):
        self.consumer.close()
