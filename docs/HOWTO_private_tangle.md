# HOWTO: Setting up a private Tangle

## Introduction
A private Tangle consists in a set of IRI nodes interconnected between each other. We also recommended you create Private Tangles with their own genesis (a custom snapshot file). This will clearly differentiate your Private Tangle from any other existing network, whether public or private.

A private Tangle can consist of a single IRI instance. The instructions below will guide you through creating your own single node private Tangle.



### The components
- IRI — IOTA Reference Implementation — software
- A custom `iota.ini` file
- A custom snapshot file — genesis
- The Coordinator (COO)

## Important things to note
The instructions below do not consider all individual circumstances of your setup. They are meant to give you an understanding on how to bootstrap your own Private Tangle on a 1 node network topology. More complex setups can be achieved by running more IRI nodes interconnected between each other.

If you prefer to use Docker to set up IRI instances, we provide [IRI docker containers](https://hub.docker.com/r/iotaledger/iri/). We recommend adapting the instructions below by following the [IRI Docker instructions](https://github.com/iotaledger/iri/blob/dev/DOCKER.md).

## Step 1: Create the IRI node
IRI is an open source, Java reference implementation of the IOTA protocol. The development of IRI is supported by the IOTA Foundation.

Dedicate a Linux server as an IRI node. The server requirements are low, we recommend the following for a better experience:

- VPS or bare metal
- 4 CPUs (or virtual CPUs)
- 8GB RAM
- SSD drive with at least 10GB – highly dependent on how much data you wish to store
- Virtually any Linux distribution, as long as Oracle Java 8 can be installed and run. We recommend Ubuntu Linux and this guide assumes it’s Ubuntu Linux.
We define a directory where all IRI data and configuration will be stored. Let’s call it `/iri`

```bash
mkdir /iri
```

Download the latest IRI from GitHub. You can always find the latest release at the [release page](https://github.com/iotaledger/iri/releases/latest).

```bash
cd /iri
wget https://github.com/iotaledger/iri/releases/download/<version>/<jar file version>.jar 
```

Replace `version` and `jar file version` with the latest version, for example, `v1.5.5` and `iri-1.5.5`. For example, IRI 1.5.5: 
`https://github.com/iotaledger/iri/releases/download/v1.5.5/iri-1.5.5.jar` 

#### Install Oracle Java 8 JRE

Only Java 8 JRE is supported at the moment. Newer versions of Java JRE may not work as expected. 

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

Create a file `/iri/conf/iota.ini` with your editor of choice and paste the following text

```yaml
[IRI]
ZMQ_ENABLED = TRUE
TESTNET = TRUE
MWM = 9
SNAPSHOT_FILE = /iri/conf/snapshots.txt
COORDINATOR = "coordinator address value" //TODO update with Coordinator address in a later step.
MILESTONE_START_INDEX = 2
MAX_DEPTH = 1000
```

`MAX_DEPTH = 1000` - only required on the node where the COO will be issuing milestones. If you are creating more than one IRI node, we recommend you remove this option from their `iota.ini` file.
`MWM = 9` - sets the minimum weight magnitude (MWM) required by a client when performing proof-of-work (PoW). The minimum value that can be configured is 9. A value lower than that requires code changes in the network stack. Keep in mind that an MWM of 9 requires a negligible amount of PoW, so we do not expect any requirement to lower it further. For comparison, the IOTA Mainnet Network uses `MWM = 14`.
`COORDINATOR` - we will bootstrap the coordinator in **Step 2** and replace the setting value with the correct value.

#### Create custom genesis
Create a custom genesis `snapshot.txt` file and place it in the `/iri/conf` folder of the main node (the node that will be issuing milestones).  

Here's an **example** one:
```yaml
WYF9OOFCQJRTLTRMREDWPOBQ9KNDMFVZSROZVXACAWKUMXAIYTFQCPAYZHNGKIWZZGKCSHSSTRDHDAJCW;2779530283277761
```
This allocates all token supply to seed `SEED99999999999999999999999999999999999999999999999999999999999999999999999999999`

## Step 2: Setting up The Coordinator
The COO uses Java 8 to run. These instructions assume that Java 8 is available on the server running the COO. Also, these instructions assume that COO runs on the same server where the IRI node resides, however keep in mind it is not a requirement.

#### Untar the file under `/root`

```
cd /root
tar xvfz coordinator.tgz
```

We now need to bootstrap the coordinator milestone merkle tree. Generate a valid random seed. The seed is going to be used by the COO to generate and sign milestones. **Do not lose the generated seed.**

```
cat /dev/urandom |LC_ALL=C tr -dc 'A-Z9' | fold -w 81 | head -n 1 
```

The output of the command above will be a random string of 81 chars, all capital letters, such as this: `COOSEED99999999999999999999999999999999999999999999999999999999999999999999999999`. 


Decide on the depth of the coordinator. 
The higher the number, the more milestones can be issued: At depth 18, = ~260 thousand milestones, 20 = ~1 million milestones, 21 = ~2 million milestones – or more precisely 2^DEPTH. For this exercise, we use depth 20 — allowing 2^20 milestones to be issued. We will later see that milestones will be issued every 40 seconds.

Keep in mind this process is highly CPU intensive. For example, generating a depth 20 tree on a 64 CPU server takes about 1 hour.

```
cd coordinator
./bin/addressGenerator CURLP27 <coo seed value> 1 20 layers.csv
```

Please change `<coo seed value>` with your own COO seed.

Allow some time for the above command to complete. The duration depends on the depth used and the CPU power of your server. You may want to run it on a much powerful server and then copy the `layers.csv` file over.

```
./bin/merkleTreeCalculator CURLP27 layers.csv layers
```

The command above will take a few minutes or less for our example depth (20).

The last command creates layer files under the layers directory. 

Now, you need to look at the first layer of the COO seed

```
cat layers/layer.0.csv
```

The output is an 81 char string. For example `QAXEPOBAWUVWTPTVDBCDMMQOUXOWZUGAJKAJORW9YKBQWDDUGATZRKZUQMXQFQAOEHJWFNWHS9MQSGHPO`. We call this the COO address.

1. Copy the COO address.
2. Open the `iota.ini` file. 
3. Replace the placeholder `"coordinator address value"` value in the `COORDINATOR` option with the copied COO address.

## Step 3: Running the Node
It’s time to start the node. The following step assumes you are running with Ubuntu or any other Linux distribution that uses `systemd`.
 Please refer to your distribution documentation if you have another init/service manager.

Create an IRI systemd unit by creating the following file: `/etc/systemd/system/iri.service` 

And populate it with the following text:

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
-jar <jar file value> \
-c conf/iota.ini \
-p 14265 

[Install]
WantedBy=multi-user.target 
```
Replace the `<jar file value>` in the `jar` option with the name of the IRI file you downloaded. For example, `iri-1.5.5.jar`.

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
The IRI node is now running but it has not received its first milestone. We need to bootstrap the Tangle. We suggest at this stage to have two terminals open on your server. One with journalctl or equivalent looking at its logs and one running the following commands:

```
cd /root/coordinator
./bin/coordinator -layers layers -host http://localhost:14265 -seed <coo seed value> -tick 40000 -depthScale 1.01 -depth 3 -broadcast -mwm 9 -index 2 -sigMode CURLP27 -powMode CURLP81 -bootstrap
```

Please change `<coo seed value>` with your own COO seed.

Let this command run until you see output similar to:
```
07/20 09:10:43.699 [Latest Milestone Tracker] INFO  com.iota.iri.Milestone - Latest milestone has changed from #2 to #3
07/20 09:10:45.385 [Solid Milestone Tracker] INFO  com.iota.iri.Milestone - Latest SOLID SUBTANGLE milestone has changed from #2 to #3
```
As soon as you see the following output, you can kill the `./bin/coordinator` command by hitting CTRL-C. The Tangle has been bootstrapped. You can now issue another COO command again, that will run indefinitely. The command will now be issued without the without the `-bootstrap` option.
At this stage we recommend to run the following command via a screen session.

```
./bin/coordinator -layers layers -host http://localhost:14265 -seed <coo seed value> -tick 40000 -depthScale 1.01 -depth 3 -broadcast -mwm 9 -sigMode CURLP27 -powMode CURLP81
```

Please change `<coo seed value>` with your own COO seed.

A new milestone will be issued by the COO every 40 seconds (set by `-tick 40000`). IRI should show this in its logs by displaying output similar to:

```
07/20 09:13:48.699 [Latest Milestone Tracker] INFO  com.iota.iri.Milestone - Latest milestone has changed from #3 to #4
07/20 09:13:50.374 [Solid Milestone Tracker] INFO  com.iota.iri.Milestone - Latest SOLID SUBTANGLE milestone has changed from #3 to #4
```

And so on. You now have a working Private Tangle.