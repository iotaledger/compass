#!/bin/bash

. lib.sh
load_config

docker run -t --rm -v `pwd`/data:/data iota/compass/docker:merkle_tree_calculator merkle_tree_calculator_deploy.jar $sigMode /data/addresses.csv /data/layers/
