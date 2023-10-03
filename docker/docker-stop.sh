#!/bin/bash

CWD=$(pwd)
cd $(dirname $(echo $0))
cd ..

# random args, just to stop the services 
echo "Stopping containers ..."
docker compose down
echo "Removing containers ..."
[ "$(docker ps -a -q)" ] && docker rm -f $(docker ps -a -q)
echo "Removing volumes ..."
[ "$(docker volume ls -q)" ] && docker volume rm $(docker volume ls -q)
echo "Done!"

cd $CWD
