#!/bin/bash

. lib.sh
load_config

docker run -t --rm -v `pwd`/data:/data iota/compass/docker:address_generator address_generator_deploy.jar $sigMode $seed $security $depth /data/addresses.csv
