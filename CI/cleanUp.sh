#!/bin/sh

echo "cleaning up"
COO_CONTAINER=`docker ps | grep coordinator | cut -f1 -d\ `
if [ "$COO_CONTAINER" ]; then
    docker kill $COO_CONTAINER
fi

IRI_CONTAINER=`docker ps | grep iri | cut -f1 -d\ `
if [ "$IRI_CONTAINER" ]; then
    docker kill $IRI_CONTAINER
fi

SIG_SOURCE_SERVER_CONTAINER=`docker ps | grep signature_source_server | cut -f1 -d\ `
if [ "$SIG_SOURCE_SERVER_CONTAINER" ]; then
    docker kill $SIG_SOURCE_SERVER_CONTAINER
fi

rm -rf docs/private_tangle/data
rm -rf docs/private_tangle/db

