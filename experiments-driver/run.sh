#!/bin/bash

log() { echo "$(date +%y/%m/%d_%H:%M:%S):: $*"; }

help() {
  echo
  echo "[CRAYFISH BENCHMARK]"
  echo
  echo "Arguments:"
  echo "                                       [!] NOTE: Configs in 'experiments-driver/configs/exp-configs/[-ec]/[-e]' will be run."
  echo "-e     | --experiments                 Experiment configs to run: a=all, i=input rate, b=batch size, s=scalability, r=bursty rate."
  echo "-ec    | --experiments-control         Controlled variable sets to run: s=small, l=large, d=debug."
  echo "                                         - small: Run input rate 256 for the scalability experiment."
  echo "                                           [!] NOTE: ResNet50 is recommended for this option due to the large"
  echo "                                                     model size and limited memory."
  echo "                                         - large: Run input rate 30000 for the scalability experiment."
  echo "                                         - debug: Run simple experiment configs in the debug folder."
  echo "-sp    | --stream-processors           Stream processor to test: a=all, f=Apache Flink, k=Kafka Streams, s=Spark Streaming, r=Ray."
  echo "-msm   | --embedded-model-servers      Embedded model serving alternative: x=none, a=all (w/o noop and nd4j), n=nd4j, d=dl4j, o=onnx, t=tf-savedmodel, k=noop."
  echo "                                       NOTE: noop will execute input rate and batch size experiments."
  echo "-msx   | --external-model-servers      External model serving alternative: x=none, a=all, t=tf-serving, s=torchserve."
  echo "-m     | --models                      Served models: a=all, f=ffnn, r=resnet50, v=vgg19."
  echo "-pm    | --parallelism-model           Valid only for Flink. Parallelism model alternative d=data parallel, t=task parallel."
  echo "-em    | --execution-mode              Execution mode: l=local, c=cluster."
  echo "-p     | --package                     Run mvn packager: false, true."
  echo "-d     | --default-configs             Print default configs."
  echo "-h     | --help                        Help."
  echo
}

no_options_help() {
  echo
  echo "[CRAYFISH BENCHMARK]"
  echo
  echo "No options were passed. The script will execute the default configuration."
  echo "If this is not the intended behaviour, please check -h for help."
  echo
}

default_configs() {
  echo
  echo "[CRAYFISH BENCHMARK]"
  echo
  echo "The benchmark will run all the configured experiments using all the available tools and combinations."
  echo "All the available models will be tested."
  echo "The benchmark will execute in local mode."
  echo "The experiment to measure the Kafka Overhead (i.e., -msm k) will not be executed."
  echo "Default configs list: -e ibs -ec l -sp a -msm a -msx a -m a -pm d -em l -pm d -p false".
  echo "Important! If one parameter is set, the default configuration will still be used for all the others. Example: if"
  echo "the script is executed with -msm n, the script will still run all the external server options."
  echo "For more information, please check -h for help."
  echo
}

set_e_default() { export EXPERIMENTS_OPT='ibs'; }
set_ec_default() { export EXPERIMENTS_CTRL_OPT='l'; }
set_sp_default() { export STREAM_PROCESSOR_OPT='fksr'; }
set_msm_default() { export EMBEDDED_MODEL_SERVERS_OPT='otd'; }
set_msx_default() { export EXTERNAL_MODEL_SERVERS_OPT='st'; }
set_pm_default() { export PARALLELISM_MODEL_OPT='d'; }
set_em_default() { export EXECUTION_MODE_OPT='l'; }
set_m_default() { export MODELS_OPT='frv'; }
set_mvn_default() { export MVN_PACKAGE_OPT='false'; }

