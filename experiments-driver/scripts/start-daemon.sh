#!/bin/bash

function getValue {
  JSON_STR=$1
  JSON_KEY=$2
  JSON_VALUE=$(echo "$JSON_STR" | python3 -c "import sys, json; print(json.load(sys.stdin)['$JSON_KEY'])")
  echo $JSON_VALUE
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
  # dp=data processor, kp=kafka producer, es-tor=external server torchserve, es-tfs=external server tf-serving
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
  --port)
    PORT="$2"
    shift
    shift
    ;;
  -* | --*)
    echo "Unknown option $1"
    exit 1
    ;;
  esac
done

while true; do
  echo "Ready! Waiting for the main server (KC). Listening to port $PORT"
  while ! EXP_ARGS=$(nc -l $PORT); do
    sleep 0.1
  done
  echo "Launching daemon ..."
  echo "Message received: $EXP_ARGS"

  if [[ $EXP_ARGS == 'EXIT' ]]; then
    echo "Experiments ended; exiting..."
    exit
  fi

  # only used in start-daemon.sh
  LOGS_DIR=$(getValue "$EXP_ARGS" 'logs-dir')
  
  # keep pass to java process
  SP=$(getValue "$EXP_ARGS" 'sp')
  MF=$(getValue "$EXP_ARGS" 'mf')
  MN=$(getValue "$EXP_ARGS" 'mn')
  MCONF=$(getValue "$EXP_ARGS" 'mconf')
  KAFKA_INPUT_TOPIC=$(getValue "$EXP_ARGS" 'kafka-input-topic')
  KAFKA_OUTPUT_TOPIC=$(getValue "$EXP_ARGS" 'kafka-output-topic')
  EXP_BATCH_SIZE=$(getValue "$EXP_ARGS" 'exp-batch-size')
  EXP_MODEL_REPLICAS=$(getValue "$EXP_ARGS" 'exp-model-replicas')
  EXP_EXEC_ARGS=$(getValue "$EXP_ARGS" 'exp-exec-args')
  GLO_EXEC_ARGS=$(getValue "$EXP_ARGS" 'glo-exec-args')

  EXEC_ARGS="-sp       $SP \
             -mf       $MF \
             -mn       $MN \
             -mconf    $MCONF \
             -in       $KAFKA_INPUT_TOPIC \
             -out      $KAFKA_OUTPUT_TOPIC \
             $EXP_EXEC_ARGS \
             $GLO_EXEC_ARGS"
  
  # Start daemon
  mkdir -p "$LOGS_DIR"
  if [[ $PROCESS == 'dp' ]]; then
    echo "Starting data processor $SP process ..."
    if [[ $SP == 'flink' ]]; then
      $FLINK_HOME/bin/start-cluster.sh
      nohup $FLINK_HOME/bin/flink run -c ExperimentDriver \
        ./experiments-driver/target/experiments-driver-1.0-SNAPSHOT.jar $EXEC_ARGS >$LOGS_DIR/stream-processor.logs 2>&1 &
    elif [[ $SP == 'kafkastreams' ]]; then
      nohup mvn exec:java -pl experiments-driver -Dexec.mainClass="ExperimentDriver" -Drun.jvmArguments="-Xmx120g" \
        -Dexec.cleanupDaemonThreads=false -Dexec.args="$EXEC_ARGS" >$LOGS_DIR/stream-processor.logs &
    elif [[ $SP == 'sparkss' ]]; then
      nohup $SPARK_HOME/bin/spark-submit --class ExperimentDriver --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.3.2 \
        ./experiments-driver/target/experiments-driver-1.0-SNAPSHOT.jar $EXEC_ARGS >$LOGS_DIR/stream-processor.logs 2>&1 &
    elif [[ $SP == 'ray' ]]; then
      ray start --head
      nohup python3 ./rayfish/main.py $EXEC_ARGS -intopic $KAFKA_INPUT_TOPIC >$LOGS_DIR/stream-processor.logs 2>&1 &
    fi

  elif [[ $PROCESS == 'kp' ]]; then
    echo "Started kafka producer process!"
    nohup mvn exec:java -pl input-producer -Dexec.mainClass="KafkaInputProducersRunner" -Dexec.cleanupDaemonThreads=false \
      -Dexec.args="$EXEC_ARGS" >$LOGS_DIR/kafka-data-in.logs &

  elif [[ $PROCESS == 'es' ]]; then
    if [[ $MF == 'torchserve' ]]; then
      echo "Starting Torchserve external server ..."
      docker compose run -d --service-ports --use-aliases torch-serving \
        torchserve --start --ncs --model-store model-store
      sleep 10

      echo "Register the $MN torch model"
      curl -v -X POST "http://torch-serving:8081/models?model_name=$MN&url=$MN.mar&initial_workers=1&batch_size=1&max_batch_delay=1000&reponse_timeout=120"
      sleep 5
      echo "Scale the number of workers used for serving to $EXP_MODEL_REPLICAS"
      curl -v -X PUT "http://torch-serving:8081/models/$MN?min_worker=$EXP_MODEL_REPLICAS&max_worker=$EXP_MODEL_REPLICAS"
      sleep 2

      # Check model serving configuration
      echo "Updated model serving configuration:"
      curl "torch-serving:8081/models/$MN"

    elif [[ $MF == 'tf-serving' ]]; then
      if [[ $SP == 'ray' ]]; then
        echo "Starting Ray serving external server ..."
        RAY_CONTAINER_ID=$(docker compose run -d --service-ports --use-aliases ray-serving tail -f /dev/null)
        docker exec $RAY_CONTAINER_ID /bin/bash -c \
          "ray start --head"
        docker exec $RAY_CONTAINER_ID /bin/bash -c \
          "python3 ./rayfish/deploy_rayserve.py $EXEC_ARGS"

      else
        MODEL_DIR="$CRAYFISH_HOME/resources/external/tf-s/$MN/models"

        # Setting batching.conf
        echo "Scaling the number of parallel threads used for serving to $EXP_MODEL_REPLICAS ..."
        sed -i "s/num_batch_threads { value: .* }/num_batch_threads { value: $EXP_MODEL_REPLICAS }/g" $MODEL_DIR/batching.config
        echo "Setting batch size $EXP_BATCH_SIZE ..."
        sed -i "s/max_batch_size { value: .* }/max_batch_size { value: $EXP_BATCH_SIZE }/g" $MODEL_DIR/batching.config
        echo ""

        # Start Tensorflow Model Serving
        CONTAINER_MODEL_DIR="/models/$MN/models"

        echo "Starting Tensorflow serving external server ..."
        docker compose run -d --service-ports --use-aliases tf-serving \
          --model_config_file=$CONTAINER_MODEL_DIR/models.config \
          --model_config_file_poll_wait_seconds=60 \
          --tensorflow_intra_op_parallelism=1 \
          --tensorflow_inter_op_parallelism=1 \
          --enable_batching=true \
          --batching_parameters_file=$CONTAINER_MODEL_DIR/batching.config
      fi
    fi
  fi

  echo "Waiting for exit signal from the main server (KC). Listening to port $PORT ..."
  while ! nc -l $PORT; do
    sleep 0.1
  done

  echo "Cleaning up all the processes spawned ..."
  # stop flink
  if [[ $SP == 'flink' && $PROCESS == 'dp' ]]; then
    $FLINK_HOME/bin/stop-cluster.sh
  fi

  # stop ray
  if [[ $SP == 'ray' && $PROCESS == 'dp' ]]; then
    ray stop
  fi

  # clean up sparkss
  if [[ $STREAM_PROCESSOR == 'sparkss' && $PROCESS == 'dp' ]]; then
    rm -r /tmp/checkpoint
  fi

  # kill all other processes
  pkill -P $$

  # stop external server, if started
  if [[ $PROCESS == 'es' ]]; then
    if [[ $MF == 'torchserve' ]]; then
      if [[ $SP == 'ray' ]]; then
        echo "Stopping Rayserve external server ..."
        docker compose down ray-serving 
      else
        echo "Stopping Torchserve external server ..."
        docker compose down torch-serving
      fi
    elif [[ $MF == 'tf-serving' ]]; then
      echo "Stopping Tensorflow external server ..."
      docker compose down tf-serving
    fi
  fi
done
