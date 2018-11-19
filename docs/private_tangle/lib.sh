#!/bin/bash

function load_config {
	if [ ! -f $scriptdir/config.json ]; then
		echo "Config file 'config.json' does not exist! Please look at config.example.json and create one!"
		exit 1
	fi

	if [ ! -d $scriptdir/data/ ]; then
		  mkdir $scriptdir/data/
		  echo "Depending on OS you might have to set SELinux permissions for data/"
	fi

	if [ ! -d $scriptdir/db/ ]; then
		  mkdir $scriptdir/db/
		  echo "Depending on OS you might have to set SELinux permissions for db/"
	fi

	mkdir $scriptdir/data &> /dev/null
	mkdir $scriptdir/db &> /dev/null

	host=$(jq -r .host $scriptdir/config.json)
	sigMode=$(jq -r .sigMode $scriptdir/config.json)
	powMode=$(jq -r .powMode $scriptdir/config.json)
	seed=$(jq -r .seed $scriptdir/config.json)
	security=$(jq .security $scriptdir/config.json)
	depth=$(jq .depth $scriptdir/config.json)
	tick=$(jq .tick $scriptdir/config.json)
	mwm=$(jq .mwm $scriptdir/config.json)
	milestoneStart=$(jq .milestoneStart $scriptdir/config.json)
}
