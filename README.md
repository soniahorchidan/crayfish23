# Crayfish: Benchmarking Model Serving in Data Streaming

## Project Structure

```
.
├── core                  # Crayfish Java core components and abstractions.
├── crayfish-java         # Crayfish adapters (i.e., FLink, Spark Structured Streaming, Kafka Streams).
├── docker                # Scripts and configuration for Docker deployment.
├── experiments-driver    # Experiments testbed and configurations.
├── input-producer        # Input Producer Component. Contains a random input generator.
├── output-consumer       # Output Consumer Component. Writes latency measurements to persistent storage.
├── rayfish               # Crayfish Ray Adapter.
├── resources             # Pre-trained models and training scripts.  
└── results-analysis      # Notebooks to analyze the results.
```


## Quick Start

### Environment

1. Unix-like environment
2. Python 3
3. Maven
4. Java 8
5. Docker installation


### Run Locally
```bash
# train ffnn/resnet50 models in different model formats
./resources/train-models.sh

# build images with the trained models
./docker/docker-build.sh

# run all experiments locally (to run a single experiment, see the options below)
./run.sh -e a -ec s -sp a -m a -msm a -msx a -em l

# clean all the running and exited containers and created volumes if you quit before the experiment finishes
./docker/docker-stop.sh

# clean log, results, and models trained by ./resources/train-models.sh
./clean-exp-files.sh
```

`run.sh` has the following options:
```
Arguments:
                                       [!] NOTE: Configs in 'experiments-driver/configs/exp-configs/[-ec]/[-e]' will be run.
-e     | --experiments                 Independent variable sets to run: a=all, i=input rate, b=batch size, s=scalability.
-ec    | --experiments-control         Controlled variable sets to run: s=small, l=large, d=debug.
                                         - small: Run input rate 256 for the scalability experiment.
                                           [!] NOTE: ResNet50 is recommended for this option due to the large
                                                     model size and limited memory.
                                         - large: Run input rate 30000 for the scalability experiment.
                                         - debug: Run simple experiment configs in the debug folder.
-sp    | --stream-processors           Stream processor to test:
                                         a=all, f=Apache Flink, k=Kafka Streams, s=Spark Streaming, r=Ray.
-m     | --models                      Served models: a=all, f=ffnn, r=resnet50.
-msm   | --embedded-model-servers      Embedded model serving alternative:
                                         x=none, a=all (w/o noop and nd4j), n=nd4j, d=dl4j, o=onnx, t=tf-savedmodel, k=noop.
                                         [!] NOTE: noop will execute input rate and batch size experiments.
-msx   | --external-model-servers      External model serving alternative: x=none, a=all, t=tf-serving, s=torchserve.
-em    | --execution-mode              Execution mode: l=local, c=cluster.
-d     | --default-configs             Print default configs.
-h     | --help                        Help.
```

### Run on Cluster
To adapt the experiment to the cluster,

1. Build the required docker images on different VMs and train models (commands are from `./docker/docker-build.sh`)
    1. Kafka consumer VM
        ```
        docker compose build output-consumer --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g)
        ```
    1. Data processor
        ```
        ./resources/train-models.sh
        docker compose build data-processor --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g) 
        ```
    1. Kafka producer VM
        ```
        docker compose build input-producer --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g)
        ```
    1. External serving VM
        ```
        ./resources/train-models.sh
        docker compose build external-serving-agent --build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g)
        docker compose build torch-serving
        docker compose build tf-serving
        docker compose build ray-serving
        ```
