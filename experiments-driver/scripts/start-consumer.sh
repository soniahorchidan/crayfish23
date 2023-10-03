#!/bin/bash

log() { echo "$(date +%y/%m/%d_%H:%M:%S):: $*"; }

function getProperty {
  PROP_FILE=$1
  PROP_KEY=$2
  PROP_VALUE=$(cat "$PROP_FILE" | grep "$PROP_KEY" | cut -d'=' -f2)
  echo "$PROP_VALUE"
}

sendSignal() {
  HOST=$1
  PORT=$2
  MSG=$3
  echo $MSG | nc $HOST $PORT -q 0
}

# Read command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
  -e | --experiments)
    EXPERIMENTS_OPT="$2"
    shift
    shift
    ;;
  -ec | --experiments-control)
    EXPERIMENTS_CTRL_OPT="$2"
    shift
    shift
    ;;
  -sp | --stream-processors)
    STREAM_PROCESSOR_OPT="$2"
    shift
    shift
    ;;
  -m | --models)
    MODELS_OPT="$2"
    shift
    shift
    ;;
  -msm | --embedded-model-servers)
    EMBEDDED_MODEL_SERVERS_OPT="$2"
    shift
    shift
    ;;
  -msx | --external-model-servers)
    EXTERNAL_MODEL_SERVERS_OPT="$2"
    shift
    shift
    ;;
  -em | --execution-mode)
    EXECUTION_MODE_OPT="$2"
    shift
    shift
    ;;
  -eid | --exp-unique-id)
    EXP_UNIQUE_ID="$2"
    shift
    shift
    ;;
  esac
done

# NOTE: global-config and exp-config will only be read by the consumer.
#       The params in configs will then be sent to other containers by EXP_FINGERPRINT

# Decide file path for configs
SUPPORTED_TOOLS_FILE="$CRAYFISH_HOME/experiments-driver/configs/supported-tools.properties"

if [[ $EXPERIMENTS_CTRL_OPT == 'l' ]]; then
  EXPERIMENTS_CONFIGS_LOCATION="$CRAYFISH_HOME/experiments-driver/configs/exp-configs/large"
elif [[ $EXPERIMENTS_CTRL_OPT == 's' ]]; then
  EXPERIMENTS_CONFIGS_LOCATION="$CRAYFISH_HOME/experiments-driver/configs/exp-configs/small"
elif [[ $EXPERIMENTS_CTRL_OPT == 'd' ]]; then
  EXPERIMENTS_CONFIGS_LOCATION="$CRAYFISH_HOME/experiments-driver/configs/exp-configs/debug"
fi

if [[ $EXECUTION_MODE_OPT == 'l' ]]; then
  GLOBAL_CONFIG="$CRAYFISH_HOME/experiments-driver/configs/global-configs-local.properties"
elif [[ $EXECUTION_MODE_OPT == 'c' ]]; then
  GLOBAL_CONFIG="$CRAYFISH_HOME/experiments-driver/configs/global-configs-cluster.properties"
fi

# Read stream processors
SP=$(getProperty $SUPPORTED_TOOLS_FILE "stream.processors")
ALL_SP=($(echo "$SP" | tr ',' '\n'))

# Read model formats
MF=$(getProperty $SUPPORTED_TOOLS_FILE "model.formats")
ALL_MSE=($(echo "$MF" | tr ',' '\n'))

# Read list of model formats that provide external serving
ES=$(getProperty $SUPPORTED_TOOLS_FILE "external.servers")
ALL_MSX=($(echo "$ES" | tr ',' '\n'))

# Read model names
MN=$(getProperty $SUPPORTED_TOOLS_FILE "model.names")
ALL_MN=($(echo "$MN" | tr ',' '\n'))

# Read kafka input producer machine endpoint
KP=$(getProperty $GLOBAL_CONFIG "kafka.input.producer.endpoint")
HOST_KP=$(echo $KP | cut -d ":" -f 1)
PORT_KP=$(echo $KP | cut -d ":" -f 2)

# Read data producer machine endpoint
DP=$(getProperty $GLOBAL_CONFIG "data.processing.endpoint")
HOST_DP=$(echo $DP | cut -d ":" -f 1)
PORT_DP=$(echo $DP | cut -d ":" -f 2)

