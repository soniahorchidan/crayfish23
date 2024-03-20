#!/usr/bin/env bash

function getProperty {
  PROP_FILE=$1
  PROP_KEY=$2
  PROP_VALUE=$(cat $PROP_FILE | grep "$PROP_KEY" | cut -d'=' -f2)
  echo $PROP_VALUE
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
  -mn | --model-name)
    MODEL_NAME="$2"
    shift
    shift
    ;;
  -econf | --experiment-config)
    EXPERIMENT_CONFIG="$2"
    shift
    shift
    ;;
  -* | --*)
    echo "Unknown option $1"
    exit 1
    ;;
  esac
done

# Read model replicas number config
MR=$(getProperty $EXPERIMENT_CONFIG "model_replicas")
MODEL_REPLICAS=($(echo "$MR" | tr ',' '\n'))

MR=$(getProperty $EXPERIMENT_CONFIG "batch_size")
BATCH_SIZE=($(echo "$MR" | tr ',' '\n'))

# Read frontend batch size. If set, the tensorflow serving performs frontend batching
MR=$(getProperty $EXPERIMENT_CONFIG "frontend_batch_size")
FRONTEND_BATCH_SIZE=($(echo "$MR" | tr ',' '\n'))

CWD=$(pwd)
PTH="./resources/external/$MODEL_NAME/tf-s"
cd $PTH

# If frontend batching is enabled, set the frontend batch size. Also set the maximum batch delay to a very high value.
if [ -n "$FRONTEND_BATCH_SIZE" ]; then
  echo "Scaling the batch size used for serving..."
  sed -i "s/max_batch_size { value: 1 }/max_batch_size { value: $FRONTEND_BATCH_SIZE }/g" ./models/batching.config
else
  sed -i "s/max_batch_size { value: .* }/max_batch_size { value: 1 }/g" ./models/batching.config
fi

# Scale Tensorflow Model Serving workers
echo "Scaling the number of parallel threads used for serving to $MODEL_REPLICAS'..."
echo "Parallelism $MODEL_REPLICAS"
echo "Batch Size $BATCH_SIZE"
#sed -i "s/max_batch_size { value: .* }/max_batch_size { value: $BATCH_SIZE }/g" ./models/batching.config
sed -i "s/num_batch_threads { value: .* }/num_batch_threads { value: $MODEL_REPLICAS }/g" ./models/batching.config

# Start Tensorflow Model Serving
echo "Starting Tensorflow model serving..."
docker-compose up -d
sleep 10

# Move to initial path
echo $CWD
cd $CWD
