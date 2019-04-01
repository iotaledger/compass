#!/bin/sh

echo "cleaning up"
docker kill $(docker ps | grep coordinator | cut -f1 -d\ )
docker kill $(docker ps | grep signature_source_server | cut -f1 -d\ )
docker kill $(docker ps | grep iri | cut -f1 -d\ )

rm -rf docs/private_tangle/data
rm -rf docs/private_tangle/db

