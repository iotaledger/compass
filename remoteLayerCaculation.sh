#!/bin/sh
cd docs/private_tangle

ROOT_TREE_LOCAL=`cat data/layers/layer.0.csv`
echo "starting Signing server"
./11_run_signature_source_server.sh &
sleep 10

echo "starting remote layer calculation"
./21_calculate_layers_remote.sh

echo "compare results"
ROOT_TREE_REMOTE=`cat data/layers/layer.0.csv`
diff ../data/layers/layer.0.csv data/layers/layer.0.csv
if [ ${ROOT_TREE_REMOTE} = ${ROOT_TREE_LOCAL} ]; then
  >&2 echo "same";
else
  >&2 echo "different: ${ROOT_TREE_REMOTE} != ${ROOT_TREE_LOCAL}"
  exit 255
fi

echo "cleaning up"
docker kill $(docker ps | grep signature_source_server | cut -f1 -d\ )