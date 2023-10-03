FROM ubuntu:latest

# python dependencies
RUN apt-get update \
  && apt-get install -y python3 python3-pip --no-install-recommends \
  && rm -rf /var/lib/apt/lists/*

RUN pip3 install "ray[serve]" pyyaml argparse requests tensorflow starlette onnxruntime confluent_kafka

# crayfish files
ENV CRAYFISH_HOME="home/user/crayfish"
RUN mkdir -p ${CRAYFISH_HOME}
WORKDIR ${CRAYFISH_HOME}
COPY rayfish rayfish
COPY resources resources
RUN mkdir -p experiments-driver/configs
COPY experiments-driver/configs/model-configs experiments-driver/configs/model-configs
