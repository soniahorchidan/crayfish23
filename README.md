# Crayfish: Navigating the Labyrinth of Machine Learning Inference in Stream Processing Systems

This repository includes Crayfish, an extensible benchmarking framework that facilitates designing and executing comprehensive evaluation studies of streaming inference pipelines. Crayfish is described in our [EDBT'24 paper](https://openproceedings.org/2024/conf/edbt/paper-156.pdf), which also includes the first systematic performance evaluation study of model serving integration tools in Stream Processing Frameworks.

You can cite the paper using the BibTeX below:

```
@inproceedings{horchidan2024crayfish,
  title={Crayfish: Navigating the Labyrinth of Machine Learning Inference in Stream Processing Systems.},
  author={Horchidan, Sonia and Chen, Po-Hao and Kritharakis, Emmanouil and Carbone, Paris and Kalavri, Vasiliki},
  booktitle={EDBT},
  pages={676--689},
  year={2024}
}
```

## Project Structure

```
.
|-- core                   # Crayfish Java core components and abstractions.
|-- crayfish-java          # Crayfish adapters (i.e., FLink, Spark Structured Streaming, Kafka Streams).
|-- experiments-driver     # Experiments testbed and configurations.
|-- input-producer         # Input Producer Component. Contains a random input generator.      
|-- output-consumer        # Output Consumer Component. Writes latency measurements to persistent storage.   
|-- rayfish                # Crayfish Ray Adapter.   
|-- resources              # Pre-trained models and training scripts.  
`-- results-analysis       # Notebooks used to analyze the results.
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

### Prerequisites

#### Environment

1. Unix-like environment
2. Python 3
3. Maven
4. Java 8
5. Docker installation

#### Configuration

Before running the experiments, the models must be located under `resources/`. To train the models and save them in the
required formats, run `resources/training/convert_ffnn.py` for the FFNN model
and  `resources/training/convert_resnet50.py` for the ResNet50 model.

Crayfish provides two execution modes: _local_, to test the experiments on a single node, and _cluster_, to deploy the
Crayfish components on a cluster of machines. For each deployment, the configuration
files `experiments-driver/configs/global-configs-local.properties`
and `experiments-driver/configs/global-configs-cluster.properties` must be updated respectively.

**NOTE!** Do not forget to update the property files above to point to the local Apache Flink and Spark Structured Streaming 
installations.

### Quick start

#### Building and packaging

The first step in executing the experiments is to compile and package the project. To do so, run the following command
in  the Crayfish home directory.
```
mvn clean install
``` 

#### Preparing the Docker images

**NOTE!** Make sure Docker is running before.

Make sure to download the needed Docker images:

```
docker pull confluentinc/cp-zookeeper:latest
docker pull confluentinc/cp-kafka:latest
docker pull tensorflow/serving:latest
``` 

#### Experiments

The main entry-point for the experiments is the `run.sh` script which can be found inside the `experiments-driver`
directory.

`run.sh` has the following options:

```
Arguments:
                                       [!] NOTE: Configs in 'experiments-driver/configs/exp-configs/[-ec]/[-e]' will be run.
-e     | --experiments                 Independent variable sets to run: a=all, i=input rate, b=batch size, s=scalability, 
                                       r=bursty rate.
-ec    | --experiments-control         Controlled variable sets to run: s=small, l=large, d=debug.
                                         - small: Run input rate 256 for the scalability experiment.
                                           [!] NOTE: ResNet50 is recommended for this option due to the large
                                                     model size and limited memory.
                                         - large: Run input rate 30000 for the scalability experiment.
                                         - debug: Run simple experiment configs in the debug folder.
-sp    | --stream-processors           Stream processor to test:
                                         a=all, f=Apache Flink, k=Kafka Streams, s=Spark Streaming, r=Ray.
-m     | --models                      Served models: a=all, f=ffnn, r=resnet50, v=vgg19.
-msm   | --embedded-model-servers      Embedded model serving alternative:
                                         x=none, a=all (w/o noop and nd4j), n=nd4j, d=dl4j, o=onnx, t=tf-savedmodel, k=noop.
                                         [!] NOTE: noop will execute input rate and batch size experiments.
-msx   | --external-model-servers      External model serving alternative: x=none, a=all, t=tf-serving, s=torchserve.
-pm    | --parallelism-model           Valid only for Flink. Parallelism model alternative d=data parallel, t=task parallel."
-em    | --execution-mode              Execution mode: l=local, c=cluster.
-d     | --default-configs             Print default configs.
-h     | --help                        Help.
```

Depending on the experiment required to be executed, the script can be used as follows:

```bash
./experiments-driver/run.sh -e i -ec l -sp fk -m f -msm od -msx x -em l
```

In this case, Apache Flink and Kafka Streams will be chosen as stream processor. The FFNN model will be served using Deep
Learning 4j and ONNX.
The test bed will execute the large experiments, with high input rates. No external server will be tested.

If executed in local mode (`-em l`), the `run.sh` script will start all the needed daemon processes for the following
components: the input producer, the data processor, the data component, and the external server.

In cluster mode (`-em c`), the `run.sh` script assumes that Kafka is already running on a cluster remotely, and that the
daemons have been started and have accessible endpoints. The script `experiments-driver/scripts/start-daemon.sh` is used
to start the daemons. The following command start a daemon process that spawns the data processor component:

```bash
./experiments-driver/scripts/start-daemon.sh -p dp -em l
```

`start-daemon.sh` has the following options:

```
Arguments:
-p     | --process                     Process which will be handled by the daemon: dp=data processor, 
                                           kp=kafka producer, es-tor=external server torchserve, 
                                           es-tfs=external server tf-serving, es-rs=external ray serve.
-em    | --execution-mode              Execution mode: l=local, c=cluster.
```

After the set of experiments complete, the resulting files will be written under `results/`. The notebooks
inside `results-analysis` can then be used to plot the throughput and latency measurements.

**NOTE:** A containerized version will soon be available to faciltate deployments on a cluster.

#### Kafka Overhead Experiments

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

#### Development Notes

If the experiments are stopped before they complete, please be aware that multiple background threads might be still 
hanging (e.g., the daemon threads waiting for wakeup signals). In this case, please make sure to kill all of them
manually. You can use the `ps` command to find their PIDs.



## Extending Crayfish

Crayfish provides a set of interfaces that allow developers to extend the benchmarking frameworks with other stream processing systems, model serving tools, and pre-trained models. We showcase examples of how to do so for each type of system.

### New Stream Processors

New stream processors can be added through abstractions similar to adapters. These adapters need to extend the Crayfish interface and provide functionalities for input reading, model scoring, and output writing alongside logic to set the parallelism of the computation. The adapter needs to provide also abstractions for a stream builder, the generic operator type, and the sink operator. When using the adapter, the model type can be chosen from the list of models supported out of the box or can be a custom model defined by the Crayfish user.

The adapter should extend the ```Crayfish``` abstract class, as below. Next, implement the required methods for building your streaming application.

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

The users can use the new adapter, for instance, to serve DeepLearning4J models, for instance, as follows:

```java
Crayfish adapter = new MyCrayfishAdapter<DL4JModel>(DL4JModel.class, config);
adapter.run();
```

The adapters for Apache Flink, Kafka Streams, and Spark Structured Streaming can be found under [crayfish-java/adapters/](https://github.com/soniahorchidan/crayfish23/tree/main/crayfish-java/adapters/src/main/java).

### New Model Serving Tools

Crayfish can be extended to support ML serving tools besides the ones included off-the-shelf. To do so, create a new Java class and extend ```CrayfishModel```. Then, implement the required methods as below.


```java
public class MyModel extends CrayfishModel {

    @Override
    public void loadModel(String modelName, String location) throws Exception {
        // Implement the logic to load the model
        // Use modelName and location to load the model from the specified location
    }

    @Override
    public void build() throws Exception {
        // Implement the logic to build the model
        // This method is called after loading the model
    }

    @Override
    public CrayfishPrediction apply(CrayfishInputData input) throws Exception {
        // Implement the logic to apply the model to the input data
        // Use the input data to make predictions
    }
}
```

The users can use the new model, for instance, in conjunction with Spark Structured Streaming, for instance, as follows:

```java
Crayfish adapter = new SparkSSCrayfishAdapter<MyModel>(MyModel.class, config);
adapter.run();
```


The implementations corresponding to the supported models can be found under [core/src/main/java/datatypes/models](https://github.com/soniahorchidan/crayfish23/tree/main/core/src/main/java/datatypes/models).

Note that there is no distinction between embedded and external tools; external tools need to send requests to the external serving instance to perform the inference. To do so, Crayfish provides the helper class ```InferenceRequest``` to perform HTTP requests to a given input. The class can be extended to customize gRPC requests as well. Examples can be found under [core/src/main/java/request/](https://github.com/soniahorchidan/crayfish23/tree/main/core/src/main/java/request).

### New Pre-trained ML Models

Crayfish supports two models out of the box: FFNN and ResNet50. However, the users can configure the framework to serve other models of choice. To do so, simply create a new directory containing model configurations under [experiments-driver/configs/model-configs](https://github.com/soniahorchidan/crayfish23/tree/main/experiments-driver/configs/model-configs). Assuming the node model is called ```new_model```, create a file ```model-config.yaml``` inside ```experiments-driver/configs/model-configs/new_model``` and provide the following information about the model:

```
model.name: new_model
input.shape: 1, 1   # the input shape of new_model
input.name: input_1  # the name of the input for the model

model.path.dl4j: path/to/dl4j/new_model
model.path.onnx: path/to/onnx/new_model
model.path.tf-savedmodel: path/to/tf-savedmodel/new_model
model.path.torch_jit: path/to/torch/new_model
model.path.torchserve: torchserve:endpoint/newmodel
model.path.tf-serving: tfserving:endpoint/newmodel
```

These configurations will be passed to the adapter corresponding to the chosen stream processor.
