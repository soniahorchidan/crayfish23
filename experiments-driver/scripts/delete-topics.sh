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
MR=$(getProperty $GLOBAL_CONFIGS "kafka.input.root.topic")
IN_TOPIC=$(echo "$MR" | tr ',' '\n')

MR=$(getProperty $GLOBAL_CONFIGS "kafka.output.root.topic")
OUT_TOPIC=$(echo "$MR" | tr ',' '\n')

MR=$(getProperty $GLOBAL_CONFIGS "kafka.bootstrap.servers")
BOOSTRAP_SERVER=$(echo "$MR" | tr ',' '\n')

echo "Deleting Kafka Topics..."
if [[ $EXECUTION_MODE == 'l' ]]; then
  docker exec crayfish-kafka-1 kafka-topics --bootstrap-server $BOOSTRAP_SERVER --delete --topic "$IN_TOPIC.*" --if-exists
  docker exec crayfish-kafka-1 kafka-topics --bootstrap-server $BOOSTRAP_SERVER --delete --topic "$OUT_TOPIC.*" --if-exists
  docker exec crayfish-kafka-1 kafka-topics --bootstrap-server $BOOSTRAP_SERVER --delete --topic "crayfish.*" --if-exists
  echo "Done!"
elif [[ $EXECUTION_MODE == 'c' ]]; then
  kafka-topics.sh --bootstrap-server $BOOSTRAP_SERVER --delete --topic "$IN_TOPIC.*" --command-config ./experiments-driver/scripts/config.properties --if-exists
  kafka-topics.sh --bootstrap-server $BOOSTRAP_SERVER --delete --topic "$OUT_TOPIC.*" --command-config ./experiments-driver/scripts/config.properties --if-exists
  kafka-topics.sh --bootstrap-server $BOOSTRAP_SERVER --delete --topic "crayfish.*" --command-config ./experiments-driver/scripts/config.properties --if-exists
fi