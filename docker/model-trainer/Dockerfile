FROM ubuntu:latest

# python dependencies
RUN apt-get update \
  && apt-get install -y python3 python3-pip --no-install-recommends \
  && rm -rf /var/lib/apt/lists/*

RUN pip3 install tensorflow torch torchvision onnx

# add user
# USER_ID and GROUP_ID should match the user on the host machine
ARG USER_ID
ARG GROUP_ID
ARG USER_NAME="user"
ARG GROUP_NAME="user"

RUN groupadd -f -g ${GROUP_ID} ${GROUP_NAME} \
  && useradd -l -u ${USER_ID} -g ${GROUP_NAME} ${USER_NAME} \
  && mkhomedir_helper ${USER_NAME}

# run as user
USER ${USER_ID}:${GROUP_ID}

ENV CRAYFISH_HOME="/home/${USER_NAME}/crayfish"
RUN mkdir -p ${CRAYFISH_HOME}
