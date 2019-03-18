#!/bin/sh

#prep environment
echo "building docker images"
bazel run //docker:coordinator
bazel run //docker:layers_calculator
bazel run //docker:signature_source_server

echo "setting up configs"
cd docs/private_tangle
cat config.example.json| jq '.tick = 5000' > config.json
cp snapshot.example.txt snapshot.txt

echo "calculating merkle tree"
./01_calculate_layers.sh
