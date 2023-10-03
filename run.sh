#!/bin/bash

log() { echo "$(date +%y/%m/%d_%H:%M:%S):: $*"; }

function getProperty {
  PROP_FILE=$1
  PROP_KEY=$2
  PROP_VALUE=$(cat "$PROP_FILE" | grep "$PROP_KEY" | cut -d'=' -f2)
  echo "$PROP_VALUE"
}

help() {
  echo
  echo "[CRAYFISH BENCHMARK]"
  echo
  echo "Arguments:"
  echo "                                       [!] NOTE: Configs in 'experiments-driver/configs/exp-configs/[-ec]/[-e]' will be run."
  echo "-e     | --experiments                 Independent variable sets to run: a=all, i=input rate, b=batch size, s=scalability."
  echo "-ec    | --experiments-control         Controlled variable sets to run: s=small, l=large, d=debug."
  echo "                                         - small: Run input rate 256 for the scalability experiment." 
  echo "                                           [!] NOTE: ResNet50 is recommended for this option due to the large"
  echo "                                                     model size and limited memory."
  echo "                                         - large: Run input rate 30000 for the scalability experiment." 
  echo "                                         - debug: Run simple experiment configs in the debug folder." 
  echo "-sp    | --stream-processors           Stream processor to test:"
  echo "                                         a=all, f=Apache Flink, k=Kafka Streams, s=Spark Streaming, r=Ray."
  echo "-m     | --models                      Served models: a=all, f=ffnn, r=resnet50."
  echo "-msm   | --embedded-model-servers      Embedded model serving alternative:"
  echo "                                         x=none, a=all (w/o noop and nd4j), n=nd4j, d=dl4j, o=onnx, t=tf-savedmodel, k=noop."
  echo "                                         [!] NOTE: noop will execute input rate and batch size experiments."
  echo "-msx   | --external-model-servers      External model serving alternative: x=none, a=all, t=tf-serving, s=torchserve."
  echo "-em    | --execution-mode              Execution mode: l=local, c=cluster."
  echo "-d     | --default-configs             Print default configs."
  echo "-h     | --help                        Help."
  echo
}

no_options_help() {
  echo
  echo "[CRAYFISH BENCHMARK]"
  echo
  echo "No options were passed. The script will execute the default configuration:"
  echo " -e a -ec l -sp a -m a -msm a -msx a -em l"
  echo "Check -d for more details."
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
  echo "Default configs list: -e a -ec l -sp a -m a -msm a -msx a -em l".
  echo "Important! If one parameter is set, the default configuration will still be used for all the others. Example: if"
  echo "the script is executed with -msm n, the script will still run all the external server options."
  echo "For more information, please check -h for help."
  echo
}

set_e_default() { export EXPERIMENTS_OPT='ibs'; }
set_ec_default() { export EXPERIMENTS_CTRL_OPT='l'; }
set_sp_default() { export STREAM_PROCESSOR_OPT='fksr'; }
set_m_default() { export MODELS_OPT='fr'; }
set_msm_default() { export EMBEDDED_MODEL_SERVERS_OPT='otd'; }
set_msx_default() { export EXTERNAL_MODEL_SERVERS_OPT='st'; }
set_em_default() { export EXECUTION_MODE_OPT='l'; }

