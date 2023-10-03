import sys
import argparse
from crayfish.pycrayfish.implementations.PySpark import PySpark
from crayfish.pycrayfish.implementations.RayServe import RayServe

EXP_CONFIGS_PATH = "./experiments/configs/"


def runRayServe(modelConfigPath, globalConfigPath,
                expConfigPath):
    RayServe.run(globalConfigPath, modelConfigPath, expConfigPath)


def runPySpark(modelConfigPath, globalConfigPath,
               expConfigPath):
    PySpark.run(globalConfigPath, modelConfigPath, expConfigPath)


def main(sp, modelFormat, modelConfig, globalConfig, expConfig):
    """
        Entry point to run all the experiments.
        Keyword arguments.
        sp -- stream processor, e.g, ray, pyspark
        modelConfig -- model configuration
        globalConfig -- global configuration
        expConfig --  experiment configuration
    """
    if sp == 'ray':
        runRayServe(modelConfig, globalConfig, expConfig)
    elif sp == 'pyspark':
        runPySpark(modelConfig, globalConfig, expConfig)
    else:
        raise Exception("Unknown stream processor. Please use the Crayfish API to provide your own implementation.")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Please provide your configuration for the experiment')
    parser.add_argument('sp', help='stream processor')
    parser.add_argument('mconf', help='model configuration')
    parser.add_argument('gconf', help='global configuration')
    parser.add_argument('econf', help='experiment configuration')
    args = parser.parse_args()
    # stream processor, modelConfig, globalConfig, expConfig
    main(args.sp, args.mconf, args.gconf, args.econf)
