#!/bin/bash

scriptdir=$(dirname "$(readlink -f "$0")")
. $scriptdir/lib.sh

load_config

docker run -m 4G -t --net host --rm -v $scriptdir/data:/data iota/compass/docker:coordinator -Xmx3G -Xms3G coordinator_deploy.jar \
	-layers /data/layers \
	-statePath /data/compass.state \
	-sigMode $sigMode \
	-powMode $powMode \
	-mwm $mwm \
	-security $security \
	-seed $seed \
	-tick $tick \
	-host $host \
	"$@"
