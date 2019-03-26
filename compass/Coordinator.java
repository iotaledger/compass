/*
 * This file is part of TestnetCOO.
 *
 * Copyright (C) 2018 IOTA Stiftung
 * TestnetCOO is Copyright (C) 2017-2018 IOTA Stiftung
 *
 * TestnetCOO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * TestnetCOO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with TestnetCOO.  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     IOTA Stiftung <contact@iota.org>
 *     https://www.iota.org/
 */

package org.iota.compass;

import com.beust.jcommander.JCommander;
import jota.IotaAPI;
import jota.dto.response.CheckConsistencyResponse;
import jota.dto.response.GetNodeInfoResponse;
import jota.dto.response.GetTransactionsToApproveResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;
import org.iota.compass.conf.CoordinatorConfiguration;
import org.iota.compass.conf.CoordinatorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Coordinator {
  private static final Logger log = LoggerFactory.getLogger(Coordinator.class);
  private final MilestoneSource db;
  private final IotaAPI api;
  private final CoordinatorConfiguration config;
  private CoordinatorState state;
  private List<IotaAPI> validatorAPIs;

  private long milestoneTick;
  private int depth;

  private Coordinator(CoordinatorConfiguration config, CoordinatorState state, SignatureSource signatureSource) throws IOException {
    this.config = config;
    this.state = state;
    URL node = new URL(config.host);

    this.db = new MilestoneDatabase(config.powMode,
        signatureSource, config.layersPath);
    this.api = new IotaAPI.Builder()
        .protocol(node.getProtocol())
        .host(node.getHost())
        .port(Integer.toString(node.getPort()))
        .build();

    validatorAPIs = config.validators.stream().map(url -> {
      URI uri = URI.create(url);
      return new IotaAPI.Builder().protocol(uri.getScheme())
          .host(uri.getHost())
          .port(Integer.toString(uri.getPort()))
          .build();
    }).collect(Collectors.toList());
  }

  private static CoordinatorState loadState(String path) throws IOException, ClassNotFoundException {
    CoordinatorState state;
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
      state = (CoordinatorState) ois.readObject();
      log.info("loaded index {}", state.latestMilestoneIndex);
    }
    return state;
  }

  private void storeState(CoordinatorState state, String path) throws IOException {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
      oos.writeObject(state);
      log.info("stored index {}", state.latestMilestoneIndex);
    }
  }

  public static void main(String[] args) throws Exception {
    CoordinatorConfiguration config = new CoordinatorConfiguration();
    CoordinatorState state;

    JCommander.newBuilder()
        .addObject(config)
        .acceptUnknownOptions(true)
        .build()
        .parse(args);

    // We want an empty state if bootstrapping
      // and to allow overriding state file using `-index` flag
    if (config.bootstrap || config.index != null) {
      state = new CoordinatorState();
    } else {
      try {
        state = loadState(config.statePath);
      } catch (Exception e) {
        String msg = "Error loading Compass state file '" + config.statePath + "'! State file required if not bootstrapping...";

        log.error(msg, e);
        throw new RuntimeException(e);
      }
    }

    Coordinator coo = new Coordinator(config, state, SignatureSourceHelper.signatureSourceFromArgs(config.signatureSource, args));
    coo.setup();
    coo.start();
  }

  /**
   * Computes the next depth to use for getTransactionsToApprove call.
   *
   * @param currentDepth tip selection depth from the current round
   * @param lastTimestamp when the current round started
   * @return depth for the next round
   */
  private int getNextDepth(int currentDepth, long lastTimestamp) {
    long now = System.currentTimeMillis();
    int nextDepth;

    log.info("Timestamp delta: " + ((now - lastTimestamp)));

    if ((now - lastTimestamp) > ((int) (config.depthScale * Long.valueOf(milestoneTick).floatValue()))) {
      // decrease depth as we took too long.
      nextDepth = currentDepth * 2 / 3;
    } else {
      // increase depth as we seem to have room for growth
      nextDepth = currentDepth * 4 / 3;
    }

    if (nextDepth < config.minDepth) {
      nextDepth = config.minDepth;
    } else if (nextDepth > config.maxDepth) {
      nextDepth = config.maxDepth;
    }

    return nextDepth;
  }

  /**
   * Checks that node is solid, bootstrapped and on latest milestone.
   *
   * @param nodeInfo response from node API call
   * @return true if node is solid
   */
  private boolean nodeIsSolid(GetNodeInfoResponse nodeInfo) {
    if (nodeInfo.getLatestSolidSubtangleMilestoneIndex() != nodeInfo.getLatestMilestoneIndex())
      return false;

    if (!config.inception && (nodeInfo.getLatestSolidSubtangleMilestoneIndex() != state.latestMilestoneIndex))
      return false;

    if (nodeInfo.getLatestMilestone().equals(MilestoneSource.EMPTY_HASH) ||
            nodeInfo.getLatestSolidSubtangleMilestone().equals(MilestoneSource.EMPTY_HASH))
      return false;

    return true;
  }

  private void broadcastLatestMilestone() throws ArgumentException {
    if (config.broadcast) {
      for (String tx : state.latestMilestoneTransactions) {
        api.storeAndBroadcast(tx);
      }
      log.info("Broadcast milestone: " + state.latestMilestoneIndex);
    }
  }

  /**
   * Sets up the coordinator and validates arguments
   */
  private void setup() throws ArgumentException {
    log.info("Setup");
    GetNodeInfoResponse nodeInfoResponse = api.getNodeInfo();

    if (config.bootstrap) {
      log.info("Bootstrapping.");
      if (!nodeInfoResponse.getLatestSolidSubtangleMilestone().equals(MilestoneSource.EMPTY_HASH) ||
              !nodeInfoResponse.getLatestMilestone().equals(MilestoneSource.EMPTY_HASH)) {
        throw new RuntimeException("Network already bootstrapped");
      }
    }

    if (config.index != null) {
      state = new CoordinatorState();
      state.latestMilestoneIndex = config.index;
    }

    log.info("Starting index from: " + state.latestMilestoneIndex);
    if (nodeInfoResponse.getLatestMilestoneIndex() > state.latestMilestoneIndex && !config.inception) {
      throw new RuntimeException("Provided index is lower than latest seen milestone: " +
              nodeInfoResponse.getLatestMilestoneIndex() + " vs " + state.latestMilestoneIndex);
    }

    milestoneTick = config.tick;
    if (milestoneTick <= 0) {
      throw new IllegalArgumentException("tick must be > 0");
    }
    log.info("Setting milestone tick rate (ms) to: " + milestoneTick);


    depth = config.depth;
    if (depth <= 0) {
      throw new IllegalArgumentException("depth must be > 0");
    }
    log.info("Setting initial depth to: " + depth);

    log.info("Validating Coordinator addresses.");
    if (!Objects.equals(nodeInfoResponse.getCoordinatorAddress(), db.getRoot())) {
      log.warn("Coordinator Addresses do not match! {} vs. {}", nodeInfoResponse.getCoordinatorAddress(), db.getRoot());
      if (!config.allowDifferentCooAddress) {
        throw new IllegalArgumentException("Coordinator Addresses do not match!");
      }
    }
  }

  private void start() throws ArgumentException, InterruptedException {
    int bootstrap = config.bootstrap ? 0 : 3;
    int milestonePropagationRetries = 0;
    log.info("Bootstrap mode: " + bootstrap);

    while (true) {
      String trunk, branch;
      GetNodeInfoResponse nodeInfoResponse = api.getNodeInfo();

      if (bootstrap == 2 && !nodeIsSolid(nodeInfoResponse)) {
        log.warn("Node not solid.");
        Thread.sleep(config.unsolidDelay);
        continue;
      }

      // Node is solid.
      if (bootstrap == 0) {
        log.info("Bootstrapping network.");
        trunk = MilestoneSource.EMPTY_HASH;
        branch = MilestoneSource.EMPTY_HASH;
        bootstrap = 1;
      } else if (bootstrap < 3) {
        // Bootstrapping means creating a chain of milestones without pulling in external transactions.
        log.info("Reusing last milestone.");
        trunk = state.latestMilestoneHash;
        branch = MilestoneSource.EMPTY_HASH;
        bootstrap++;
      } else {
        if (!nodeIsSolid(nodeInfoResponse)) {
          if (attemptToRepropagateLatestMilestone(milestonePropagationRetries,
                  nodeInfoResponse.getLatestSolidSubtangleMilestoneIndex())) {
            milestonePropagationRetries++;
            // We wait a third of the milestone tick
            Thread.sleep(milestoneTick / 3);
            continue;
          }
          else {
            throw new RuntimeException("Latest milestone " + state.latestMilestoneHash + " #" +
                    state.latestMilestoneIndex + " is failing to propagate!!!");
          }
        }
        milestonePropagationRetries = 0;
        // GetTransactionsToApprove will return tips referencing latest milestone.
        GetTransactionsToApproveResponse txToApprove = api.getTransactionsToApprove(depth, state.latestMilestoneHash);
        trunk = txToApprove.getTrunkTransaction();
        branch = txToApprove.getBranchTransaction();

        if (!validateTransactionsToApprove(trunk, branch)) {
          throw new RuntimeException("Trunk & branch were not consistent!!! T: " + trunk + " B: " + branch);
        }

      }

      // If all the above checks pass we are ready to issue a new milestone
      state.latestMilestoneIndex++;

      createAndBroadcastMilestone(trunk, branch);
      updateDepth(bootstrap);
      state.latestMilestoneTime = System.currentTimeMillis();

      // Everything went fine, now we store
      try {
        storeState(state, config.statePath);
      } catch (Exception e) {
        String msg = "Error saving Compass state to file '" + config.statePath + "'!";

        log.error(msg, e);
        throw new RuntimeException(e);
      }

      Thread.sleep(milestoneTick);
    }
  }

  private void updateDepth(int bootstrap) {
    int nextDepth;
    if (bootstrap >= 3) {
      nextDepth = getNextDepth(depth, state.latestMilestoneTime);
    } else {
      nextDepth = depth;
    }

    log.info("Depth: " + depth + " -> " + nextDepth);

    depth = nextDepth;
  }

  private void createAndBroadcastMilestone(String trunk, String branch) throws ArgumentException {
    log.info("Issuing milestone: " + state.latestMilestoneIndex);
    log.info("Trunk: " + trunk + " Branch: " + branch);

    List<Transaction> latestMilestoneTransactions = db.createMilestone(trunk, branch, state.latestMilestoneIndex, config.MWM);
    state.latestMilestoneTransactions = latestMilestoneTransactions.stream().map(Transaction::toTrytes).collect(Collectors.toList());
    state.latestMilestoneHash = latestMilestoneTransactions.get(0).getHash();

    // Do not store the state before broadcasting, since if broadcasting fails we should repeat the same milestone.
    broadcastLatestMilestone();
  }

  /**
   * Checks the consistency of 2 given transactions against the nodes specified by {@link #validatorAPIs}
   * @param trunk transaction to be approved by milestone
   * @param branch transaction to be approved by milestone
   * @return {@code true} if the checks passed or didn't take place. Else return {@code false}.
   */
  private boolean validateTransactionsToApprove(String trunk, String branch) {
    if (validatorAPIs.size() > 0) {
      boolean isConsistent = validatorAPIs.parallelStream().allMatch(api -> {
        CheckConsistencyResponse response;
        try {
          response = api.checkConsistency(trunk, branch);
          if (!response.getState()) {
            log.error("{} reported invalid consistency: {}", api.getHost(), response.getInfo());
          }
          return response.getState();
        } catch (ArgumentException e) {
          e.printStackTrace();
          return false;
        }
      });

      return isConsistent;
    }

    //nothing was checked so validation can't fail
    return true;
  }

  /**
   * Attempts to rebroadcast the latest milestone. Should succeed if {@code milestonePropagationRetries}
   * is not above configured threshold.
   *
   * @param milestonePropagationRetries number of propagation retries that have already taken place
   * @param lsm latest solid milestone index
   * @return {@code true} if the milestone was broadcasted again else return false
   * @throws ArgumentException upon an API problem
   */
  private boolean attemptToRepropagateLatestMilestone(int milestonePropagationRetries, int lsm) throws ArgumentException {
    // Bail if we attempted to broadcast the latest Milestone too many times
    if (milestonePropagationRetries > config.propagationRetriesThreshold) {
      return false;
    }
    log.warn("getNodeInfo returned latestSolidSubtangleMilestoneIndex #{}, " +
            "it should be #{}. Rebroadcasting latest milestone.", lsm, state.latestMilestoneIndex);
    // We reissue the previous milestone again
    broadcastLatestMilestone();
    return true;
  }
}
