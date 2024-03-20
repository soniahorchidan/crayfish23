#!/bin/bash

echo "Stopping Docker..."
docker stop crayfish-kafka-1
docker stop crayfish-zookeeper-1
echo "Done!"
