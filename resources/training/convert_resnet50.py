from pathlib import Path

import torch
import tensorflow as tf

TF_SERVING_PATH = "../external/resnet50/tf-s/models/resnet50/1/"
TORCHSERVE_PATH = "../external/resnet50/tor-s/models/"
DL4J_PATH = "../embedded/resnet50/tf-savedmodel/model-1/"
ONNX_PATH = "../embedded/resnet50/onnx/model-1/"
SAVEDMODEL_PATH = "../embedded/resnet50/tf-savedmodel/model-1/"

Path(TF_SERVING_PATH).mkdir(parents=True, exist_ok=True)
Path(TORCHSERVE_PATH).mkdir(parents=True, exist_ok=True)
Path(DL4J_PATH).mkdir(parents=True, exist_ok=True)
Path(ONNX_PATH).mkdir(parents=True, exist_ok=True)
Path(SAVEDMODEL_PATH).mkdir(parents=True, exist_ok=True)

batch_size = 1
random_input = torch.randn(batch_size, 3, 224, 224, requires_grad=True)

# # torch model
print('Saving torch model...')
model = torch.hub.load('NVIDIA/DeepLearningExamples:torchhub', 'nvidia_resnet50', pretrained=True)
torch.jit.save(torch.jit.trace(model, (random_input)), TORCHSERVE_PATH + "/jit_resnet50.torch")
torch.save(model.state_dict(), TORCHSERVE_PATH + '/resnet50.torch')

# onnx model
print('Saving onnx model...')
model.load_state_dict(torch.load(TORCHSERVE_PATH + '/resnet50.torch'))
batch_size = 1
random_input = torch.randn(batch_size, 3, 224, 224, requires_grad=True)
torch.onnx.export(model,
                  random_input,
                  ONNX_PATH + '/model-resnet50.onnx',
                  opset_version=12,
                  input_names=['input'],  # the model's input names
                  output_names=['output'],  # the model's output names
                  dynamic_axes={'input': {0: 'batch_size'},  # variable length axes
                                'output': {0: 'batch_size'}})

# tensorflow model
# ## NOTE: works to load, but throws error at inference time
# from onnx_tf.backend import prepare
print('Saving tensorflow model...')
# onnx_model = onnx.load(ONNX_PATH + '/model-resnet50.onnx')
# tf_rep = prepare(onnx_model)
# tf_rep.export_graph(SAVEDMODEL_PATH)

# model = tf.keras.applications.ResNet50(weights='imagenet')
# tf.keras.models.save_model(
#     model,
#     SAVEDMODEL_PATH,
#     overwrite=True,
#     include_optimizer=False,
#     save_format=None,
#     signatures=None,
#     options=None
# )

print('Saving tensorflow H5 model...')
model = tf.keras.applications.ResNet50(weights='imagenet', input_shape=(224, 224, 3))
# print(model.summary())
tf.keras.models.save_model(
    model,
    TF_SERVING_PATH,
    overwrite=True,
    include_optimizer=False,
    save_format=None,
    signatures=None,
    options=None
)

tf.keras.models.save_model(
    model,
    DL4J_PATH + '/model.h5',
    overwrite=True,
    include_optimizer=False,
    save_format=None,
    signatures=None,
    options=None
)

print('All done!')
