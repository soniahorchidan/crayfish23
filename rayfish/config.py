import yaml
from jproperties import Properties


class ConfigParser():
    def __init__(self, args):
        model_config_path = args.mconf
        exp_config_path = args.econf
        e_prop = Properties()

        with open(exp_config_path, 'rb') as econfig_file:
            e_prop.load(econfig_file)
        with open(model_config_path, 'r') as stream:
            self.MODEL_CONFIG = yaml.safe_load(stream)

        if 'gconf' in args:
            global_config_path = args.gconf
            g_prop = Properties()
            with open(global_config_path, 'rb') as gconfig_file:
                g_prop.load(gconfig_file)
                self.BOOTSTRAP_SERVER = g_prop.get('kafka.bootstrap.servers').data
                self.INPUT_DATA_TOPIC = g_prop.get('kafka.input.data.topic').data
                self.OUTPUT_TOPIC = g_prop.get('kafka.output.topic').data
                self.KAFKA_USERNAME = g_prop.get('kafka.auth.username').data
                self.KAFKA_PASSWORD = g_prop.get('kafka.auth.password').data
                self.KAFKA_MAX_REQ = int(g_prop.get('kafka.max.req.size').data)
        self.MODEL_FORMAT = args.mf

        self.EXPERIMENT_CONFIG = {}
        for k in e_prop.keys():
            self.EXPERIMENT_CONFIG[k] = e_prop.get(k).data

    def get_kafka_producer_config(self):
        conf = {'bootstrap.servers': self.BOOTSTRAP_SERVER,
                'message.max.bytes': self.KAFKA_MAX_REQ,
                'sasl.mechanisms': 'PLAIN',
                'security.protocol': 'SASL_PLAINTEXT',
                'sasl.username': self.KAFKA_USERNAME,
                'sasl.password': self.KAFKA_PASSWORD}

        # TODO: add the password and username from configuration file
        return conf

    def get_kafka_consumer_config(self):
        conf = {'bootstrap.servers': self.BOOTSTRAP_SERVER,
                'group.id': 'ray',
                'fetch.message.max.bytes': self.KAFKA_MAX_REQ,
                'max.partition.fetch.bytes': self.KAFKA_MAX_REQ,
                # 'auto.offset.reset': 'earliest',
                'sasl.mechanisms': 'PLAIN',
                'security.protocol': 'SASL_PLAINTEXT',
                'sasl.username': self.KAFKA_USERNAME,
                'sasl.password': self.KAFKA_PASSWORD}

        # TODO: add the password and username from configuration file
        return conf

    def get_experiment_config(self):
        return self.EXPERIMENT_CONFIG

    def get_model_config(self):
        return self.MODEL_CONFIG

    def get_model_format(self):
        return self.MODEL_FORMAT

    def get_kafka_input_topic(self):
        return self.INPUT_DATA_TOPIC

    def get_kafka_output_topic(self):
        return self.OUTPUT_TOPIC
