#!/bin/bash

function getProperty {
  PROP_FILE=$1
  PROP_KEY=$2
  PROP_VALUE=$(cat $PROP_FILE | grep "$PROP_KEY" | cut -d'=' -f2)
  echo $PROP_VALUE
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
  -gconfig | --global-config)
    GLOBAL_CONFIGS="$2"
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

IN_TOPIC=$(getProperty $GLOBAL_CONFIGS "kafka.input.root.topic")
OUT_TOPIC=$(getProperty $GLOBAL_CONFIGS "kafka.output.root.topic")
BOOTSTRAP_SERVER=$(getProperty $GLOBAL_CONFIGS "kafka.client.endpoint")

if [[ $EXECUTION_MODE == 'l' ]]; then
  SH_TOPIC="${KAFKA_HOME}/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER"
elif [[ $EXECUTION_MODE == 'c' ]]; then
  KAFKA_CONFIG="${CRAYFISH_HOME}/experiments-driver/scripts/config.properties"
  SH_TOPIC="${KAFKA_HOME}/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER --command-config $KAFKA_CONFIG"
fi

echo "Deleting kafka crayfish topics..."
/bin/bash -c "$SH_TOPIC --delete --topic \"$IN_TOPIC.*\""
/bin/bash -c "$SH_TOPIC --delete --topic \"$OUT_TOPIC.*\""
/bin/bash -c "$SH_TOPIC --delete --topic \"$crayfish.*\""
echo "Done!"