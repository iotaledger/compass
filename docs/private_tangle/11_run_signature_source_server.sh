#!/bin/bash

. lib.sh
load_config

docker run -t --net host --rm -v `pwd`/data:/data iota/compass/docker:signature_source_server signature_source_server_deploy.jar \
	-sigMode $sigMode \
	-security $security \
	-seed $seed \
	-port 50051 \
	"$@"
