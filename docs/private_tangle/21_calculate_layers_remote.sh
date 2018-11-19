#!/bin/bash

scriptdir=$(dirname "$(readlink -f "$0")")
. $scriptdir/lib.sh

load_config

docker run -t --net host --rm -v $scriptdir/data:/data iota/compass/docker:layers_calculator layers_calculator_deploy.jar -depth $depth -layers /data/layers \
	-signatureSource remote \
	-remoteURI localhost:50051 \
	-remotePlaintext \

