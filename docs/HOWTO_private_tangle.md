# HOWTO: Setting up a private Tangle

## Introduction
A private Tangle consists in a set of IRI nodes interconnected between each other. We also recommended you create Private Tangles with their own genesis (a custom snapshot file). This will clearly differentiate your Private Tangle from any other existing network, whether public or private.

A private Tangle can consist of a single IRI instance. The instructions below will guide you through creating your own single node private Tangle.

### The components
- IRI — IOTA Reference Implementation — software
- A custom snapshot file — genesis
- The Coordinator (COO)
- The scripts in `docs/private_tangle`

## Important things to note
The instructions below do not consider all individual circumstances of your setup. They are meant to give you an understanding on how to bootstrap your own Private Tangle on a 1 node network topology. More complex setups can be achieved by running more IRI nodes interconnected between each other.

If you prefer to use Docker to set up IRI instances, we provide [IRI docker containers](https://hub.docker.com/r/iotaledger/iri/). We recommend adapting the instructions below by following the [IRI Docker instructions](https://github.com/iotaledger/iri/blob/dev/DOCKER.md).

## Step 1: Setting up the Coordinator
The Coordinator uses Java to run. These instructions assume that you have already setup [bazel](https://bazel.build) on 
your system and installed the `//docker:coordinator` and `//docker:layers_calculator` images. The relevant scripts are inside the `private_tangle` folder.
**The scripts assume that they are in the same folder as the `config.json` file and data folders.**

### Bootstrapping the Coordinator
We now need to bootstrap the Coordinator milestone merkle tree. 
1. Generate a valid random seed. 
   The seed is going to be used by the COO to generate and sign milestones. **Do not lose the generated seed.**

   ```
   cat /dev/urandom |LC_ALL=C tr -dc 'A-Z9' | fold -w 81 | head -n 1 
   ```

   The output of the command above will be a random string of 81 chars, all capital letters, such as this:
   `COOSEED99999999999999999999999999999999999999999999999999999999999999999999999999`. 

2. Decide on the depth of the Coordinator. 

   The higher the number, the more milestones can be issued: At depth 18, = ~260 thousand milestones, 
   20 = ~1 million milestones, 21 = ~2 million milestones – or more precisely 2^DEPTH. 

   For this exercise, we use depth 8 — allowing 256 milestones to be issued. 

   **Keep in mind this process is highly CPU intensive. For example, generating a depth 20 tree on a 64 CPU server takes about 1 hour.**
3. Copy the `config.example.json` file to `config.json` and alter its contents (specifying correct depth & seed).
4. Run the layer calculator via `./01_calculate_layers.sh` from the `private_tangle` folder.
5. After completing execution, the LayersCalculator will tell you the root of the generated merkle tree. *This is the Coordinator's address*. 

## Step 2: Running the IRI node
IRI is an open source, Java reference implementation of the IOTA protocol. The development of IRI is supported by the IOTA Foundation.

Dedicate a Linux server as an IRI node. The server requirements are low, we recommend the following for a better experience:

- VPS or bare metal
- 4 CPUs (or virtual CPUs)
- 8GB RAM
- SSD drive with at least 10GB – highly dependent on how much data you wish to store
- Virtually any Linux distribution, as long as Docker is available. We recommend Ubuntu Linux and this guide assumes it’s Ubuntu Linux.

The script assumes that the DB will be stored in the same path as the script. 

If you look inside the script, here's some of those parameters explaned:

- `--testnet-coordinator $COO_ADDRESS` - the Coordinator address that IRI listens on
- `--mwm` (e.g. `9`) - sets the minimum weight magnitude (MWM) required by a client when performing proof-of-work (PoW). Keep in mind that an MWM of 9 requires a negligible amount of PoW. For comparison, the IOTA Mainnet Network uses `MWM = 14`. 
- `--max-depth` (e.g. `1000`) - only required on the node where the COO will be issuing milestones. If you are creating more than one IRI node, this is not necessary.
- `--milestone-start` (e.g. `1`) - the lowest milestone index that IRI uses
- `--milestone-keys` - see the description of `depth` further above
- `--snapshot` - the file containing the private tangle's current snapshot information

### Create custom genesis
Create a custom genesis `snapshot.txt` file and place it in the same folder as the script.

Here's an **example** one:
```yaml
FJHSSHBZTAKQNDTIKJYCZBOZDGSZANCZSWCNWUOCZXFADNOQSYAHEJPXRLOVPNOQFQXXGEGVDGICLMOXX;2779530283277761
```
This allocates all token supply to seed `SEED99999999999999999999999999999999999999999999999999999999999999999999999999999`

### Start IRI
```
./02_run_iri.sh
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

## Step 3: Running the Coordinator
The IRI node is now running but it has not received its first milestone. We need to bootstrap the Tangle. 
We suggest at this stage to have two terminals open on your server. One with journalctl or equivalent looking at its logs and one running the following commands:

```
./03_run_coordinator.sh -bootstrap -broadcast
```

Let this command run until you see output similar to:
```
07/20 09:10:43.699 [Latest Milestone Tracker] INFO  com.iota.iri.Milestone - Latest milestone has changed from #2 to #3
07/20 09:10:45.385 [Solid Milestone Tracker] INFO  com.iota.iri.Milestone - Latest SOLID SUBTANGLE milestone has changed from #2 to #3
```

For future runs, you no longer need to provide the `-bootstrap` parameter (the Coordinator actually won't start with it).
The `-broadcast` flag, however, is required as a security measure that the Coordinator should actually broadcast its milestones to IRI.

A new milestone will be issued by the COO every 60 seconds (set by `"tick": 60000` in the `config.json`). 

You now have a working Private Tangle.