1. Open the respective docker container on different VMs manually. The order matters. (commands are from `./run.sh`)
    1. Kafka producer VM
        ```
        docker compose run -d --service-ports --use-aliases -u $(id -u):$(id -g) input-producer \
            /bin/bash -c "./experiments-driver/scripts/start-daemon.sh -p kp -em 'l' --port $PORT_KP >logs/$EXP_UNIQUE_ID-kp-daemon-logs.txt 2>&1" 
        ```
    1. Data processor
        ```
        docker compose run -d --service-ports --use-aliases -u $(id -u):$(id -g) data-processor \
            /bin/bash -c "./experiments-driver/scripts/start-daemon.sh -p dp -em 'l' --port $PORT_DP >logs/$EXP_UNIQUE_ID-dpd-daemon-logs.txt 2>&1"
        ```
    1. External serving VM
        ```
        docker compose run -u $(id -u):$(id -g) output-consumer \
            /bin/bash -c "./experiments-driver/scripts/start-consumer.sh \
            -e $EXPERIMENTS_OPT \
            -ec $EXPERIMENTS_CTRL_OPT \
            -sp $STREAM_PROCESSOR_OPT \
            -m $MODELS_OPT \
            -msm $EMBEDDED_MODEL_SERVERS_OPT \
            -msx $EXTERNAL_MODEL_SERVERS_OPT \
            -em $EXECUTION_MODE_OPT \
            -eid $EXP_UNIQUE_ID"
        ```
    1. Kafka consumer VM
        ```
        docker compose run -u $(id -u):$(id -g) output-consumer \
            /bin/bash -c "./experiments-driver/scripts/start-consumer.sh \
            -e $EXPERIMENTS_OPT \
            -ec $EXPERIMENTS_CTRL_OPT \
            -sp $STREAM_PROCESSOR_OPT \
            -m $MODELS_OPT \
            -msm $EMBEDDED_MODEL_SERVERS_OPT \
            -msx $EXTERNAL_MODEL_SERVERS_OPT \
            -em $EXECUTION_MODE_OPT \
            -eid $EXP_UNIQUE_ID"
        ```
1. Update ip addresses for each VM in `experiments-driver/configs/global-configs-cluster.properties`

### Kafka Overhead Experiments

The experiments measuring the overhead introduced by Kafka in the pipeline do not use the Crayfish pipeline, as they
employ a standalone FLink implementation. To run these experiments, the following script can be used:

```bash
./experiments-driver/run-standalone.sh -em l -ec l
```

`run-standalone.sh` has the following options:

```
Arguments:
-ec    | --experiments-control         Controlled variable sets to run: s=small, l=large, d=debug.
                                         - small: Run input rate 256 for the scalability experiment.
                                           [!] NOTE: ResNet50 is recommended for this option due to the large
                                                     model size and limited memory.
                                         - large: Run input rate 30000 for the scalability experiment.
                                         - debug: Run simple experiment configs in the debug folder.
-em    | --execution-mode              Execution mode: l=local, c=cluster.
```

## Evaluation

### Pre-trained Models Configurations

<table>
  <caption></caption>
  <thead>
    <tr>
      <th colspan="2"></th>
      <th>FNN</th>
      <th>ResNet50</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td colspan="2">Input Size</td>
      <td>28x28</td>
      <td>224x224x3</td>
    </tr>
    <tr>
      <td colspan="2">Output Size</td>
      <td>10x1</td>
      <td>1000x1</td>
    </tr>
    <tr>
      <td colspan="2">Parameters Number</td>
      <td>28K</td>
      <td>23M</td>
    </tr>
    <tr>
      <td rowspan="4">Model Size</td>
      <td>ONNX</td>
      <td>113 KB</td>
      <td>97 MB</td>
    </tr>
    <tr>
      <td>Torch</td>
      <td>115 KB</td>
      <td>98 MB</td>
    </tr>
    <tr>
      <td>H5</td>
      <td>133 KB</td>
      <td>98 MB</td>
    </tr>
    <tr>
      <td>SavedModel</td>
      <td>508 KB</td>
      <td>101 MB</td>
    </tr>
  </tbody>
</table>

### Considered Tools

- Apache Flink 1.15.2
- Apache Kafka Streams 3.2.3
- Spark Structured Streaming 3.3.2
- Ray 2.4
- ONNX 1.12.1
- DeepLearning4j 1.0.0-M2.1
- TensorFlow Java 1.13.1
- TorchServe 0.7.1
- TensorFlow Serving 2.11.1

### Deployment Configuration on Google Cloud Platform

All the machines were equipped with Intel(R) Xeon(R) CPU @ 2.20GHz.
<table>
    <thead>
        <tr>
            <th>Virtual Machine</th>
            <th>vCPUs</th>
            <th>RAM (GB)</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>(4 x) Kafka Broker</td>
            <td>4</td>
            <td>15</td>
        </tr>
        <tr>
            <td>Zookeeper</td>
            <td>2</td>
            <td>8</td>
        </tr>
        <tr>
            <td>Input Producer</td>
            <td>32</td>
            <td>28</td>
        </tr>
        <tr>
            <td>Output Consumer</td>
            <td>4</td>
            <td>15</td>
        </tr>
        <tr>
            <td>Stream Processor</td>
            <td>64</td>
            <td>240</td>
        </tr>
        <tr>
            <td>External Server</td>
            <td>16</td>
            <td>60</td>
        </tr>
    </tbody>
</table>