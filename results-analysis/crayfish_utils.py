import os
from statistics import mean
import numpy as np

def list_dirs(dr):
    r = []
    for root, dirs, files in os.walk(dr):
        for name in dirs:
            r.append(os.path.join(root, name))
        break
    return r


def list_files(dr):
    r = []
    for root, dirs, files in os.walk(dr):
        for name in files:
            if ".DS_Store" != name:
                r.append(os.path.join(root, name))
        break
    return r


def get_file_name(file_path):
    return os.path.splitext(os.path.basename(file_path))[0]


def get_measurements(dr):
    # reads the files line by line to avoid loading all the results in memory
    experiment_results = {}
    dirs = [d for d in list_dirs(dr)]
    for s in dirs:
        stream_processor = get_file_name(s)

        for m in list_dirs(s):
            model_name = get_file_name(m)
            for mf in list_dirs(m):
                model_format = get_file_name(mf)

                for exp_res in list_files(mf):
                    filename = os.fsdecode(exp_res)
                    print(filename)
                    times = []

                    with open(exp_res) as f_in:
                        for line in f_in:
                            times.append(tuple(map(int, line.rstrip('\n').split(","))))
                                
                    if len(times) == 0:
                        continue

                    # strip warm-up queries
                    warm_up_requests_num = int(len(times) * 0.25)
                    times = times[warm_up_requests_num:]
                    exp_footprint = get_file_name(exp_res)

                    # extract experiment footprint details
                    exp_details = exp_footprint.split("-")[1:]
                    input_rate = int(exp_details[0][2:])
                    batch_size = int(exp_details[1][2:])
                    model_replicas = int(exp_details[2][2:])

                    exp_footprint = str(input_rate) + "_" + str(batch_size) + "_" + str(model_replicas)

                    if exp_footprint not in experiment_results:
                        experiment_results[exp_footprint] = {}
                    if stream_processor not in experiment_results[exp_footprint]:
                        experiment_results[exp_footprint][stream_processor] = {}
                    if model_name not in experiment_results[exp_footprint][stream_processor]:
                        experiment_results[exp_footprint][stream_processor][model_name] = {}
                    if model_format not in experiment_results[exp_footprint][stream_processor][model_name]:
                        experiment_results[exp_footprint][stream_processor][model_name][model_format] = []
                        
                    # compute metrics
                    start_generation = min([item[0] for item in times])
                    end_generation = max([item[0] for item in times])
                    time_to_complete_generation = (end_generation - start_generation) * 0.001 # convert to seconds
                    end = max([item[1] for item in times])
                    time_to_complete = (end - start_generation) * 0.001 # convert to seconds
                    throughput = len(times) / time_to_complete 
                    avg_latency = np.average([end - start for (start, end) in times])
                    
                    experiment_results[exp_footprint][stream_processor][model_name][model_format].append({"avg_latency": avg_latency, "throughput": throughput})
    return experiment_results
    
