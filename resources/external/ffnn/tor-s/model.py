import torch
import torch.nn as nn
import torch.nn.functional as F


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