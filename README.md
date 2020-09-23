Deprecated
=============================

This solution has been deprecated. Please use [Hornet coordinator plug-in](https://docs.iota.org/docs/hornet/1.1/tutorials/set-up-a-private-tangle-hornet) when setting up a private Tangle instead. 

# Compass

Compass is an open-source implementation of an IOTA Network coordinator. 

## Getting started
1. Install bazel 0.28.1 from https://bazel.build
2. Build & install the Compass docker image via `bazel run //docker:coordinator`
3. Build & install the LayerCalculator docker image via `bazel run //docker:layers_calculator`
3. Look at the run scripts in [docs/private_tangle](docs/private_tangle) to get you started.

There also exists a more detailed howto for setting up private Tangle networks in [docs/HOWTO_private_tangle.md](docs/HOWTO_private_tangle.md)

