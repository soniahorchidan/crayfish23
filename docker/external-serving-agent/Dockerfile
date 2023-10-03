FROM ubuntu:latest

# add user
# USER_ID and GROUP_ID should match the user on the host machine
ARG USER_ID
ARG GROUP_ID
ARG USER_NAME="user"
ARG GROUP_NAME="user"
ARG USER_HOME_DIR="/home/${USER_NAME}"

RUN groupadd -f -g ${GROUP_ID} ${GROUP_NAME} \
  && useradd -l -u ${USER_ID} -g ${GROUP_NAME} ${USER_NAME} \
  && mkhomedir_helper ${USER_NAME}

# crayfish external dependencies
# 1. python3, netcat, docker.io
RUN apt-get update \
  && apt-get install -y ca-certificates curl git netcat python3 docker.io --no-install-recommends \
  && rm -rf /var/lib/apt/lists/*

# 2. docker compose
ARG DOCKER_CONFIG=${DOCKER_CONFIG:-${USER_HOME_DIR}/.docker}
RUN mkdir -p $DOCKER_CONFIG/cli-plugins 
RUN curl -SL https://github.com/docker/compose/releases/download/v2.17.2/docker-compose-linux-x86_64 -o $DOCKER_CONFIG/cli-plugins/docker-compose
RUN chmod +x $DOCKER_CONFIG/cli-plugins/docker-compose

# run as user
USER ${USER_ID}:${GROUP_ID}

# project folder name in the container has to be exactly as same as the outside one to make docker compose works
ENV CRAYFISH_HOME="${USER_HOME_DIR}/crayfish-dev"
RUN mkdir -p ${CRAYFISH_HOME}
WORKDIR ${CRAYFISH_HOME}

# crayfish files
RUN mkdir -p experiments-driver
COPY --chown=${USER_ID}:${GROUP_ID} experiments-driver/scripts experiments-driver/scripts
RUN mkdir -p experiments-driver/configs
COPY --chown=${USER_ID}:${GROUP_ID} experiments-driver/configs/model-configs experiments-driver/configs/model-configs
COPY --chown=${USER_ID}:${GROUP_ID} resources resources
COPY --chown=${USER_ID}:${GROUP_ID} docker-compose.yml docker-compose.yml
COPY --chown=${USER_ID}:${GROUP_ID} log4j.properties ./
