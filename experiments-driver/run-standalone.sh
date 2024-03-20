#!/bin/bash

function getProperty {
  PROP_FILE=$1
  PROP_KEY=$2
  PROP_VALUE=$(cat $PROP_FILE | grep "$PROP_KEY" | cut -d'=' -f2)
  echo $PROP_VALUE
}

# cluster mode is default
EXECUTION_MODE='c'
# debug mode false by default
RUN_DEBUG_CONF_OPT='false'

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
  -em | --execution-mode)
    EXECUTION_MODE="$2"
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
  -* | --*)
    echo "Unknown option $1"
    exit 1
    ;;
  esac
done

if [[ $EXECUTION_MODE == 'l' ]]; then
  GLOBAL_CONFIGS='./experiments-driver/configs/global-configs-local.properties'
elif [[ $EXECUTION_MODE == 'c' ]]; then
  GLOBAL_CONFIGS='./experiments-driver/configs/global-configs-cluster.properties'
else
  echo "ERROR: Unknown execution mode. Exiting"
  exit 1
fi

echo "Standalone experiment starting..."
FLINK_PATH=$(getProperty $GLOBAL_CONFIGS "flink.bin.path")
MODEL_CONFIG='./experiments-driver/configs/model-configs/ffnn/model-config.yaml'

if [[ $EXPERIMENTS_CTRL_OPT == 'l' ]]; then
  EXPERIMENTS_CONFIGS_LOCATION="./experiments-driver/configs/exp-configs/large"
elif [[ $EXPERIMENTS_CTRL_OPT == 's' ]]; then
  EXPERIMENTS_CONFIGS_LOCATION="./experiments-driver/configs/exp-configs/small"
elif [[ $EXPERIMENTS_CTRL_OPT == 'd' ]]; then
  EXPERIMENTS_CONFIGS_LOCATION="./experiments-driver/configs/exp-configs/debug"
fi

for EXP_CONFIG in $(find $EXPERIMENTS_CONFIGS_LOCATION -name '*.properties' ! -name 'config.properties'); do
  COMMON_EXP_CONFIG="$(dirname "$EXP_CONFIG")/config.properties"
  ER=$(getProperty $COMMON_EXP_CONFIG "experiment.runs")
  REPETITIONS=($(echo "$ER" | tr ',' '\n'))
  ER=$(getProperty $COMMON_EXP_CONFIG "experiment.records")
  EXP_RECORDS_NUM=($(echo "$ER" | tr ',' '\n'))
  ER=$(getProperty $COMMON_EXP_CONFIG "max.input.req.per.worker")
  MAX_INPUT_RATE_PER_PRODUCER=($(echo "$ER" | tr ',' '\n'))
  for EXP_REP in $(eval echo {1..$REPETITIONS}); do
    echo "--- [EXP #$EXP_REP] EXPERIMENT CONFIG FILE: $EXP_CONFIG"
    $FLINK_PATH/bin/start-cluster.sh
    $FLINK_PATH/bin/flink run -c StandaloneDriver ./experiments-driver/target/experiments-driver-1.0-SNAPSHOT.jar \
      -gconf $GLOBAL_CONFIGS -mconf $MODEL_CONFIG -econf $EXP_CONFIG -er $EXP_RECORDS_NUM -mir $MAX_INPUT_RATE_PER_PRODUCER
    "$FLINK_PATH"/bin/stop-cluster.sh
    sleep 10
  done
done

$FLINK_PATH/bin/stop-cluster.sh
echo "Standalone experiment ended!"
