#!/bin/bash

log() { echo "$(date +%y/%m/%d_%H:%M:%S):: $*"; }

set_em_default() { export EXECUTION_MODE_OPT='l'; }
set_ec_default() { export EXPERIMENTS_CTRL_OPT='d'; }

set_defaults() {
  set_em_default
  set_ec_default
}

# Set configs defaults
set_defaults

# Read command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
  -em | --execution-mode)
    case $2 in
      l | c )
        EXECUTION_MODE_OPT="$2"
        shift
        shift
        ;;
      *)
        echo "ERROR: Invalid arg $2 for $1. Run the script with -h for help." >&2
        exit 1
    esac
    ;;
  -ec | --experiments-control)
    case $2 in
      s | l | d )
        EXPERIMENTS_CTRL_OPT="$2"
        shift
        shift
        ;;
      *)
        echo "ERROR: Invalid arg $2 for $1. Run the script with -h for help." >&2
        exit 1
    esac
    ;;
  -* | --*)
    echo "ERROR: Unknown option $1. Run the script with -h for help." >&2
    exit 1
    ;;
  *)
    echo "ERROR: Unknown option $1. Run the script with -h for help." >&2
    exit 1
    ;;
  esac
done

# Run from the project root
cd $(dirname $(echo $0))

# Start the experiment
log "Crayfish experiments starting"
if [[ $EXECUTION_MODE_OPT == 'l' ]]; then
  log "Experiments running in local execution mode"

  # Open folders which will be later mounted on the containers
  mkdir -p "logs"
  mkdir -p "results-standalone"

  # A unique ID for all experiments in this run
  EXP_UNIQUE_ID=$(date +%s)

  log "Starting standalone container ... "
  docker compose -f docker/standalone/docker-compose.yml run -u $(id -u):$(id -g) standalone \
    /bin/bash -c "./experiments-driver/scripts/start-standalone.sh -em $EXECUTION_MODE_OPT -ec $EXPERIMENTS_CTRL_OPT >logs/$EXP_UNIQUE_ID-standalone-logs.txt" 

elif [[ $EXECUTION_MODE_OPT == 'c' ]]; then
  log "Experiments running in cluster execution mode. Starting Kafka inside a Docker container"
fi