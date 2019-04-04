#!/bin/sh
scriptdir=$(dirname "$(readlink -f "$0")")

if ! /bin/sh ${scriptdir}/startIRI.sh; then
    exit 255
fi

cd docs/private_tangle

echo "starting Signing server"
./11_run_signature_source_server.sh &
sleep 10

echo "starting Compass bootstrap"
./12_run_coordinator_remote.sh -bootstrap -broadcast &
sleep 20

echo "restarting Compass"
docker kill $(docker ps | grep coordinator | cut -f1 -d\ )
while [ `docker ps | grep coordinator | cut -f1 -d\ ` ]; do
sleep 1;
done
sleep 2

./12_run_coordinator_remote.sh -broadcast &
sleep 20

if ! /bin/sh ${scriptdir}/checkLogs.sh; then
    exit 255
fi

cd ../..