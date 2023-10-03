import yaml
from crayfish.pycrayfish.adapter import RayServeCrayfishAdapter

CONFIG_NAME = 'model.path.torch_jit'


class RayServe:
    def run(self, globalConfigPath, modelConfigPath, expConfigPath):
        with open(modelConfigPath, 'r') as stream:
            try:
                model_config = yaml.safe_load(stream)
            except yaml.YAMLError as exc:
                print(exc)

        model_endpoint = model_config[CONFIG_NAME]
        adapter = RayServeCrayfishAdapter(model_endpoint, globalConfigPath, expConfigPath)
        adapter.run()

