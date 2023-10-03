#!/bin/bash

# run from root
CWD=$(pwd)
cd $(dirname $(echo $0))
cd ..

# train ffnn and resnet50
./resources/clean-models.sh
mkdir -p resources/embedded
docker compose -f docker/model-trainer/docker-compose.yml run -u $(id -u):$(id -g) \
    model-trainer python3 training/convert_ffnn.py
docker compose -f docker/model-trainer/docker-compose.yml run -u $(id -u):$(id -g) \
    model-trainer python3 training/convert_resnet50.py

cd $CWD