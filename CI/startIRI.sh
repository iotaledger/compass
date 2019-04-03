#!/bin/sh
cd docs/private_tangle

echo "starting IRI"
./02_run_iri.sh &
until [ `docker ps | grep iri | cut -f1 -d\ ` ]; do
sleep 1;
done

echo "waiting for IRI to start"
while ! curl http://localhost:14265 -X POST -H 'Content-Type: application/json' -H 'X-IOTA-API-Version: 1' -d '{"command": "getNodeInfo"}'; do
    echo "API not ready"
    sleep 1;
    if ! [ `docker ps | grep iri | cut -f1 -d\ ` ]; then
        echo "IRI failed to initialize"
        exit 255
    fi
done
echo ""
echo "IRI initialized"