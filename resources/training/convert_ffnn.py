from pathlib import Path

import torch
import tensorflow as tf
import torch.nn as nn
import torch.nn.functional as F
from tensorflow import keras
from torch.utils.data import random_split
from torch.utils.data.dataloader import DataLoader
from torchvision.datasets import FashionMNIST
from torchvision.transforms import ToTensor

TF_SERVING_PATH = "../external/ffnn/tf-s/models/ffnn/1/"
TORCHSERVE_PATH = "../external/ffnn/tor-s/models/"
DL4J_PATH = "../embedded/ffnn/dl4j/model-1/"
ONNX_PATH = "../embedded/ffnn/onnx/model-1/"
SAVEDMODEL_PATH = "../embedded/ffnn/tf-savedmodel/model-1/"

Path(TF_SERVING_PATH).mkdir(parents=True, exist_ok=True)
Path(TORCHSERVE_PATH).mkdir(parents=True, exist_ok=True)
Path(DL4J_PATH).mkdir(parents=True, exist_ok=True)
Path(ONNX_PATH).mkdir(parents=True, exist_ok=True)
Path(SAVEDMODEL_PATH).mkdir(parents=True, exist_ok=True)

class FFNN(nn.Module):
    """Simple Feed Forward Neural Network with n hidden layers"""

    def __init__(self, input_size, num_hidden_layers, hidden_size, out_size, accuracy_function):
        super().__init__()
        self.accuracy_function = accuracy_function
        # Create first hidden layer
        self.input_layer = nn.Linear(input_size, hidden_size)
        # Create remaining hidden layers
        self.hidden_layers = nn.ModuleList()
        for i in range(0, num_hidden_layers):
            self.hidden_layers.append(nn.Linear(hidden_size, hidden_size))

        # Create output layer
        self.output_layer = nn.Linear(hidden_size, out_size)

    def forward(self, input_image):
        # Flatten image
        input_image = input_image.view(input_image.size(0), -1)
        # Utilize hidden layers and apply activation function
        output = self.input_layer(input_image)
        output = F.relu(output)
        for layer in self.hidden_layers:
            output = layer(output)
            output = F.relu(output)
        # Get predictions
        output = self.output_layer(output)
        return output

    def training_step(self, batch):
        # Load batch
        images, labels = batch
        # Generate predictions
        output = self(images)
        # Calculate loss
        loss = F.cross_entropy(output, labels)
        return loss

    def validation_step(self, batch):
        # Load batch
        images, labels = batch
        # Generate predictions
        output = self(images)
        # Calculate loss
        loss = F.cross_entropy(output, labels)
        # Calculate accuracy
        acc = self.accuracy_function(output, labels)
        return {'val_loss': loss, 'val_acc': acc}

    def validation_epoch_end(self, outputs):
        batch_losses = [x['val_loss'] for x in outputs]
        # Combine losses and return mean value
        epoch_loss = torch.stack(batch_losses).mean()
        # Combine accuracies and return mean value
        batch_accs = [x['val_acc'] for x in outputs]
        epoch_acc = torch.stack(batch_accs).mean()
        return {'val_loss': epoch_loss.item(), 'val_acc': epoch_acc.item()}

    def epoch_end(self, epoch, result):
        print("Epoch: {} - Validation Loss: {:.4f}, Validation Accuracy: {:.4f}".format(epoch, result['val_loss'],
                                                                                        result['val_acc']))


class ModelTrainer():
    def fit(self, epochs, learning_rate, model, train_loader, val_loader, opt_func=torch.optim.SGD):
        history = []
        optimizer = opt_func(model.parameters(), learning_rate)
        for epoch in range(epochs):
            # Training
            for batch in train_loader:
                loss = model.training_step(batch)
                loss.backward()
                optimizer.step()
                optimizer.zero_grad()
            # Validation
            result = self._evaluate(model, val_loader)
            model.epoch_end(epoch, result)
            history.append(result)

        return history

    def _evaluate(self, model, val_loader):
        outputs = [model.validation_step(batch) for batch in val_loader]
        return model.validation_epoch_end(outputs)


def accuracy(outputs, labels):
    _, preds = torch.max(outputs, dim=1)
    return torch.tensor(torch.sum(preds == labels).item() / len(preds))


