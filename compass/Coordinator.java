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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class Coordinator {
  private static final Logger log = LoggerFactory.getLogger(Coordinator.class);
  private final URL node;
  private final MilestoneSource db;
  private final IotaAPI api;
  private final CoordinatorConfiguration config;
  private int latestMilestone;
  private String latestMilestoneHash;
  private long latestMilestoneTime;
  private List<IotaAPI> validatorAPIs;

  private long milestoneTick;
  private int depth;

  public Coordinator(CoordinatorConfiguration config, SignatureSource signatureSource) throws IOException {
    this.config = config;
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

  public static void main(String[] args) throws Exception {
    CoordinatorConfiguration config = new CoordinatorConfiguration();

    JCommander.newBuilder()
        .addObject(config)
        .acceptUnknownOptions(true)
        .build()
        .parse(args);

    Coordinator coo = new Coordinator(config, SignatureSourceHelper.signatureSourceFromArgs(config.signatureSource, args));
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

    if (!config.inception && (nodeInfo.getLatestSolidSubtangleMilestoneIndex() != latestMilestone))
      return false;

    if (nodeInfo.getLatestMilestone().equals(MilestoneSource.EMPTY_HASH) || nodeInfo.getLatestSolidSubtangleMilestone().equals(MilestoneSource.EMPTY_HASH))
      return false;

    return true;
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
      latestMilestone = config.index;
    } else {
      // = node's start milestone index if bootstrap
      latestMilestone = nodeInfoResponse.getLatestMilestoneIndex();
    }

    log.info("Starting index from: " + latestMilestone);
    if (nodeInfoResponse.getLatestMilestoneIndex() > latestMilestone && !config.inception) {
      throw new RuntimeException("Provided index is lower than latest seen milestone: " + nodeInfoResponse.getLatestMilestoneIndex() + " vs " + latestMilestone);
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
  }

  private void start() throws ArgumentException, InterruptedException {
    int bootstrap = config.bootstrap ? 0 : 3;
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
        trunk = latestMilestoneHash;
        branch = MilestoneSource.EMPTY_HASH;
        bootstrap++;
      } else {
        // As it's solid,
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

      latestMilestone++;

      log.info("Issuing milestone: " + latestMilestone);
      log.info("Trunk: " + trunk + " Branch: " + branch);
      List<Transaction> txs = db.createMilestone(trunk, branch, latestMilestone, config.MWM);

      latestMilestoneHash = txs.get(0).getHash();

      if (config.broadcast) {
        for (Transaction tx : txs) {
          api.storeAndBroadcast(tx.toTrytes());
        }
        log.info("Broadcasted milestone.");
      }

      log.info("Emitted milestone: " + latestMilestone);

      if (bootstrap >= 3) {
        nextDepth = getNextDepth(depth, latestMilestoneTime);
      } else {
        nextDepth = depth;
      }

      log.info("Depth: " + depth + " -> " + nextDepth);

      depth = nextDepth;
      latestMilestoneTime = System.currentTimeMillis();

      Thread.sleep(milestoneTick);
    }
  }
}
