#!/bin/sh
scriptdir=$(dirname "$(readlink -f "$0")")

if ! /bin/sh startIRI.sh; then
    exit 255
fi

cd docs/private_tangle

echo "starting Compass bootstrap"
./03_run_coordinator.sh -bootstrap -broadcast &
sleep 30

echo "restarting Compass"
docker kill $(docker ps | grep compass | cut -f1 -d\ )
while [ `docker ps | grep compass | cut -f1 -d\ ` ]; do
sleep 1;
done
sleep 2

./03_run_coordinator.sh -broadcast &
sleep 30

if ! /bin/sh ${scriptdir}/checkLogs.sh; then
    exit 255
fi

cd ../..