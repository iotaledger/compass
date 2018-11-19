#!/bin/bash

scriptdir=$(dirname "$(readlink -f "$0")")
. $scriptdir/lib.sh

load_config

docker run -t --net host --rm -v $scriptdir/data:/data iota/compass/docker:signature_source_server signature_source_server_deploy.jar \
	-sigMode $sigMode \
	-security $security \
	-seed $seed \
	-plaintext \
	-port 50051 \
	"$@"