set_defaults() {
  set_e_default
  set_ec_default
  set_sp_default
  set_msm_default
  set_msx_default
  set_pm_default
  set_em_default
  set_mvn_default
  set_m_default
}

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
  NC_VERSION=$4
  echo $MSG | nc $HOST $PORT -q 0
  if [[ "$nc_version" == *"gnu"* ]]; then  # GNU netcat
    echo $MSG | nc $HOST $PORT -c
  else  # Assuming BSD/traditional netcat
    echo $MSG | nc $HOST $PORT -q 0
  fi
}


nc_version=$(nc -h 2>&1 | grep -i "gnu") 
if [[ -z "$nc_version" ]]; then
  echo "netcat not found. Please install it."
  exit 1
fi

# Set configs defaults
set_defaults

# Read command line arguments
if [ $# -eq 0 ]; then
  no_options_help
else
  while [[ $# -gt 0 ]]; do
    case $1 in
    -h | --help)
      help
      shift
      exit 1
      ;;
    -d | --default-configs)
      default_configs
      shift
      ;;
    -e | --experiments)
      EXPERIMENTS_OPT="$2"
      if [[ $EXPERIMENTS_OPT == 'a' ]]; then set_e_default; fi
      shift
      shift
      ;;
    -ec | --experiments-control)
      case $2 in
      s | l | d)
        EXPERIMENTS_CTRL_OPT="$2"
        shift
        shift
        ;;
      *)
        echo "ERROR: Invalid arg $2 for $1. Run the script with -h for help." >&2
        exit 1
        ;;
      esac
      ;;
    -m | --models)
      MODELS_OPT="$2"
      if [[ $MODELS_OPT == 'a' ]]; then set_m_default; fi
      shift
      shift
      ;;
    -sp | --stream-processors)
      STREAM_PROCESSOR_OPT="$2"
      if [[ $STREAM_PROCESSOR_OPT == 'a' ]]; then set_sp_default; fi
      shift
      shift
      ;;
    -msm | --embedded-model-servers)
      EMBEDDED_MODEL_SERVERS_OPT="$2"
      if [[ $EMBEDDED_MODEL_SERVERS_OPT == 'a' ]]; then set_msm_default; fi
      shift
      shift
      ;;
    -msx | --external-model-servers)
      EXTERNAL_MODEL_SERVERS_OPT="$2"
      if [[ $EXTERNAL_MODEL_SERVERS_OPT == 'a' ]]; then set_msx_default; fi
      shift
      shift
      ;;
    -pm | --parallelism-model)
      PARALLELISM_MODEL_OPT="$2"
      shift
      shift
      ;;
    -p | --package)
      MVN_PACKAGE_OPT="$2"
      shift
      shift
      ;;
    -em | --execution-mode)
      EXECUTION_MODE_OPT="$2"
      shift
      shift
      ;;
    \?)
      echo "ERROR: Invalid option $1. Run the script with -h for help." >&2
      exit 1
      ;;
    -* | --*)
      echo "ERROR: Unknown option $1. Run the script with -h for help." >&2
      exit 1
      ;;
    *)
      echo "ERROR: Unknown option $1. Run the script with -h for help." >&2
      exit 1
      ;;
    esac
  done
fi

SUPPORTED_TOOLS_FILE=".//experiments-driver/configs/supported-tools.properties"

if [[ $EXPERIMENTS_CTRL_OPT == 'l' ]]; then
  EXPERIMENTS_CONFIGS_LOCATION="./experiments-driver/configs/exp-configs/large"
elif [[ $EXPERIMENTS_CTRL_OPT == 's' ]]; then
  EXPERIMENTS_CONFIGS_LOCATION="./experiments-driver/configs/exp-configs/small"
elif [[ $EXPERIMENTS_CTRL_OPT == 'd' ]]; then
  EXPERIMENTS_CONFIGS_LOCATION="./experiments-driver/configs/exp-configs/debug"
fi

if [[ $EXECUTION_MODE_OPT == 'l' ]]; then
  GLOBAL_CONFIGS='./experiments-driver/configs/global-configs-local.properties'
elif [[ $EXECUTION_MODE_OPT == 'c' ]]; then
  GLOBAL_CONFIGS='./experiments-driver/configs/global-configs-cluster.properties'
