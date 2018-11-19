#!/bin/bash

scriptdir=$(dirname "$(readlink -f "$0")")
. $scriptdir/lib.sh

load_config

COO_ADDRESS=$(cat $scriptdir/data/layers/layer.0.csv)

tritsReq=$[243-$mwm]
bytesReq=$[$tritsReq/5 + $[($tritsReq%5) > 0]]
pktSize=$[1604 + $bytesReq]

docker pull iotaledger/iri:latest
docker run -t --net host --rm -v $scriptdir/db:/iri/data -v $scriptdir/snapshot.txt:/snapshot.txt -p 14265 iotaledger/iri:latest \
       --testnet \
       --remote \
       --testnet-coordinator $COO_ADDRESS \
       --mwm $mwm \
       --milestone-start $milestoneStart \
       --milestone-keys $depth \
       --packet-size $pktSize \
       --request-hash-size $bytesReq \
       --snapshot /snapshot.txt \
       --max-depth 1000 $@

