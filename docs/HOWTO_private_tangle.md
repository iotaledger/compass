# HOWTO: Setting up a private Tangle

## Introduction
A private Tangle consists in a set of IRI nodes interconnected between each other. It is also recommended to create Private Tangles with their own genesis, so to clearly differentiate from any other existing network, whether public or private.

A private Tangle can also consist of a single IRI instance. The instructions below will guide you through creating your own single node private Tangle.

### The components
- IRI — IOTA Reference Implementation — software
- A custom iri.ini file
- A custom snapshot file — genesis
- The Coordinator (COO)

## Step 1: create the IRI node
IRI is the Java open source implementation of the Tangle whitepaper supported by the IOTA Foundation.

Dedicate a Linux server as an IRI node. The server requirements are pretty low, we recommend the following for a better experience:

- VPS or bare metal
- 4 CPUs (or virtual CPUs)
- 8GB RAM
- SSD drive with at least 10GB – highly dependant on how much data you wish to store
- Virtually any Linux distribution, as long as Oracle Java can be installed and run. We recommend Ubuntu Linux and this guide assumes it’s Ubuntu Linux.

We define a directory where all IRI data and configuration will be stored. Let’s call it `/iri`

```bash
mkdir /iri
```

Download the latest IRI from GitHub. At the moment of writing the latest version is [1.5.1](https://github.com/iotaledger/iri/releases/download/v1.5.1/iri-1.5.1.jar)

```bash
cd /iri
wget https://github.com/iotaledger/iri/releases/download/v1.5.1/iri-1.5.1.jar 
```

#### Install Oracle Java 8 JRE
```
add-apt-repository ppa:webupd8team/java
apt-get update
apt-get install oracle-java8-installer
```

#### Configure IRI

```
mkdir /iri/data
mkdir /iri/conf
```

Create a file `/iri/conf/iri.ini` with your editor of choice and paste the following text

```yaml
[IRI]
ZMQ_ENABLED = TRUE
TESTNET = TRUE
MWM = 9
SNAPSHOT_FILE = /iri/conf/snapshots.txt
COORDINATOR = COOADDRESS99999999999999999999999999999999999999999999999999999999999999999999999
MILESTONE_START_INDEX = 2
MAX_DEPTH = 1000
```

Two important things to note:

- The IRI option `MAX_DEPTH = 1000` is required on the node where the COO will be issuing milestones against. If you create more IRI nodes, it is recommended to remove this option from their /iri/conf/iri.ini
- The IRI option `MWM = 9` indicates the minimum weight magnitude (MWM) required by a client when performing proof-of-work (PoW). The minimum value that can be configured is 9. A value lower than that requires code changes in the network stack. Keep in mind that a MWM of 9 requires a negligible amount of PoW, so we do not expect any requirement to lower it further. 
  For comparison, the IOTA Mainnet Network is at MWM 14.

#### Create custom genesis
Create a custom genesis file and place it in `/iri/conf/snapshot.txt`

Here's an example one:
```yaml
WYF9OOFCQJRTLTRMREDWPOBQ9KNDMFVZSROZVXACAWKUMXAIYTFQCPAYZHNGKIWZZGKCSHSSTRDHDAJCW;2779530283277761
```
This allocates all token supply to the seed `SEED99999999999999999999999999999999999999999999999999999999999999999999999999999`


**Important**: the `COORDINATOR` setting specified above is a “dummy” one (`COOADDRESS99999999999999999999999999999999999999999999999999999999999999999999999`). We are now going to bootstrap the coordinator and will get back to this file with the right value to place in it.

## Step 2: Setting up The Coordinator
The COO uses Java to run. These instructions assume that Java is available on the server running the COO. Also, these instructions assume that COO runs on the same server where the IRI node resides, however keep in mind it is not a requirement.

#### Untar the file under `/root`

```
cd /root
tar xvfz coordinator.tgz
```

We now need to bootstrap the coordinator milestone merkle tree. Generate a valid random seed. This is seed is going to be used by the COO to generate and sign milestones. **Do not lose the generated seed.**

```
cat /dev/urandom |LC_ALL=C tr -dc 'A-Z9' | fold -w 81 | head -n 1 
```

The output of the command above will be a random string of 81 chars, all capital letters. From now on we assume that the seed generated is `COOSEED99999999999999999999999999999999999999999999999999999999999999999999999999`. Please always replace `COOSEED99999999999999999999999999999999999999999999999999999999999999999999999999` with your actual Coordinator seed and make sure the seed has been generated with the command above 


Decide on the depth of the coordinator. 
The higher the number, the more milestones can be issued: At depth 18, = ~260 thousand milestones, 20 = ~1 million milestones, 21 = ~2 million milestones – or more precisely 2^DEPTH. For this exercise we use depth 20 — allowing 2^20 milestones to be issued. We will later see that milestones will be issued each 40 seconds.

Keep in mind this process is highly CPU intense. For example, generating a depth 20 tree on a 64 CPU server takes about 1 hour.

```
cd coordinator
./bin/addressGenerator CURLP27 COOSEED99999999999999999999999999999999999999999999999999999999999999999999999999 20 layers.csv
```

The command above will take a while, depending on the depth used and the CPU power of your server. You may want to run it on a much powerful server and then copy the `layers.csv` file over.

```
./bin/merkleTreeCalculator CURLP27 layers.csv layers
```

The command above will take a few minutes or less for our example depth (20).

The last command creates layer files under the layers directory. 

Now, you need to look at the first layer of the COO seed

```
cat layers/layer.0.csv
```

The output is a 81 char string. For example `QAXEPOBAWUVWTPTVDBCDMMQOUXOWZUGAJKAJORW9YKBQWDDUGATZRKZUQMXQFQAOEHJWFNWHS9MQSGHPO`. We call this the COO address. 
Copy the output of the command above and replace it in the `/iri/conf/iri.ini` file, option `COORDINATOR`. 

## Step 3: Running the Node
It’s time to start the node. The following step assumes you are running with Ubuntu or any other Linux distribution that uses `systemd`.
 Please refer to your distribution documentation if you have another init/service manager.

Create an IRI systemd unit by create the file `/etc/systemd/system/iri.service` with the following text

```systemd
[Unit]

Description=IRI
[Service]
WorkingDirectory=/iri
TimeoutStartSec=0
Restart=always
ExecStart=/usr/bin/java \
-Xms1G -Xmx6G
-Djava.net.preferIPv4Stack=true \
-jar iri-1.5.1.jar \
-c conf/iota.ini \
-p 14265 

[Install]
WantedBy=multi-user.target 
```

IMPORTANT: The `systemd` unit above includes hardcoded Java memory settings. Please remember to change them according to your server specifications. The setting above assumes a server has 4GB of RAM and Java can use only up to 3GB.

```
systemctl daemon-reload
```

Start the IRI node.
```
systemctl start iri.service
```

You can check the IRI logs via journalctl (assuming you are on Ubuntu)

```
journalctl -u iri.service -f
```

### IRI node explained
IRI by default uses three ports. If you need to access these ports remotely, please make sure the firewall on your server is setup accordingly. The ports are:

- UDP neighbor peering port (default is `14600`)
- TCP neighbor peering port (default is `15600`)
- TCP HTTP API port (default is `14265`)

#### Checking the Node Status
Using curl and jq you can test the TCP API port.

```
apt-get install -y curl jq
curl -s http://localhost:14265 -X POST -H 'X-IOTA-API-Version: 1' -H 'Content-Type: application/json' -d '{"command": "getNodeInfo"}' | jq
```

Please refer to https://iota.readme.io/reference for all HTTP IRI API commands available. 


## Step 4: Starting the Coordinator
The IRI node is now running but it has not received its first milestone. We need to bootstrap the Tangle. We suggest at this stage to have two terminals open on your server. One with journalctl or equivalent looking at its logs and one running the following commands

```
cd /root/coordinator
./bin/coordinator -layers layers -host http://localhost:14265 -seed COOSEED99999999999999999999999999999999999999999999999999999999999999999999999999 -tick 40000 -depthScale 1.01 -depth 3 -broadcast -mwm 9 -index 2 -bootstrap
```

As always, please change `COOSEED99999999999999999999999999999999999999999999999999999999999999999999999999` with your own COO seed.

Let this command run for until you see a similar output
```
07/20 09:10:43.699 [Latest Milestone Tracker] INFO  com.iota.iri.Milestone - Latest milestone has changed from #2 to #3
07/20 09:10:45.385 [Solid Milestone Tracker] INFO  com.iota.iri.Milestone - Latest SOLID SUBTANGLE milestone has changed from #2 to #3
```
As soon as you see the following output, you can kill the `./bin/coordinator` command by hitting CTRL-C. The Tangle has been bootstrapped. You can now issue another COO command that will run indefinitely. 
At this stage we recommend to run the following command either via a screen session.

```
./bin/coordinator -layers layers -host http://localhost:14265 -seed COOSEED99999999999999999999999999999999999999999999999999999999999999999999999999 -tick 40000 -depthScale 1.01 -depth 3 -broadcast -mwm 9
```

As always, please change `COOSEED99999999999999999999999999999999999999999999999999999999999999999999999999` with your own COO seed.

Every 40 seconds a new milestone will be issued by the COO. IRI should manifest it in its logs by displaying a similar output to the following

```
07/20 09:13:48.699 [Latest Milestone Tracker] INFO  com.iota.iri.Milestone - Latest milestone has changed from #3 to #4
07/20 09:13:50.374 [Solid Milestone Tracker] INFO  com.iota.iri.Milestone - Latest SOLID SUBTANGLE milestone has changed from #3 to #4
```

And so on. You now have a working private Tangle.

## Important things to note
The instructions above do not consider all individual circumstances. They are meant to give the reader an understanding on how to bootstrap a private Tangle on a 1 node network topology. More complex setups can be achieved by running more IRI nodes interconnected between each other.

If you prefer to use Docker, we provide IRI docker containers at https://hub.docker.com/r/iotaledger/iri/ and we recommend to adapt the instructions above by following https://github.com/iotaledger/iri/blob/dev/DOCKER.md