else
  log "ERROR: Unknown option for the execution mode -em. Available options: l=local, c=cluster" >&2
  exit 1
fi

RUN_FLINK_TASK_PARALLEL=false
if [[ $PARALLELISM_MODEL_OPT == 't' ]]; then
  RUN_FLINK_TASK_PARALLEL=true
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
K=$(getProperty $GLOBAL_CONFIGS "kafka.input.producer.endpoint")
KP=($(echo "$K" | tr ',' '\n'))
HOST_KP=$(echo $KP | cut -d ":" -f 1)
PORT_KP=$(echo $KP | cut -d ":" -f 2)

# Read data producer machine endpoint
D=$(getProperty $GLOBAL_CONFIGS "data.processing.endpoint")
DP=($(echo "$D" | tr ',' '\n'))
HOST_DP=$(echo $DP | cut -d ":" -f 1)
PORT_DP=$(echo $DP | cut -d ":" -f 2)

# Read external server machine endpoints
E=$(getProperty $GLOBAL_CONFIGS "external.serving.endpoint.tfs")
ES=($(echo "$E" | tr ',' '\n'))
HOST_ES_TFS=$(echo $ES | cut -d ":" -f 1)
PORT_ES_TFS=$(echo $ES | cut -d ":" -f 2)

E=$(getProperty $GLOBAL_CONFIGS "external.serving.endpoint.tor")
ES=($(echo "$E" | tr ',' '\n'))
HOST_ES_TOR=$(echo $ES | cut -d ":" -f 1)
PORT_ES_TOR=$(echo $ES | cut -d ":" -f 2)

E=$(getProperty $GLOBAL_CONFIGS "external.serving.endpoint.rayserve")
ES=($(echo "$E" | tr ',' '\n'))
HOST_ES_RS=$(echo $ES | cut -d ":" -f 1)
PORT_ES_RS=$(echo $ES | cut -d ":" -f 2)

mkdir -p "logs"
log "Crayfish experiments starting"
# Start Kafka and Daemons locally if in local execution mode
if [[ $EXECUTION_MODE_OPT == 'l' ]]; then
  log "Experiments running in local execution mode. Starting Kafka inside a Docker container"
  docker compose up -d
  sleep 10
  log "Starting daemons locally"
  nohup ./experiments-driver/scripts/start-daemon.sh -p dp -em 'l' >logs/"$(date +%s)-dpd-daemon-logs.txt" &
  nohup ./experiments-driver/scripts/start-daemon.sh -p kp -em 'l' >logs/"$(date +%s)-kp-daemon-logs.txt" &
  if [[ $EXTERNAL_MODEL_SERVERS_OPT == *"t"* ]]; then
    if [[ $STREAM_PROCESSOR_OPT == *"r"* ]]; then
      nohup ./experiments-driver/scripts/start-daemon.sh -p es-rs -em 'l' >logs/"$(date +%s)-es-rs-daemon-logs.txt" &
    else
      nohup ./experiments-driver/scripts/start-daemon.sh -p es-tfs -em 'l' >logs/"$(date +%s)-es-tfserving-daemon-logs.txt" &
    fi
  fi
  if [[ $EXTERNAL_MODEL_SERVERS_OPT == *"s"* ]]; then
    nohup ./experiments-driver/scripts/start-daemon.sh -p es-tor -em 'l' >logs/"$(date +%s)-es-torchserve-daemon-logs.txt" &
  fi
elif [[ $EXECUTION_MODE_OPT == 'c' ]]; then
  log "Experiments running in cluster execution mode. Starting Kafka inside a Docker container"
fi

if [[ $MVN_PACKAGE_OPT == 'true' ]]; then
  log "##################### Maven Packaging ######################"
  mvn clean install
