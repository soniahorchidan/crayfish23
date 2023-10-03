import ray as serve
from confluent_kafka import Consumer


@serve.deployment
class RayInputActor(object):
    def __init__(self, kafka, source):
        consumer_config = {
            'bootstrap.servers': kafka.BOOTSTRAP_SERVER,
            'group.id': 'ray',
            'enable.auto.commit': True,
            'auto.offset.reset': 'earliest',
        }
        self.consumer = Consumer(consumer_config)
        self.consumer.subscribe([source])
        self.next_op_handler = None
        self.run = False

    def start(self):
        self.run = True

    def set_next_op_handler(self, handler):
        self.next_op_handler = handler

    def process(self, handler):
        while self.run:
            msg = self.consumer.poll(1.)
            if msg is None:
                continue
            if msg.error():
                print('Consumer Error: {}'.format(msg.error()))
                continue
            self.next_op_handler.process(msg)

    # TODO: who calls this?
    def stop(self):
        self.run = False

    def destroy(self):
        self.consumer.close()
