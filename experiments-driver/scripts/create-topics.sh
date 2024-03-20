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
  -r | --replication)
    REPLICATION="$2"
    shift
    shift
    ;;
  -gconfig | --global-config)
    GLOBAL_CONFIGS="$2"
    shift
    shift
    ;;
  -econfig | --exp-config)
    EXP_CONFIGS="$2"
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

if [[ -z "${REPLICATION}" ]]; then
  REPLICATION=1
fi

MR=$(getProperty $EXP_CONFIGS "model_replicas")
MODEL_REPLICAS=$(echo "$MR" | tr ',' '\n')

MR=$(getProperty $EXP_CONFIGS "input_rate")
INPUT_RATE=$(echo "$MR" | tr ',' '\n')

MR=$(getProperty $EXP_CONFIGS "batch_size")
BATCH_SIZE=$(echo "$MR" | tr ',' '\n')

MR=$(getProperty $GLOBAL_CONFIGS "kafka.input.data.partitions.num")
PARTITIONS_IN=$(echo "$MR" | tr ',' '\n')

MR=$(getProperty $GLOBAL_CONFIGS "kafka.output.data.partitions.num")
PARTITIONS_OUT=$(echo "$MR" | tr ',' '\n')

NOW=$(date +%s)
EXP_FOOTPRINT="ir$INPUT_RATE-bs$BATCH_SIZE-mr$MODEL_REPLICAS-$NOW"

MR=$(getProperty $GLOBAL_CONFIGS "kafka.input.root.topic")
IN_TOPIC=$(echo "$MR" | tr ',' '\n')
INPUT_TOPIC_NAME="$IN_TOPIC-$EXP_FOOTPRINT"

MR=$(getProperty $GLOBAL_CONFIGS "kafka.output.root.topic")
OUT_TOPIC=$(echo "$MR" | tr ',' '\n')
OUTPUT_TOPIC_NAME="$OUT_TOPIC-$EXP_FOOTPRINT"

MR=$(getProperty $GLOBAL_CONFIGS "kafka.bootstrap.servers")
BOOSTRAP_SERVER=$(echo "$MR" | tr ',' '\n')

# TODO: unify :(
if [[ $EXECUTION_MODE == 'l' ]]; then
  echo "Waiting for the kafka broker to start..."
  docker exec crayfish-kafka-1 kafka-topics --bootstrap-server localhost:29092 --list
  echo "Kafka broker started!"
  ./experiments-driver/scripts/delete-topics.sh -gconfig $GLOBAL_CONFIGS -em 'l'
  docker exec crayfish-kafka-1 kafka-topics --bootstrap-server $BOOSTRAP_SERVER --create --if-not-exists --topic $INPUT_TOPIC_NAME --replication-factor $REPLICATION --partitions $PARTITIONS_IN
  echo "Created topic $IN_TOPIC with $PARTITIONS_IN partitions."
  docker exec crayfish-kafka-1 kafka-topics --bootstrap-server $BOOSTRAP_SERVER --create --if-not-exists --topic $OUTPUT_TOPIC_NAME --replication-factor $REPLICATION --partitions $PARTITIONS_OUT --config message.timestamp.type=LogAppendTime
  echo "Created topic $OUT_TOPIC with $PARTITIONS_OUT partition."
  sed -i '' "s/kafka.input.data.topic=.*/kafka.input.data.topic=$INPUT_TOPIC_NAME/" ./experiments-driver/configs/global-configs-local.properties
  sed -i '' "s/kafka.output.topic=.*/kafka.output.topic=$OUTPUT_TOPIC_NAME/" ./experiments-driver/configs/global-configs-local.properties

elif [[ $EXECUTION_MODE == 'c' ]]; then
  echo "Waiting for the kafka broker to start..."
  kafka-topics.sh --bootstrap-server $BOOSTRAP_SERVER --list --command-config ./experiments-driver/scripts/config.properties
  echo "Kafka broker started!"
  ./experiments-driver/scripts/delete-topics.sh -gconfig $GLOBAL_CONFIGS -em 'c'
  kafka-topics.sh --bootstrap-server $BOOSTRAP_SERVER --create --topic $INPUT_TOPIC_NAME --replication-factor $REPLICATION --partitions $PARTITIONS_IN --command-config ./experiments-driver/scripts/config.properties --config max.message.bytes=52428800
  echo "Created topic $INPUT_TOPIC_NAME with $PARTITIONS_IN partitions."
  kafka-topics.sh --bootstrap-server $BOOSTRAP_SERVER --create --topic $OUTPUT_TOPIC_NAME --replication-factor $REPLICATION --partitions $PARTITIONS_OUT --command-config ./experiments-driver/scripts/config.properties --config message.timestamp.type=LogAppendTime --config max.message.bytes=52428800
  echo "Created topic $OUTPUT_TOPIC_NAME with $PARTITIONS_OUT partition."
  sed -i "s/kafka.input.data.topic=.*/kafka.input.data.topic=$INPUT_TOPIC_NAME/" ./experiments-driver/configs/global-configs-cluster.properties
  sed -i "s/kafka.output.topic=.*/kafka.output.topic=$OUTPUT_TOPIC_NAME/" ./experiments-driver/configs/global-configs-cluster.properties
fi
