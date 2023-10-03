#!/bin/bash

log() { echo "$(date +%y/%m/%d_%H:%M:%S):: $*"; }

function getProperty {
  PROP_FILE=$1
  PROP_KEY=$2
  PROP_VALUE=$(cat $PROP_FILE | grep "$PROP_KEY" | cut -d'=' -f2)
  echo $PROP_VALUE
}

# Read command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
  -em | --execution-mode)
    EXECUTION_MODE_OPT="$2"
    shift
    shift
    ;;
  -ec | --experiments-control)
    EXPERIMENTS_CTRL_OPT="$2"
    shift
    shift
    ;;
  esac
done

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

MODEL_CONFIG="$CRAYFISH_HOME/experiments-driver/configs/model-configs/ffnn/model-config.yaml"

log "Standalone experiment starting..."
for EXP_CONFIG in $(find $EXPERIMENTS_CONFIGS_LOCATION -name '*.properties' ! -name 'config.properties'); do
  COMMON_EXP_CONFIG="$(dirname "$EXP_CONFIG")/config.properties"
  REPETITIONS=$(getProperty $COMMON_EXP_CONFIG "experiment.runs")
  EXP_RECORDS_NUM=$(getProperty $COMMON_EXP_CONFIG "experiment.records")
  for EXP_REP in $(eval echo {1..$REPETITIONS}); do
    log "--- [EXP #$EXP_REP] EXPERIMENT CONFIG FILE: $EXP_CONFIG"
    $FLINK_HOME/bin/start-cluster.sh
    $FLINK_HOME/bin/flink run -c StandaloneDriver ./experiments-driver/target/experiments-driver-1.0-SNAPSHOT.jar \
      -gconf $GLOBAL_CONFIG -mconf $MODEL_CONFIG -econf $EXP_CONFIG -er $EXP_RECORDS_NUM
    $FLINK_HOME/bin/stop-cluster.sh
    sleep 10
  done
done

log "Standalone experiment ended!"