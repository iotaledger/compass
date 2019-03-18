#!/bin/sh
cd docs/private_tangle

echo "starting IRI"
./02_run_iri.sh &
until [ `docker ps | grep iri | cut -f1 -d\ ` ]; do
sleep 1;
done
sleep 20

echo "starting Compass bootstrap"
./03_run_coordinator.sh -bootstrap -broadcast &
sleep 120

echo "cleaning up"
docker kill $(docker ps | grep compass | cut -f1 -d\ )
docker kill $(docker ps | grep iri | cut -f1 -d\ )