#!/bin/bash

. lib.sh
load_config

docker run -t --rm -v $scriptdir/data:/data iota/compass/docker:layers_calculator layers_calculator_deploy.jar -sigMode $sigMode -seed $seed -depth $depth -security $security -layers /data/layers
