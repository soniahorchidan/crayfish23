from crayfish.pycrayfish.api.Crayfish import Crayfish
from pyspark.sql import SparkSession
from jproperties import Properties
import torch


class PySparkCrayfishAdapter(Crayfish):

    def __init__(self, model_endpoint, global_config_path,
                 experiment_config_path):

        global_config = Properties()
        with open(global_config_path, 'rb') as config_file:
            global_config.load(config_file)

        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model_endpoint = model_endpoint
        self.BOOTSTRAP_SERVER = global_config.get('kafka.bootstrap.servers').data
        self.INPUT_DATA_TOPIC = global_config.get('kafka.input.data.topic').data
        self.OUTPUT_TOPIC = global_config.get('kafka.output.topic').data

        experiment_config = Properties()
        with open(experiment_config, 'rb') as config_file:
            experiment_config.load(config_file)

        self.EMBEDDED_MODEL_REPLICAS = int(experiment_config.get('model_replicas').data)

        try:
            self.ASYNC_OPERATOR_TIMEOUT = int(experiment_config.get('async_op_capacity').data)
        except KeyError as _:
            self.ASYNC_OPERATOR_TIMEOUT = 500000

    def streamBuilder(self):
        spark = SparkSession \
            .builder \
            .appName('PySpark Crayfish') \
            .getOrCreate()
        return spark

    def inputOP(self, spark, next_op):
        df = spark \
            .readStream \
            .format("kafka") \
            .option("kafka.bootstrap.servers", self.BOOTSTRAP_SERVER) \
            .option("subscribe", self.INPUT_DATA_TOPIC) \
            .load()
        return df

    def scoringOP(self, input, next_op):
        pass # need to inference the model and score

    def outputOP(self, output):
        sink = output.select("topic"). \
            writeStream.format("kafka"). \
            option("kafka.bootstrap.servers", self.BOOTSTRAP_SERVER) \
            .start()

    def start(self, streamBuilder) -> None:
        pass # need to start pyspark structured streaming
