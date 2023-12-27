# Crayfish: Navigating the Labyrinth of Machine Learning Inference in Stream Processing Systems

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

## Supported Tools

### Pre-trained Models

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

### Stream Processors

- Apache Flink 1.15.2
- Apache Kafka Streams 3.2.3
- Spark Structured Streaming 3.3.2
- Ray 2.4

### Embedded Serving Frameworks

- ONNX 1.12.1
- DeepLearning4j 1.0.0-M2.1
- TensorFlow Java (SavedModel) 1.13.1

### External Serving Frameworks
- TorchServe 0.7.1
- TensorFlow Serving 2.11.1

## Quick Start

### Environment

1. Unix-like environment
2. Python 3
3. Maven
4. Java 8
5. Docker installation


### Experiments
We offer an option to test the tools locally before deploying to a cluster. 

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
-e     | --experiments                 Independent variable sets to run: a=all, i=input rate, b=batch size, s=scalability, r=bursty rate.
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
To adapt the experiments to be executed on the cluster,

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
1. Update the IP addresses for each VM in `experiments-driver/configs/global-configs-cluster.properties`

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

## Extending Crayfish

Crayfish provides a set of interfaces that allow developers to extend the benchmarking frameworks with other stream processing systems, model serving tools, and pre-trained models. We showcase examples of how to do so for each type of system.

### New Stream Processors

New stream processors can be added through abstractions similar to adapters. These adapters need to extend the Crayfish interface and provide functionalities for input reading, model scoring, and output writing alongside logic to set the parallelism of the computation. The adapter needs to provide also abstractions for a stream builder, the generic operator type, the sink operator, and the model type to serve. The model type can be chosen from the list of models supported out of the box or can be a custom model defined by the Crayfish user.

The basic adapter should extend the ```Crayfish``` abstract class, as below. Next, implement the required methods for building your streaming application.

```java
public class MyCrayfishAdapter extends Crayfish<MyStreamBuilder, MyOperatorType, MySinkType, MyModel> {
    // Implement required methods
    // ...

    @Override
    public MyStreamBuilder streamBuilder() {
        // Implement the logic to create and return the stream builder specific to the stream processor
    }

    @Override
    public MyOperatorType inputOp(MyStreamBuilder streamBuilder) {
        // Implement the logic to create and return a source operator
    }

    @Override
    public MyOperatorType embeddedScoringOp(MyStreamBuilder sb, MyOperatorType input) throws Exception {
        // Implement the logic for embedded ML serving
    }

    @Override
    public MyOperatorType externalScoringOp(MyStreamBuilder sb, MyOperatorType input) throws Exception {
        // Implement the logic for external ML serving
    }

    @Override
    public CrayfishUtils.Either<MyOperatorType, MySinkType> outputOp(MyStreamBuilder sb,
                                                                     MyOperatorType output) throws Exception {
        // Implement the logic for creating and returning output. Depending on the stream processor intrinsics,
        // return either the sink or another abstraction. This will be used in the start() method to start the
        // streaming application.
    }

    @Override
    public void start(MyStreamBuilder sb, Properties metaData,
                      CrayfishUtils.Either<MyOperatorType, MySinkType> out) throws Exception {
        // Implement the logic to start the processing
    }

    @Override
    public boolean hasOperatorParallelism() {
        // Return true if operator parallelism is supported
    }

    @Override
    public CrayfishUtils.Either<MyOperatorType, MySinkType> setOperatorParallelism(
            CrayfishUtils.Either<MyOperatorType, MySinkType> operator, int parallelism) throws Exception {
        // Implement the logic to set operator parallelism
        // Return null if not supported
    }

    @Override
    public void setDefaultParallelism(MyStreamBuilder sb, Properties metaData, int parallelism) throws Exception {
        // Implement the logic to set default parallelism
    }

    @Override
    public Properties addMetadata() throws Exception {
        // Implement the logic to add metadata. Some stream processing systems require extra configurations to be
        // set via Properties.
        // Return null if not required.
    }
}
```
