FROM pytorch/torchserve:latest

COPY resources/external/tor-s/ffnn ffnn
COPY resources/external/tor-s/resnet50 resnet50
COPY experiments-driver/configs/model-configs model-configs

# Archiving ffnn torch model
RUN torch-model-archiver \
  --force \
  --model-name ffnn \
  --version 1.0 \
  --export-path model-store \
  --serialized-file ffnn/models/jit_ffnn.torch \
  --handler ffnn/model_handler.py \
  --extra-files model-configs/ffnn/model-config.yaml

# Archiving resnet50 torch model
RUN torch-model-archiver \
  --force \
  --model-name resnet50 \
  --version 1.0 \
  --export-path model-store \
  --serialized-file resnet50/models/jit_resnet50.torch \
  --handler resnet50/model_handler.py \
  --extra-files model-configs/resnet50/model-config.yaml