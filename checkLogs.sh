#!/bin/sh
scriptdir=$(dirname "$(readlink -f "$0")")

echo "checking containers are running"
if ! [ `docker ps | grep iri | cut -f1 -d\ ` ]; then
    echo "IRI exited, see logs"
    exit 255
fi

if ! [ `docker ps | grep compass | cut -f1 -d\ ` ]; then
    echo "Compass exited, see logs"
    exit 255
fi

echo "scanning logs for errors"
if docker logs $(docker ps | grep iri | cut -f1 -d\ ) | grep -i 'error'; then
    echo "IRI threw errors, see logs"
    exit 255
fi
if docker logs $(docker ps | grep compass | cut -f1 -d\ ) | grep -i 'error'; then
    echo "Compass threw errors, see logs"
    exit 255
fi