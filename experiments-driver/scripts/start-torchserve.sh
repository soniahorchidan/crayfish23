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

MR=$(getProperty $EXPERIMENT_CONFIG "frontend_batch_size")
FRONTEND_BATCH_SIZE=($(echo "$MR" | tr ',' '\n'))

echo "Archiving PyTorch model..."
mkdir -p model_store/
torch-model-archiver --force --model-name $MODEL_NAME \
  --version 1.0 \
  --export-path model_store \
  --serialized-file ./resources/external/$MODEL_NAME/tor-s/models/jit_$MODEL_NAME.torch \
  --handler ./resources/external/$MODEL_NAME/tor-s/model_handler.py \
  --extra-files ./experiments-driver/configs/model-configs/$MODEL_NAME/model-config.yaml

echo "Starting PyTorch model serving..."
#sed -E -i "s ffnn|resnet50|vgg19 $MODEL_NAME "  ./resources/external/$MODEL_NAME/tor-s/config.properties
torchserve --start --ncs --model-store model_store/ --ts-config ./resources/external/$MODEL_NAME/tor-s/config.properties
sleep 10

# If frontend batching is enabled, set the frontend batch size. Also set the maximum batch delay to a very high value.
if [ -n "$FRONTEND_BATCH_SIZE" ]; then
  echo "Scaling the batch size used for serving..."
  curl -v -X PUT "http://localhost:8081/models/$MODEL_NAME?batch_size=$FRONTEND_BATCH_SIZE&max_batch_delay=600000"
  sleep 2
fi

# Scale PyTorch Model Serving workers
echo "Scaling the number of workers used for serving..."
curl -v -X PUT "http://localhost:8081/models/$MODEL_NAME?min_worker=$MODEL_REPLICAS&max_worker=$MODEL_REPLICAS"
sleep 2

# Check model serving configuration
echo "Updated model serving configuration:"
curl "localhost:8081/models/$MODEL_NAME"
