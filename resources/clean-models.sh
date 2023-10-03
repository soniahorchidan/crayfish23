#!/bin/bash

# run from root
CWD=$(pwd)
cd $(dirname $(echo $0))

# delete all models and training data
MODEL_PATHS=(
    "embedded/ffnn"
    "embedded/resnet50"
    "external/tf-s/ffnn/models/ffnn"
    "external/tf-s/resnet50/models/resnet50"
    "external/tor-s/ffnn/models"
    "external/tor-s/resnet50/models"
    "training/data"
)

rm -rf ${MODEL_PATHS[@]}

cd $CWD