#!/bin/bash

. lib.sh
load_config

docker run -t --net host --rm -v `pwd`/data:/data iota/compass/docker:coordinator coordinator_deploy.jar \
	-layers /data/layers \
	-powMode $powMode \
	-mwm $mwm \
	-tick $tick \
	-host $host \
	-signatureSource remote \
	-remoteURI localhost:50051 \
	-remotePlaintext \
	"$@"
