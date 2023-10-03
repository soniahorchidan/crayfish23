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
    GLOBAL_CONFIG="$2"
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

# exp config
BATCH_SIZE=$(getProperty $EXP_CONFIGS "batch_size")
MODEL_REPLICAS=$(getProperty $EXP_CONFIGS "model_replicas")
INPUT_RATE=$(getProperty $EXP_CONFIGS "input_rate")

# global config
PARTITIONS_IN=$(getProperty $GLOBAL_CONFIG "kafka.input.data.partitions.num")
PARTITIONS_OUT=$(getProperty $GLOBAL_CONFIG "kafka.output.data.partitions.num")
INPUT_TOPIC=$(getProperty $GLOBAL_CONFIG "kafka.input.root.topic")
OUTPUT_TOPIC=$(getProperty $GLOBAL_CONFIG "kafka.output.root.topic")
BOOTSTRAP_SERVER=$(getProperty $GLOBAL_CONFIG "kafka.client.endpoint")

# Start kafka topic
EXP_FOOTPRINT="ir$INPUT_RATE-bs$BATCH_SIZE-mr$MODEL_REPLICAS-$(date +%s)"
INPUT_TOPIC_NAME="$INPUT_TOPIC-$EXP_FOOTPRINT"
OUTPUT_TOPIC_NAME="$OUTPUT_TOPIC-$EXP_FOOTPRINT"

if [[ $EXECUTION_MODE == 'l' ]]; then
  SH_TOPIC="${KAFKA_HOME}/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER"
elif [[ $EXECUTION_MODE == 'c' ]]; then
  KAFKA_CONFIG="${CRAYFISH_HOME}/experiments-driver/scripts/config.properties"
  SH_TOPIC="${KAFKA_HOME}/bin/kafka-topics.sh --bootstrap-server $BOOTSTRAP_SERVER --command-config $KAFKA_CONFIG"
fi

echo "Waiting for the kafka broker to start..."
echo "Listing all topics..."
/bin/bash -c "$SH_TOPIC --list"
$CRAYFISH_HOME/experiments-driver/scripts/delete-topics.sh -gconfig $GLOBAL_CONFIG -em $EXECUTION_MODE
echo "Creating new crayfish topics..."
/bin/bash -c "$SH_TOPIC --create --topic $INPUT_TOPIC_NAME --replication-factor $REPLICATION --partitions $PARTITIONS_IN --config message.timestamp.type=LogAppendTime --config max.message.bytes=52428800"
echo "Created topic $INPUT_TOPIC_NAME with $PARTITIONS_IN partitions."
/bin/bash -c "$SH_TOPIC --create --topic $OUTPUT_TOPIC_NAME --replication-factor $REPLICATION --partitions $PARTITIONS_OUT --config message.timestamp.type=LogAppendTime --config max.message.bytes=52428800"
echo "Created topic $OUTPUT_TOPIC_NAME with $PARTITIONS_OUT partition."
echo "Kafka broker started!"

echo "Updating I/O topics name in global configs"
sed -i "s/kafka.input.data.topic=.*/kafka.input.data.topic=$INPUT_TOPIC_NAME/" $GLOBAL_CONFIG
sed -i "s/kafka.output.topic=.*/kafka.output.topic=$OUTPUT_TOPIC_NAME/" $GLOBAL_CONFIG
