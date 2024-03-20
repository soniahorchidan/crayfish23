#!/bin/bash

# NOTE: TO BE USED ONLY FOR THE DATA PROCESSOR AND KAFKA PRODUCER

function getProperty {
  PROP_FILE=$1
  PROP_KEY=$2
  PROP_VALUE=$(cat $PROP_FILE | grep "$PROP_KEY" | cut -d'=' -f2)
  echo $PROP_VALUE
}

# cluster mode is default
EXECUTION_MODE='l'

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
  # dp=data processor, kp=kafka producer, es-tor=external server torchserve, es-tfs=external server tf-serving, es-rs=external ray serve
  -p | --process)
    PROCESS="$2"
    shift
    shift
    ;;
  -em | --execution-mode)
    EXECUTION_MODE="$2"
    shift
    shift
    ;;
  -* | --*)
    echo "Unknown option $1"
    exit 1
    ;;
  esac
done

if [[ $EXECUTION_MODE == 'l' ]]; then
  GLOBAL_CONFIGS='./experiments-driver/configs/global-configs-local.properties'
  if [[ $STREAM_PROCESSOR == 'ray' ]]; then
    GLOBAL_CONFIGS='../experiments-driver/configs/global-configs-cluster.properties'
  fi
elif [[ $EXECUTION_MODE == 'c' ]]; then
  GLOBAL_CONFIGS='./experiments-driver/configs/global-configs-cluster.properties'
  if [[ $STREAM_PROCESSOR == 'ray' ]]; then
    GLOBAL_CONFIGS='../experiments-driver/configs/global-configs-cluster.properties'
  fi
else
  echo "ERROR: Unknown execution mode. Exiting"
  exit 1
fi

if [[ $PROCESS == 'dp' ]]; then
  MR=$(getProperty $GLOBAL_CONFIGS "data.processing.endpoint")
elif [[ $PROCESS == 'kp' ]]; then
  MR=$(getProperty $GLOBAL_CONFIGS "kafka.input.producer.endpoint")
elif [[ $PROCESS == 'es-tor' ]]; then
  MR=$(getProperty $GLOBAL_CONFIGS "external.serving.endpoint.tor")
elif [[ $PROCESS == 'es-tfs' ]]; then
  MR=$(getProperty $GLOBAL_CONFIGS "external.serving.endpoint.tfs")
elif [[ $PROCESS == 'es-rs' ]]; then
  MR=$(getProperty $GLOBAL_CONFIGS "external.serving.endpoint.rayserve")
fi
ENDPOINT=$(echo "$MR" | tr ',' '\n')
HOST=$(echo "$ENDPOINT" | cut -d ":" -f 1) # TODO: sanity check? (i.e., check if it is the same as in config file)
PORT=$(echo "$ENDPOINT" | cut -d ":" -f 2)

