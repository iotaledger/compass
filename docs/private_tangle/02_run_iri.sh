#!/bin/bash

scriptdir=$(dirname "$(readlink -f "$0")")
. $scriptdir/lib.sh

load_config

COO_ADDRESS=$(cat $scriptdir/data/layers/layer.0.csv)

docker pull iotaledger/iri:latest
docker run -t --net host --rm -v $scriptdir/db:/iri/data -v $scriptdir/snapshot.txt:/snapshot.txt -p 14265 iotaledger/iri:latest \
       --testnet \
       --remote \
       --testnet-coordinator $COO_ADDRESS \
       --mwm $mwm \
       --milestone-start $milestoneStart \
       --milestone-keys $depth \
       --snapshot /snapshot.txt \
       --max-depth 1000 $@

