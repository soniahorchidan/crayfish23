#!/bin/bash

CWD=$(pwd)
cd $(dirname $(echo $0))
cd ..

DOCKER_ARGS="--build-arg USER_ID=$(id -u) --build-arg GROUP_ID=$(id -g)"

# model-trainer
docker compose -f docker/model-trainer/docker-compose.yml build model-trainer $DOCKER_ARGS

# standard experiment (for run.sh)
docker compose build input-producer $DOCKER_ARGS
docker compose build data-processor $DOCKER_ARGS
docker compose build output-consumer $DOCKER_ARGS
docker compose build external-serving-agent $DOCKER_ARGS
docker compose build torch-serving
docker compose build tf-serving
docker compose build ray-serving

# standalone experiment (for run-standalone.sh)
#docker compose -f docker/standalone/docker-compose.yml build standalone $DOCKER_ARGS

cd $CWD