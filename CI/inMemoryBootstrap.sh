#!/bin/sh
scriptdir=$(dirname "$(readlink -f "$0")")

if ! /bin/sh ${scriptdir}/startIRI.sh; then
    exit 255
fi



cd docs/private_tangle

echo "starting Compass bootstrap"
./03_run_coordinator.sh -bootstrap -broadcast &
#let compass run for a while
sleep 30

if ! /bin/sh ${scriptdir}/checkLogs.sh; then
    exit 255
fi

cd ../..