# Read external server machine endpoints
ES=$(getProperty $GLOBAL_CONFIG "external.serving.endpoint")
HOST_ES=$(echo $ES | cut -d ":" -f 1)
PORT_ES=$(echo $ES | cut -d ":" -f 2)

log "################### Starting Experiments ###################"
for SP in "${ALL_SP[@]}"; do
  ##### execute only stream processors specified by parameter
  if [[ ! ($STREAM_PROCESSOR_OPT == *"f"*) && $SP == "flink" ]]; then continue; fi
  if [[ ! ($STREAM_PROCESSOR_OPT == *"k"*) && $SP == "kafkastreams" ]]; then continue; fi
  if [[ ! ($STREAM_PROCESSOR_OPT == *"s"*) && $SP == "sparkss" ]]; then continue; fi
  if [[ ! ($STREAM_PROCESSOR_OPT == *"r"*) && $SP == "ray" ]]; then continue; fi
  log
  log "STREAM PROCESSOR: $SP"
  for MN in "${ALL_MN[@]}"; do
    ##### execute only models specified by parameter
    if [[ ! ($MODELS_OPT == *"f"*) && $MN == "ffnn" ]]; then continue; fi
    if [[ ! ($MODELS_OPT == *"v"*) && $MN == "vgg19" ]]; then continue; fi
    if [[ ! ($MODELS_OPT == *"r"*) && $MN == "resnet50" ]]; then continue; fi
    log "- MODEL NAME: $MN"
    MODEL_CONFIG="$CRAYFISH_HOME/experiments-driver/configs/model-configs/$MN/model-config.yaml"
    for MF in "${ALL_MSE[@]}"; do
      # execute only model formats specified by parameter
      if [[ ! ($EMBEDDED_MODEL_SERVERS_OPT == *"k"*) && $MF == "no-op" ]]; then continue; fi
      if [[ ! ($EMBEDDED_MODEL_SERVERS_OPT == *"o"*) && $MF == "onnx" ]]; then continue; fi
      if [[ ! ($EMBEDDED_MODEL_SERVERS_OPT == *"t"*) && $MF == "tf-savedmodel" ]]; then continue; fi
      if [[ ! ($EMBEDDED_MODEL_SERVERS_OPT == *"n"*) && $MF == "nd4j" ]]; then continue; fi
      if [[ ! ($EMBEDDED_MODEL_SERVERS_OPT == *"d"*) && $MF == "dl4j" ]]; then continue; fi

      if [[ ! ($EXTERNAL_MODEL_SERVERS_OPT == *"t"*) && $MF == "tf-serving" ]]; then continue; fi
      if [[ ! ($EXTERNAL_MODEL_SERVERS_OPT == *"s"*) && $MF == "torchserve" ]]; then continue; fi

      [[ " ${ALL_MSX[*]} " =~ ${MF} ]] && IS_EMBEDDED=false || IS_EMBEDDED=true
      # TODO: move this to Experiment Driver logic or to some config files with possible experiment combinations
      # nd4j format not supported for the large models
      if [[ $MF == "nd4j" && ($MN == "resnet50" || $MN == "vgg19") ]]; then continue; fi

      log "-- MODEL FORMAT = $MF"
      for EXP_CONFIG in $(find $EXPERIMENTS_CONFIGS_LOCATION -name '*.properties' ! -name 'config.properties'); do
        if [[ ! ($EXPERIMENTS_OPT == *"i"*) && $EXP_CONFIG == *input-rate* ]]; then continue; fi
        if [[ ! ($EXPERIMENTS_OPT == *"b"*) && $EXP_CONFIG == *batch-size* ]]; then continue; fi
        if [[ ! ($EXPERIMENTS_OPT == *"s"*) && $EXP_CONFIG == *scalability* ]]; then continue; fi

        # if no-op experiment, execute only input-rate and batch-size
        if [[ $EMBEDDED_MODEL_SERVERS_OPT == *"k"* && $EXP_CONFIG == *scalability* ]]; then continue; fi

        COMMON_EXP_CONFIG="$(dirname "$EXP_CONFIG")/config.properties"
        REPETITIONS=$(getProperty $COMMON_EXP_CONFIG "experiment.runs")
        EXP_RECORDS_NUM=$(getProperty $COMMON_EXP_CONFIG "experiment.records")

        for EXP_REP in $(eval echo {1..$REPETITIONS}); do
          log "--- [EXP #$EXP_REP] EXPERIMENT CONFIG FILE: $EXP_CONFIG"

          LOGS_DIR="logs/$SP/$MF/$MN/$EXP_UNIQUE_ID-$EXP_REP"
          mkdir -p "$LOGS_DIR"

          $CRAYFISH_HOME/experiments-driver/scripts/create-topics.sh -gconfig $GLOBAL_CONFIG -econfig $EXP_CONFIG -em $EXECUTION_MODE_OPT
          sleep 10

          # Expand exp config
          EXP_BATCH_SIZE=$(getProperty $EXP_CONFIG "batch_size")
          EXP_MODEL_REPLICAS=$(getProperty $EXP_CONFIG "model_replicas")
          EXP_INPUT_RATE=$(getProperty $EXP_CONFIG "input_rate")
          EXP_ASYNC_OP_CAPACITY=$(getProperty $EXP_CONFIG "async_op_capacity")

          EXP_EXEC_ARGS="-bs       $EXP_BATCH_SIZE \
                         -mr       $EXP_MODEL_REPLICAS \
                         -ir       $EXP_INPUT_RATE \
                         -ac       $EXP_ASYNC_OP_CAPACITY \
                         -rcd      $EXP_RECORDS_NUM"

          # Expand global config (GLO prefix indicates that the value is consistent through experiments)
          GLO_OUT_DIR=$(getProperty $GLOBAL_CONFIG "out.dir")
          GLO_MAX_INPUT_REQ_PER_WORKER=$(getProperty $GLOBAL_CONFIG "max.input.req.per.worker")
          GLO_KAFKA_MAX_REQ_SIZE=$(getProperty $GLOBAL_CONFIG "kafka.max.req.size")
          GLO_KAFKA_INPUT_MODELS_TOPIC=$(getProperty $GLOBAL_CONFIG "kafka.input.models.topic")
          GLO_KAFKA_INPUT_MODELS_TIMEOUT=$(getProperty $GLOBAL_CONFIG "kafka.input.models.timeout")
          GLO_KAFKA_INPUT_DATA_PARTITIONS_NUM=$(getProperty $GLOBAL_CONFIG "kafka.input.data.partitions.num")
          GLO_KAFKA_OUTPUT_DATA_PARTITIONS_NUM=$(getProperty $GLOBAL_CONFIG "kafka.output.data.partitions.num")
          GLO_KAFKA_CLIENT_ENDPOINT=$(getProperty $GLOBAL_CONFIG "kafka.client.endpoint")
          GLO_KAFKA_AUTH_USER=$(getProperty $GLOBAL_CONFIG "kafka.auth.user")
          GLO_KAFKA_AUTH_PASS=$(getProperty $GLOBAL_CONFIG "kafka.auth.pass")

          GLO_EXEC_ARGS="-dir      $GLO_OUT_DIR \
                         -req      $GLO_MAX_INPUT_REQ_PER_WORKER\
                         -rs       $GLO_KAFKA_MAX_REQ_SIZE \
                         -mtopic   $GLO_KAFKA_INPUT_MODELS_TOPIC \
                         -mtimeout $GLO_KAFKA_INPUT_MODELS_TIMEOUT \
                         -ip       $GLO_KAFKA_INPUT_DATA_PARTITIONS_NUM \
                         -op       $GLO_KAFKA_OUTPUT_DATA_PARTITIONS_NUM \
                         -ce       $GLO_KAFKA_CLIENT_ENDPOINT"
          
          if [[ ! -z $GLO_KAFKA_AUTH_USER && ! -z $GLO_KAFKA_AUTH_PASS ]]; then
            GLO_EXEC_ARGS="$GLO_EXEC_ARGS \
                           -user     $GLO_KAFKA_AUTH_USER \
                           -pswd     $GLO_KAFKA_AUTH_PASS"
          fi
          
          # belong to global config but change every experiment
          KAFKA_INPUT_TOPIC=$(getProperty $GLOBAL_CONFIG "kafka.input.data.topic")
          KAFKA_OUTPUT_TOPIC=$(getProperty $GLOBAL_CONFIG "kafka.output.topic")

          # send wake-up signal to all processes with info about which experiment to start
          EXP_FOOTPRINT="{\"sp\": \"$SP\", \
                          \"mf\": \"$MF\", \
                          \"mn\": \"$MN\", \
                          \"mconf\": \"$MODEL_CONFIG\", \
                          \"kafka-input-topic\": \"$KAFKA_INPUT_TOPIC\", \
                          \"kafka-output-topic\": \"$KAFKA_OUTPUT_TOPIC\", \
                          \"logs-dir\": \"$LOGS_DIR\", \
                          \"exp-batch-size\": \"$EXP_BATCH_SIZE\", \
                          \"exp-model-replicas\": \"$EXP_MODEL_REPLICAS\", \
                          \"exp-exec-args\": \"$EXP_EXEC_ARGS\", \
                          \"glo-exec-args\": \"$GLO_EXEC_ARGS\"}"
                  
          EXEC_ARGS="-sp       $SP \
                     -mf       $MF \
                     -mn       $MN \
                     -mconf    $MODEL_CONFIG \
                     -in       $KAFKA_INPUT_TOPIC \
                     -out      $KAFKA_OUTPUT_TOPIC \
                     $EXP_EXEC_ARGS \
                     $GLO_EXEC_ARGS"

          # Start Kafka Consumer
          nohup mvn exec:java -pl output-consumer -Dexec.mainClass="KafkaConsumer" -Dexec.cleanupDaemonThreads=false \
            -Dexec.args="$EXEC_ARGS" >$LOGS_DIR/kafka-out.logs &
          KC_PID=$!
          log "Kafka Consumer started on PID $KC_PID"

          # Important to start the data processor first to ensure we do not lose datapoints
          echo "$EXP_FOOTPRINT" | sendSignal "$HOST_DP" "$PORT_DP" "$EXP_FOOTPRINT"
          log "Sent wakeup signal to the data processor at $HOST_DP:$PORT_DP."
          sleep 30 # Let it have time to start

          # Start external server, if required by the experiment
          if [[ $IS_EMBEDDED == 'false' ]]; then
            sendSignal "$HOST_ES" "$PORT_ES" "$EXP_FOOTPRINT"
            log "Sent wakeup signal to the external server at $HOST_ES:$PORT_ES."
            sleep 30 # Let it have time to start
          fi

          # Start input producer
          sendSignal "$HOST_KP" "$PORT_KP" "$EXP_FOOTPRINT"
          log "Sent wakeup signal to the kafka producer at $HOST_KP:$PORT_KP."

          # wait for kafka consumer to finish
          wait $KC_PID
          log "Experiment finished!"
          sleep 5

          # Send CLEANUP signal to machines to kill processes
          sendSignal "$HOST_KP" "$PORT_KP" "CLEANUP"
          sendSignal "$HOST_DP" "$PORT_DP" "CLEANUP"
          if [[ $IS_EMBEDDED == 'false' ]]; then
            sendSignal "$HOST_ES" "$PORT_ES" "CLEANUP"
          fi
          log "CLEANUP signals sent."
          sleep 5

          # Delete kafka topics
          $CRAYFISH_HOME/experiments-driver/scripts/delete-topics.sh -gconfig $GLOBAL_CONFIG -em $EXECUTION_MODE_OPT
          log "Deleted Kafka Topics"
        done
      done
    done
  done
done

#Send EXIT signal to daemons
log "Experiments ended! Cleaning up."
sendSignal "$HOST_KP" "$PORT_KP" "EXIT"
sendSignal "$HOST_DP" "$PORT_DP" "EXIT"
if [[ ! $EXTERNAL_MODEL_SERVERS_OPT == 'x' ]]; then
  sendSignal "$HOST_ES" "$PORT_ES" "EXIT"
fi

log "Done."
