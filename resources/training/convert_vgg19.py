import tensorflow as tf
import torch
from pathlib import Path

Path("../models/vgg19/torch/model-1").mkdir(parents=True, exist_ok=True)
Path("../models/vgg19/tf-savedmodel/model-1").mkdir(parents=True, exist_ok=True)
Path("../models/vgg19/onnx/model-1").mkdir(parents=True, exist_ok=True)

# torch model
print('Saving torch model...')
model = torch.hub.load('pytorch/vision:v0.10.0', 'vgg19', pretrained=True)
torch.save(model.state_dict(), '../models/vgg19/torch/model-1/vgg19.torch')

# onnx model
print('Saving onnx model...')
model.load_state_dict(torch.load('../models/vgg19/torch/model-1/vgg19.torch'))
batch_size = 1
random_input = torch.randn(batch_size, 3, 224, 224, requires_grad=True)
torch.onnx.export(model,
                  random_input,
                  '../models/vgg19/onnx/model-1/vgg19.onnx',
                  input_names=['input'],  # the model's input names
                  output_names=['output'],  # the model's output names
                  dynamic_axes={'input': {0: 'batch_size'},  # variable length axes
                                'output': {0: 'batch_size'}})

# tensorflow model
print('Saving tensorflow model...')
model = tf.keras.applications.VGG19(weights='imagenet')
tf.keras.models.save_model(
    model,
    '../models/vgg19/tf-savedmodel/model-1/',
    overwrite=True,
    include_optimizer=True,
    save_format=None,
    signatures=None,
    options=None
)

print('All done!')