set_defaults() {
  set_e_default
  set_ec_default
  set_sp_default
  set_m_default
  set_msm_default
  set_msx_default
  set_em_default
}

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
      case $2 in
        a | i | b | s )
          EXPERIMENTS_OPT="$2"
          if [[ $EXPERIMENTS_OPT == 'a' ]]; then set_e_default; fi
          shift
          shift
          ;;
        *)
          echo "ERROR: Invalid arg $2 for $1. Run the script with -h for help." >&2
          exit 1
      esac
      ;;
    -ec | --experiments-control)
      case $2 in
        s | l | d )
          EXPERIMENTS_CTRL_OPT="$2"
          shift
          shift
          ;;
        *)
          echo "ERROR: Invalid arg $2 for $1. Run the script with -h for help." >&2
          exit 1
      esac
      ;;
    -sp | --stream-processors)
      case $2 in
        a | f | k | s | r )
          STREAM_PROCESSOR_OPT="$2"
          if [[ $STREAM_PROCESSOR_OPT == 'a' ]]; then set_sp_default; fi
          shift
          shift
          ;;
        *)
          echo "ERROR: Invalid arg $2 for $1. Run the script with -h for help." >&2
          exit 1
      esac
      ;;
    -m | --models)
      case $2 in
        a | f | r | v )
          MODELS_OPT="$2"
          if [[ $MODELS_OPT == 'a' ]]; then set_m_default; fi
          shift
          shift
          ;;
        *)
          echo "ERROR: Invalid arg $2 for $1. Run the script with -h for help." >&2
          exit 1
      esac
      ;;
    -msm | --embedded-model-servers)
      case $2 in
        x | a | n | d | o | t | k )
          EMBEDDED_MODEL_SERVERS_OPT="$2"
          if [[ $EMBEDDED_MODEL_SERVERS_OPT == 'a' ]]; then set_msm_default; fi
          shift
          shift
          ;;
        *)
          echo "ERROR: Invalid arg $2 for $1. Run the script with -h for help." >&2
          exit 1
      esac
      ;;
    -msx | --external-model-servers)
      case $2 in
        x | a | t | s )
          EXTERNAL_MODEL_SERVERS_OPT="$2"
          if [[ $EXTERNAL_MODEL_SERVERS_OPT == 'a' ]]; then set_msx_default; fi
          shift
          shift
          ;;
        *)
          echo "ERROR: Invalid arg $2 for $1. Run the script with -h for help." >&2
          exit 1
      esac
      ;;
    -em | --execution-mode)
      case $2 in
        l | c )
          EXECUTION_MODE_OPT="$2"
          shift
          shift
          ;;
        *)
          echo "ERROR: Invalid arg $2 for $1. Run the script with -h for help." >&2
          exit 1
      esac
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

# Run from the project root
cd $(dirname $(echo $0))

# Read listening ports for input-producer, data-processor, and external servers
if [[ $EXECUTION_MODE_OPT == 'l' ]]; then
  GLOBAL_CONFIG='./experiments-driver/configs/global-configs-local.properties'
elif [[ $EXECUTION_MODE_OPT == 'c' ]]; then
  GLOBAL_CONFIG='./experiments-driver/configs/global-configs-cluster.properties'
fi

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

# Start the experiment
log "Crayfish experiments starting"
if [[ $EXECUTION_MODE_OPT == 'l' ]]; then
  log "Experiments running in local execution mode"

  # Open folders which will be later mounted on the containers
  mkdir -p "logs"
  mkdir -p "results"
  
  # A unique ID for all experiments in this run
  EXP_UNIQUE_ID=$(date +%s)

  # Start containers
  log "Starting Kafka cluster ..."
  docker compose up -d zookeeper
  docker compose up -d kafka

  log "Starting input-producer and data-processor, who wait for wakeup signals from output-consumer ... "
  docker compose run -d --service-ports --use-aliases -u $(id -u):$(id -g) input-producer \
    /bin/bash -c "./experiments-driver/scripts/start-daemon.sh -p kp -em 'l' --port $PORT_KP >logs/$EXP_UNIQUE_ID-kp-daemon-logs.txt 2>&1" 
  docker compose run -d --service-ports --use-aliases -u $(id -u):$(id -g) data-processor \
    /bin/bash -c "./experiments-driver/scripts/start-daemon.sh -p dp -em 'l' --port $PORT_DP >logs/$EXP_UNIQUE_ID-dpd-daemon-logs.txt 2>&1"
  if [[ ! $EXTERNAL_MODEL_SERVERS_OPT == 'x' ]]; then
    log "Starting external-serving, who wait for wakeup signals from output-consumer ... "
    docker run -d --rm \
      --name external-serving-agent \
      -u $(id -u):$(id -g) \
      --group-add $(getent group docker | cut -d ':' -f 3) \
      -p 8004:8004 \
      --network="crayfish-dev_internal" \
      --net-alias="external-serving-agent" \
      -v /var/run/docker.sock:/var/run/docker.sock \
      -v ./logs:/home/user/crayfish-dev/logs \
      crayfish-external-serving-agent \
      /bin/bash -c "./experiments-driver/scripts/start-daemon.sh -p es -em 'l' --port $PORT_ES >logs/$EXP_UNIQUE_ID-es-daemon-logs.txt 2>&1"
  fi
  
  log "Starting output-consumer ..."
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

  log "All experiments finished!"

  # Clean up docker containers and volumn
  ./docker/docker-stop.sh
elif [[ $EXECUTION_MODE_OPT == 'c' ]]; then
  log "Experiments running in cluster execution mode. Starting Kafka inside a Docker container"
fi
