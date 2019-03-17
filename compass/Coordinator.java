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
  private static final String statePath = System.getProperty("user.dir") + File.separator + CoordinatorState.COORDINATOR_STATE_PATH;
  private final URL node;
  private final MilestoneSource db;
  private final IotaAPI api;
  private final CoordinatorConfiguration config;
  private CoordinatorState state;
  private List<IotaAPI> validatorAPIs;

  private long milestoneTick;
  private int depth;

  public Coordinator(CoordinatorConfiguration config, CoordinatorState state, SignatureSource signatureSource) throws IOException {
    this.config = config;
    this.state = state;
    this.node = new URL(config.host);

    this.db = new MilestoneDatabase(config.powMode,
        signatureSource, config.layersPath);
    this.api = new IotaAPI.Builder()
        .protocol(this.node.getProtocol())
        .host(this.node.getHost())
        .port(Integer.toString(this.node.getPort()))
        .build();

    validatorAPIs = config.validators.stream().map(url -> {
      URI uri = URI.create(url);
      return new IotaAPI.Builder().protocol(uri.getScheme())
          .host(uri.getHost())
          .port(Integer.toString(uri.getPort()))
          .build();
    }).collect(Collectors.toList());
  }

  private static CoordinatorState loadState() throws IOException, ClassNotFoundException {
    CoordinatorState state;
    ObjectInputStream ois = null;
    try {
      ois = new ObjectInputStream(new FileInputStream(CoordinatorState.COORDINATOR_STATE_PATH));
      state = (CoordinatorState) ois.readObject();
    } finally {
      if (ois != null) {
        ois.close();
      }
    }
    return state;
  }

  private void storeState(CoordinatorState state) throws IOException {
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(new FileOutputStream(CoordinatorState.COORDINATOR_STATE_PATH));
      oos.writeObject(state);
    } finally {
      if (oos != null) {
        oos.close();
      }
    }
  }

  public static void main(String[] args) throws Exception {
    CoordinatorConfiguration config = new CoordinatorConfiguration();
    CoordinatorState state;
    // We want an empty state if bootstrapping
    if (config.bootstrap) {
      state = new CoordinatorState();
    } else {
      try {
        state = loadState();
      } catch (Exception e) {
        String msg = "Error loading Compass state file '" + statePath + "'! State file required if not bootstrapping.";

        log.error(msg, e);
        throw new RuntimeException(e);
      }
    }

    JCommander.newBuilder()
        .addObject(config)
        .acceptUnknownOptions(true)
        .build()
        .parse(args);

    Coordinator coo = new Coordinator(config, state, SignatureSourceHelper.signatureSourceFromArgs(config.signatureSource, args));
    coo.setup();
    coo.start();
  }

  /**
   * Computes the next depth to use for getTransactionsToApprove call.
   *
   * @param currentDepth
   * @param lastTimestamp
   * @return
   */
  protected int getNextDepth(int currentDepth, long lastTimestamp) {
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

    // hardcoded lower & upper threshold
    if (nextDepth < 3) {
      nextDepth = 3;
    } else if (nextDepth > 1000) {
      nextDepth = 1000;
    }

    return nextDepth;
  }

  /**
   * Checks that node is solid, bootstrapped and on latest milestone.
   *
   * @param nodeInfo
   * @return true if node is solid
   */
  protected boolean nodeIsSolid(GetNodeInfoResponse nodeInfo) {
    if (nodeInfo.getLatestSolidSubtangleMilestoneIndex() != nodeInfo.getLatestMilestoneIndex())
      return false;

    if (!config.inception && (nodeInfo.getLatestSolidSubtangleMilestoneIndex() != state.latestMilestoneIndex))
      return false;

    if (nodeInfo.getLatestMilestone().equals(MilestoneSource.EMPTY_HASH) || nodeInfo.getLatestSolidSubtangleMilestone().equals(MilestoneSource.EMPTY_HASH))
      return false;

    return true;
  }

  private void broadcastLatestMilestone() throws ArgumentException {
    if (config.broadcast) {
      for (Transaction tx : state.latestMilestoneTransactions) {
        api.storeAndBroadcast(tx.toTrytes());
      }
      log.info("Broadcasted milestone: " + state.latestMilestoneIndex);
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
      if (!nodeInfoResponse.getLatestSolidSubtangleMilestone().equals(MilestoneSource.EMPTY_HASH) || !nodeInfoResponse.getLatestMilestone().equals(MilestoneSource.EMPTY_HASH)) {
        throw new RuntimeException("Network already bootstrapped");
      }
    }

    if (config.index != null) {
      state = new CoordinatorState();
      state.latestMilestoneIndex = config.index;
    }

    log.info("Starting index from: " + state.latestMilestoneIndex);
    if (nodeInfoResponse.getLatestMilestoneIndex() > state.latestMilestoneIndex && !config.inception) {
      throw new RuntimeException("Provided index is lower than latest seen milestone: " + nodeInfoResponse.getLatestMilestoneIndex() + " vs " + state.latestMilestoneIndex);
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
      log.error("Coordinator Addresses do not match! {} vs. {}", nodeInfoResponse.getCoordinatorAddress(), db.getRoot());
      if (!config.allowDifferentCooAddress) {
        throw new IllegalArgumentException("Coordinator Addresses do not match!");
      }
    }
  }

  private void start() throws ArgumentException, InterruptedException, IOException {
    int bootstrap = config.bootstrap ? 0 : 3;
    int milestonePropagationRetries = 0;
    log.info("Bootstrap mode: " + bootstrap);

    while (true) {
      String trunk, branch;
      int nextDepth;
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
        // As it's solid,
        // If the node returns a latest milestone that is not the one we last issued
        if (nodeInfoResponse.getLatestMilestoneIndex() != state.latestMilestoneIndex) {
          // Bail if we attempted to broadcast the latest Milestone too many times
          if (milestonePropagationRetries > config.propagationRetriesThreshold) {
            String msg = "Latest milestone " + state.latestMilestoneHash + " #" + state.latestMilestoneIndex + " is failing to propagate!!!";

            log.error(msg);
            throw new RuntimeException(msg);
          }
          log.warn("getNodeInfo returned latestMilestoneIndex #{}, it should be #{}. Rebroadcasting latest milestone.", nodeInfoResponse.getLatestMilestoneIndex(), state.latestMilestoneIndex);
          // We reissue the previous milestone again
          broadcastLatestMilestone();
          milestonePropagationRetries++;
          // We wait a third of the milestone tick
          Thread.sleep(milestoneTick / 3);
          continue;
        }
        milestonePropagationRetries = 0;
        // GetTransactionsToApprove will return tips referencing latest milestone.
        GetTransactionsToApproveResponse txToApprove = api.getTransactionsToApprove(depth, nodeInfoResponse.getLatestMilestone());
        trunk = txToApprove.getTrunkTransaction();
        branch = txToApprove.getBranchTransaction();

        if (validatorAPIs.size() > 0) {
          boolean isConsistent = validatorAPIs.parallelStream().map(api -> {
            CheckConsistencyResponse response = null;
            try {
              response = api.checkConsistency(trunk, branch);
              if(!response.getState()) {
                log.error("{} reported invalid consistency: {}", api.getHost(), response.getInfo());
              }
              return response.getState();
            } catch (ArgumentException e) {
              e.printStackTrace();
              return false;
            }
          }).allMatch(a -> a == true);

          if (!isConsistent) {
            String msg = "Trunk & branch were not consistent!!! T: " + trunk + " B: " + branch;

            log.error(msg);
            throw new RuntimeException(msg);
          }

        }
      }

      // If all the above checks pass we are ready to issue a new milestone
      state.latestMilestoneIndex++;

      log.info("Issuing milestone: " + state.latestMilestoneIndex);
      log.info("Trunk: " + trunk + " Branch: " + branch);
      state.latestMilestoneTransactions = db.createMilestone(trunk, branch, state.latestMilestoneIndex, config.MWM);

      state.latestMilestoneHash = state.latestMilestoneTransactions.get(0).getHash();

      // Do not store the state before broadcasting, since if broadcasting fails we should repeat the same milestone.
      broadcastLatestMilestone();

      if (bootstrap >= 3) {
        nextDepth = getNextDepth(depth, state.latestMilestoneTime);
      } else {
        nextDepth = depth;
      }

      log.info("Depth: " + depth + " -> " + nextDepth);

      depth = nextDepth;
      state.latestMilestoneTime = System.currentTimeMillis();

      // Everything went fine, now we store
      try {
        storeState(state);
      } catch (Exception e) {
        String msg = "Error saving Compass state to file '" + statePath + "'!";

        log.error(msg, e);
        throw new RuntimeException(e);
      }

      Thread.sleep(milestoneTick);
    }
  }
}