while true; do
  echo "Ready! Waiting for the main server (KC). Listening to port $PORT"
  START=true
  while $START; do
    #    EXP_ARGS=$(python3 ./experiments-driver/tools/netcat_server.py "$PORT")
    EXP_ARGS=$(nc -l "$PORT")
    if [[ "$EXP_ARGS" == *CRAYFISH* ]]; then
      START=false
      pattern="CRAYFISH"
      EXP_ARGS=${EXP_ARGS/$pattern/}
    fi
    sleep 0.1
  done
  echo "Launching daemon...Message received: $EXP_ARGS"

  if [[ $EXP_ARGS == 'EXIT' ]]; then
    echo "Experiments ended; exiting..."
    exit
  fi

  STREAM_PROCESSOR=$(echo $EXP_ARGS | python3 -c "import sys, json; print(json.load(sys.stdin)['sp'])")
  MODEL_FORMAT=$(echo $EXP_ARGS | python3 -c "import sys, json; print(json.load(sys.stdin)['mf'])")
  MODEL_NAME=$(echo $EXP_ARGS | python3 -c "import sys, json; print(json.load(sys.stdin)['mn'])")
  EXP_CONFIGS=$(echo $EXP_ARGS | python3 -c "import sys, json; print(json.load(sys.stdin)['econfig'])")
  MODEL_CONFIG=$(echo $EXP_ARGS | python3 -c "import sys, json; print(json.load(sys.stdin)['mconfig'])")
  EXP_NUM_REC=$(echo $EXP_ARGS | python3 -c "import sys, json; print(json.load(sys.stdin)['er'])")
  MAX_INPUT_RATE_PER_PRODUCER=$(echo $EXP_ARGS | python3 -c "import sys, json; print(json.load(sys.stdin)['mir'])")
  KAFKA_INPUT_TOPIC=$(echo $EXP_ARGS | python3 -c "import sys, json; print(json.load(sys.stdin)['kafka-input-topic'])")
  KAFKA_OUTPUT_TOPIC=$(echo $EXP_ARGS | python3 -c "import sys, json; print(json.load(sys.stdin)['kafka-output-topic'])")
  RUN_TASK_PARALLEL=$(echo $EXP_ARGS | python3 -c "import sys, json; print(json.load(sys.stdin)['task-parallel'])")

  echo "Received $MAX_INPUT_RATE_PER_PRODUCER"

  #  if [[ $EXECUTION_MODE == 'l' ]]; then
  #    sed -i "s/kafka.input.data.topic=.*/kafka.input.data.topic=$KAFKA_INPUT_TOPIC/" ./experiments-driver/configs/global-configs-local.properties
  #    sed -i "s/kafka.output.topic=.*/kafka.output.topic=$KAFKA_OUTPUT_TOPIC/" ./experiments-driver/configs/global-configs-local.properties
  if [[ $EXECUTION_MODE == 'c' ]]; then
    sed -i "s/kafka.input.data.topic=.*/kafka.input.data.topic=$KAFKA_INPUT_TOPIC/" ./experiments-driver/configs/global-configs-cluster.properties
    sed -i "s/kafka.output.topic=.*/kafka.output.topic=$KAFKA_OUTPUT_TOPIC/" ./experiments-driver/configs/global-configs-cluster.properties
  fi

  LOGS_DIR="logs/$STREAM_PROCESSOR/$MODEL_FORMAT/$MODEL_NAME/$(date +%s)"
  mkdir -p $LOGS_DIR

  if [[ $PROCESS == 'dp' ]]; then
    echo "Started data processor $STREAM_PROCESSOR process!"
    if [[ $STREAM_PROCESSOR == 'flink' ]]; then
      FLINK_PATH=$(getProperty $GLOBAL_CONFIGS "flink.bin.path")
      $FLINK_PATH/bin/start-cluster.sh
      nohup $FLINK_PATH/bin/flink run -c ExperimentDriver ./experiments-driver/target/experiments-driver-1.0-SNAPSHOT.jar \
        -sp $STREAM_PROCESSOR -mf $MODEL_FORMAT -mconf $MODEL_CONFIG -gconf $GLOBAL_CONFIGS -econf $EXP_CONFIGS -task-par $RUN_TASK_PARALLEL >$LOGS_DIR/stream-processor.logs &
    elif [[ $STREAM_PROCESSOR == 'kafkastreams' ]]; then
      nohup mvn exec:java -pl experiments-driver -Dexec.mainClass="ExperimentDriver" -Drun.jvmArguments="-Xmx120g" -Dexec.cleanupDaemonThreads=false \
        -Dexec.args="-sp $STREAM_PROCESSOR -mf $MODEL_FORMAT -mconf $MODEL_CONFIG -gconf $GLOBAL_CONFIGS -econf $EXP_CONFIGS" >$LOGS_DIR/stream-processor.logs &
    elif [[ $STREAM_PROCESSOR == 'sparkss' ]]; then
      SPARK_PATH=$(getProperty $GLOBAL_CONFIGS "spark.bin.path")
      nohup $SPARK_PATH/bin/spark-submit --class ExperimentDriver --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.3.2 ./experiments-driver/target/experiments-driver-1.0-SNAPSHOT.jar \
        -sp $STREAM_PROCESSOR -mf $MODEL_FORMAT -mconf $MODEL_CONFIG -gconf $GLOBAL_CONFIGS -econf $EXP_CONFIGS >$LOGS_DIR/stream-processor.logs &
    elif [[ $STREAM_PROCESSOR == 'ray' ]]; then
      echo "Started Ray Server!"
      nohup ./experiments-driver/scripts/start-ray.sh
      echo $GLOBAL_CONFIGS
      nohup python3 ./rayfish/main.py -mf $MODEL_FORMAT -mn $MODEL_NAME -mconf $MODEL_CONFIG -gconf $GLOBAL_CONFIGS -econf $EXP_CONFIGS >$LOGS_DIR/stream-processor.logs &
    fi

  elif [[ $PROCESS == 'kp' ]]; then
    echo "Started Kafka Producer process!"
    nohup mvn exec:java -pl input-producer -Dexec.mainClass="KafkaInputProducersRunner" -Dexec.cleanupDaemonThreads=false \
      -Dexec.args="-mconf $MODEL_CONFIG -gconf $GLOBAL_CONFIGS -econf $EXP_CONFIGS -er $EXP_NUM_REC -mir $MAX_INPUT_RATE_PER_PRODUCER" >$LOGS_DIR/kafka-data-in.logs &

  elif [[ $PROCESS == 'es-tor' ]]; then
    echo "Started TorchServe External Server!"
    nohup ./experiments-driver/scripts/start-torchserve.sh -mn $MODEL_NAME -econf $EXP_CONFIGS >$LOGS_DIR/externalserver-torchserve.logs &

  elif [[ $PROCESS == 'es-tfs' ]]; then
    echo "Started TorchServe External Server!"
    nohup ./experiments-driver/scripts/start-tf-serving.sh -mn $MODEL_NAME -econf $EXP_CONFIGS >$LOGS_DIR/externalserver-tf-serving.logs &

  elif [[ $PROCESS == 'es-rs' ]]; then
    echo "Started RayServe External Server!"
    nohup ./experiments-driver/scripts/start-rayserve.sh -mf $MODEL_FORMAT -econf $EXP_CONFIGS -mconf $MODEL_CONFIG >$LOGS_DIR/externalserver-rayserve.logs &
  fi

  echo "Waiting for exit signal from the main server (KC). Listening to port $PORT"

  START=true
  while $START; do
    #    EXP_ARGS=$(python3 ./experiments-driver/tools/netcat_server.py "$PORT")
    EXP_ARGS=$(nc -l $PORT)
    if [[ "$EXP_ARGS" == *CRAYFISH* ]]; then
      START=false
      pattern="CRAYFISH"
      EXP_ARGS=${EXP_ARGS/$pattern/}
    fi
    sleep 0.1
  done

  echo "Cleaning up all the processes spawned."
  # stop the Flink cluster
  if [[ $STREAM_PROCESSOR == 'flink' && $PROCESS == 'dp' ]]; then
    "$FLINK_PATH"/bin/stop-cluster.sh
  fi

  # stop the ray server
  if [[ $STREAM_PROCESSOR == 'ray' && $PROCESS == 'dp' ]]; then
    ./experiments-driver/scripts/stop-ray.sh
  fi

  # Clean up Spark SS
  if [[ $STREAM_PROCESSOR == 'sparkss' && $PROCESS == 'dp' ]]; then
    rm -r /tmp/checkpoint
  fi

  # stop external server, if started
  if [[ $PROCESS == 'es-tor' ]]; then
    ./experiments-driver/scripts/stop-torchserve.sh
  elif [[ $PROCESS == 'es-tfs' ]]; then
    ./experiments-driver/scripts/stop-tf-serving.sh
  elif [[ $PROCESS == 'es-rs' ]]; then
    ./experiments-driver/scripts/stop-rayserve.sh
  fi

  # kill all other processes
  pkill -P $$

done
