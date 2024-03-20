#!/usr/bin/env bash

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
  -mf | --model-format)
    MODEL_FORMAT="$2"
    shift
    shift
    ;;
  -econf | --experiment-config)
    EXPERIMENT_CONFIG="$2"
    shift
    shift
    ;;
  -mconf | --model-config)
    MODEL_CONFIG="$2"
    shift
    shift
    ;;
  -* | --*)
    echo "Unknown option $1"
    exit 1
    ;;
  esac
done

echo "Starting Ray!"
./experiments-driver/scripts/start-ray.sh
echo "Started Ray!"
python3 ./rayfish/deploy_rayserve.py -mf $MODEL_FORMAT -mconf $MODEL_CONFIG -econf $EXPERIMENT_CONFIG
