#!/bin/bash

function load_config {
	if [ ! -f config.json ]; then
		echo "Config file 'config.json' does not exist! Please look at config.example.json and create one!"
		exit 1
	fi

	mkdir data &> /dev/null

	host=$(jq -r .host config.json)
	sigMode=$(jq -r .sigMode config.json)
	powMode=$(jq -r .powMode config.json)
	seed=$(jq -r .seed config.json)
	security=$(jq .security config.json)
	depth=$(jq .depth config.json)
	tick=$(jq .tick config.json)
	mwm=$(jq .mwm config.json)


	if [ ! -d data/ ]; then
		mkdir data/
		echo "Depending on OS you might have to set SELinux permissions for data/"
	fi
}