fi

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
    MODEL_CONFIG="./experiments-driver/configs/model-configs/$MN/model-config.yaml"
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
      if [[ $MF == "nd4j" && ($MN == "resnet50") ]]; then continue; fi

      log "-- MODEL FORMAT = $MF"
      for EXP_CONFIG in $(find $EXPERIMENTS_CONFIGS_LOCATION -name '*.properties' ! -name 'config.properties'); do
        if [[ ! ($EXPERIMENTS_OPT == *"i"*) && $EXP_CONFIG == *input-rate* ]]; then continue; fi
        if [[ ! ($EXPERIMENTS_OPT == *"b"*) && $EXP_CONFIG == *batch-size* ]]; then continue; fi
        if [[ ! ($EXPERIMENTS_OPT == *"s"*) && $EXP_CONFIG == *scalability* ]]; then continue; fi
        if [[ ! ($EXPERIMENTS_OPT == *"r"*) && $EXP_CONFIG == *bursty-rate* ]]; then continue; fi

        # if no-op experiment, execute only input-rate and batch-size
        if [[ $EMBEDDED_MODEL_SERVERS_OPT == *"k"* && $EXP_CONFIG == *scalability* ]]; then continue; fi

        COMMON_EXP_CONFIG="$(dirname "$EXP_CONFIG")/config.properties"
        ER=$(getProperty $COMMON_EXP_CONFIG "experiment.runs")
        REPETITIONS=($(echo "$ER" | tr ',' '\n'))
        ER=$(getProperty $COMMON_EXP_CONFIG "experiment.records")
        EXP_RECORDS_NUM=($(echo "$ER" | tr ',' '\n'))
        ER=$(getProperty $COMMON_EXP_CONFIG "max.input.req.per.worker")
        MAX_INPUT_RATE_PER_WORKER=($(echo "$ER" | tr ',' '\n'))

        for EXP_REP in $(eval echo {1..$REPETITIONS}); do

          log "--- [EXP #$EXP_REP] EXPERIMENT CONFIG FILE: $EXP_CONFIG"

          LOGS_DIR="logs/$SP/$MF/$MN/$(date +%s)"
          mkdir -p "$LOGS_DIR"

          ./experiments-driver/scripts/create-topics.sh -gconfig $GLOBAL_CONFIGS -econfig $EXP_CONFIG -em $EXECUTION_MODE_OPT
          log "Created Kafka Topics"
          K=$(getProperty $GLOBAL_CONFIGS "kafka.input.data.topic")
          KAFKA_INPUT_TOPIC=($(echo "$K" | tr ',' '\n'))
          K=$(getProperty $GLOBAL_CONFIGS "kafka.output.topic")
          KAFKA_OUTPUT_TOPIC=($(echo "$K" | tr ',' '\n'))
          sleep 10

          # Start Kafka Consumer
          nohup mvn exec:java -pl output-consumer -Dexec.mainClass="KafkaConsumer" -Dexec.cleanupDaemonThreads=false \
            -Dexec.args="-sp $SP -mf $MF -mconf $MODEL_CONFIG -gconf $GLOBAL_CONFIGS -econf $EXP_CONFIG -er $EXP_RECORDS_NUM -tp $RUN_FLINK_TASK_PARALLEL" >$LOGS_DIR/kafka-out.logs &
          KC_PID=$!
          log "Kafka Consumer Started on PID $KC_PID"
          sleep 10

          # send wake-up signal to all processes with info about which experiment to start
          EXP_FOOTPRINT="CRAYFISH{\"mf\": \"$MF\", \
                         \"sp\": \"$SP\", \
                         \"mn\": \"$MN\", \
                         \"task-parallel\": \"$RUN_FLINK_TASK_PARALLEL\", \
                         \"econfig\": \"$EXP_CONFIG\", \
                         \"mconfig\": \"$MODEL_CONFIG\", \
                         \"er\": \"$EXP_RECORDS_NUM\", \
                         \"mir\": \"$MAX_INPUT_RATE_PER_WORKER\", \
                         \"is-embedded\": \"$IS_EMBEDDED\", \
                         \"kafka-input-topic\": \"$KAFKA_INPUT_TOPIC\", \
                         \"kafka-output-topic\": \"$KAFKA_OUTPUT_TOPIC\"}"

          # Important to start the data processor first to ensure we do not lose datapoints
          echo "$EXP_FOOTPRINT" | sendSignal "$HOST_DP" "$PORT_DP" "$EXP_FOOTPRINT" "$NC_VERSION"
          log "Sent wakeup signal to the data processor at $HOST_DP $PORT_DP."
          sleep 30 # Let it have time to start. Ray is especially slow

          # Start external server, if required by the experiment
          if [[ $IS_EMBEDDED == 'false' ]]; then
            if [[ $MF == 'torchserve' ]]; then
              sendSignal "$HOST_ES_TOR" "$PORT_ES_TOR" "$EXP_FOOTPRINT" "$NC_VERSION"
            elif [[ $MF == 'tf-serving' ]]; then
              if [[ $SP == 'ray' ]]; then
                sendSignal "$HOST_ES_RS" "$PORT_ES_RS" "$EXP_FOOTPRINT" "$NC_VERSION"
              else
                sendSignal "$HOST_ES_TFS" "$PORT_ES_TFS" "$EXP_FOOTPRINT" "$NC_VERSION"
              fi
            fi
            log "Sent signal to the external server."
            sleep 10 # Let it have time to start. Ray is especially slow
          fi

          # Start input producer
          sendSignal "$HOST_KP" "$PORT_KP" "$EXP_FOOTPRINT" "$NC_VERSION"
          log "Sent signal to the kafka producer at $HOST_KP $PORT_KP."

          # wait for kafka consumer to finish
          wait $KC_PID
          log "Experiment finished!"
          sleep 5

          # Send CLEANUP signal to machines to kill processes
          sendSignal "$HOST_KP" "$PORT_KP" "CRAYFISHCLEANUP" "$NC_VERSION"
          sendSignal "$HOST_DP" "$PORT_DP" "CRAYFISHCLEANUP" "$NC_VERSION"
          if [[ $IS_EMBEDDED == 'false' ]]; then
            # TODO: combine to run on one endpoint only
            if [[ $MF == 'torchserve' ]]; then
              sendSignal "$HOST_ES_TOR" "$PORT_ES_TOR" "CRAYFISHCLEANUP" "$NC_VERSION"
            elif [[ $MF == 'tf-serving' ]]; then
              if [[ $SP == 'ray' ]]; then
                sendSignal "$HOST_ES_RS" "$PORT_ES_RS" "CRAYFISHCLEANUP" "$NC_VERSION"
              else
                sendSignal "$HOST_ES_TFS" "$PORT_ES_TFS" "CRAYFISHCLEANUP" "$NC_VERSION"
              fi
            fi
          fi
          log "CLEANUP signals sent."
          sleep 5

          # Delete kafka topics
          ./experiments-driver/scripts/delete-topics.sh -gconfig $GLOBAL_CONFIGS -em $EXECUTION_MODE_OPT
          log "Deleted Kafka Topics"
        done
      done
    done
  done
done

#Send EXIT signal to daemons
log "Experiments ended! Cleaning up."
sendSignal "$HOST_KP" "$PORT_KP" "CRAYFISHEXIT" "$NC_VERSION"
sendSignal "$HOST_DP" "$PORT_DP" "CRAYFISHEXIT" "$NC_VERSION"
sendSignal "$HOST_ES_TOR" "$PORT_ES_TOR" "CRAYFISHEXIT" "$NC_VERSION"
sendSignal "$HOST_ES_TFS" "$PORT_ES_TFS" "CRAYFISHEXIT" "$NC_VERSION"
sendSignal "$HOST_ES_RS" "$PORT_ES_RS" "CRAYFISHEXIT" "$NC_VERSION"

pkill -P $$
log "Done."
