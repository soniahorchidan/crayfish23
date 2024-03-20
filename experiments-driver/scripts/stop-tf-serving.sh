#!/usr/bin/env bash

echo "Stopping Tensorflow model serving..."
docker stop crayfish-tf-serving
echo "Done!"