def freeze_session(session, keep_var_names=None, output_names=None, clear_devices=True):
    """
    Freezes the state of a session into a pruned computation graph.

    Creates a new computation graph where variable nodes are replaced by
    constants taking their current value in the session. The new graph will be
    pruned so subgraphs that are not necessary to compute the requested
    outputs are removed.
    @param session The TensorFlow session to be frozen.
    @param keep_var_names A list of variable names that should not be frozen,
                          or None to freeze all the variables in the graph.
    @param output_names Names of the relevant graph outputs.
    @param clear_devices Remove the device directives from the graph for better portability.
    @return The frozen graph definition.
    """
    graph = session.graph
    with graph.as_default():
        freeze_var_names = list(set(v.op.name for v in tf.global_variables()).difference(keep_var_names or []))
        output_names = output_names or []
        output_names += [v.op.name for v in tf.global_variables()]
        input_graph_def = graph.as_graph_def()
        if clear_devices:
            for node in input_graph_def.node:
                node.device = ""
        frozen_graph = tf.graph_util.convert_variables_to_constants(
            session, input_graph_def, output_names, freeze_var_names)
        return frozen_graph


if __name__ == '__main__':
    ### save models ###
    ## torch format ##
    batch_size = 128
    input_size = 784
    num_classes = 10
    num_hidden_layers = 3
    validation_size = 10000
    epochs = 1

    data = FashionMNIST(root='data/', download=True, transform=ToTensor())
    train_size = len(data) - validation_size
    train_data, val_data = random_split(data, [train_size, validation_size])
    train_loader = DataLoader(train_data, batch_size, shuffle=True, num_workers=4, pin_memory=True)
    val_loader = DataLoader(val_data, batch_size * 2, num_workers=4, pin_memory=True)

    # define model
    model = FFNN(input_size, num_hidden_layers, 32, out_size=num_classes, accuracy_function=accuracy)
    print(model)

    # print model info
    pytorch_total_params = sum(p.numel() for p in model.parameters())
    print(pytorch_total_params)

    # train model
    model_trainer = ModelTrainer()
    training_history = []
    training_history += model_trainer.fit(epochs, 0.2, model, train_loader, val_loader)

    ## onnx format ##
    print('Saving onnx model...')
    batch_size = 1
    random_input = torch.randn(batch_size, 1, 784, requires_grad=True)
    torch.onnx.export(model,
                      random_input,
                      ONNX_PATH + '/model-ffnn.onnx',
                      input_names=['input'],  # the model's input names
                      output_names=['output'],  # the model's output names
                      dynamic_axes={'input': {0: 'batch_size'},  # variable length axes
                                    'output': {0: 'batch_size'}})

    print('Saving torch model...')
    torch.save(model.state_dict(), TORCHSERVE_PATH + '/ffnn.torch')

    ## JIT save##
    print('Saving torch JIT model...')
    torch.jit.save(torch.jit.trace(model, (random_input)), TORCHSERVE_PATH + "/jit_ffnn.torch")

    ## tensorflow format ##
    print('Saving tensorflow model...')
    fashion_mnist = keras.datasets.fashion_mnist
    (train_images, train_labels), (test_images, test_labels) = fashion_mnist.load_data()

    # scale the values to 0.0 to 1.0
    train_images = train_images / 255.0
    test_images = test_images / 255.0

    # reshape for feeding into the model
    train_images = train_images.reshape(train_images.shape[0], 784)
    test_images = test_images.reshape(test_images.shape[0], 784)

    class_names = ['T-shirt/top', 'Trouser', 'Pullover', 'Dress', 'Coat',
                   'Sandal', 'Shirt', 'Sneaker', 'Bag', 'Ankle boot']
    # train model
    model = keras.Sequential([
        keras.layers.Input(shape=(784)),
        keras.layers.Dense(32, activation='relu', name='input_784_to_32'),
        keras.layers.Dense(32, activation='relu', name='hidden_linear_1'),
        keras.layers.Dense(32, activation='relu', name='hidden_linear_2'),
        keras.layers.Dense(32, activation='relu', name='hidden_linear_3'),
        keras.layers.Dense(10, name='Dense')
    ])
    model.summary()

    testing = False

    model.compile(loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
                  metrics=[keras.metrics.SparseCategoricalAccuracy()])
    model.fit(train_images, train_labels, epochs=epochs)

    test_loss, test_acc = model.evaluate(test_images, test_labels)

    tf.keras.models.save_model(
        model,
        SAVEDMODEL_PATH,
        overwrite=True,
        include_optimizer=False,
        save_format=None,
        signatures=None,
        options=None
    )

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
        DL4J_PATH + "/model.h5",
        overwrite=True,
        include_optimizer=False,
        save_format=None,
        signatures=None,
        options=None
    )
