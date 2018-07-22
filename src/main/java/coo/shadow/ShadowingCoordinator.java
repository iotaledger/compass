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

package coo.shadow;

import cfb.pearldiver.PearlDiverLocalPoW;
import com.beust.jcommander.JCommander;
import coo.MilestoneDatabase;
import coo.MilestoneSource;
import coo.conf.ShadowingConfiguration;
import coo.crypto.Hasher;
import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;
import jota.dto.response.GetTransactionsToApproveResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;
import jota.pow.SpongeFactory;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * As opposed to the regular `coo.Coordinator`, this coordinator will issue shadow milestones for an existing list of milestones.
 * This is useful if you want to migrate an existing Coordinator to a new seed or hashing method.
 * <p>
 * !!! *NOTE* that the IRI node this ShadowingCoordinator talks to should already be configured to use the new Coordinator address !!!
 */
public class ShadowingCoordinator {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ShadowingConfiguration config;
  private final IotaAPI api;
  private final URL node;
  private final MilestoneSource db;
  private List<OldMilestone> oldMilestones;

  public ShadowingCoordinator(ShadowingConfiguration config) throws IOException {
    this.config = config;
    this.db = new MilestoneDatabase(SpongeFactory.Mode.valueOf(config.powMode), SpongeFactory.Mode.valueOf(config.sigMode), config.layersPath, config.seed);
    this.node = new URL(config.host);
    this.api = new IotaAPI.Builder().localPoW(new PearlDiverLocalPoW())
        .protocol(this.node.getProtocol())
        .host(this.node.getHost())
        .port(Integer.toString(this.node.getPort()))
        .build();
  }

  public static void main(String[] args) throws Exception {
    ShadowingConfiguration config = new ShadowingConfiguration();
    JCommander.newBuilder()
        .addObject(config)
        .build()
        .parse(args);

    ShadowingCoordinator coo = new ShadowingCoordinator(config);
    coo.setup();
    coo.start();
  }

  /**
   * Configures this `ShadowingCoordinator` instance and validates parameters
   *
   * @throws Exception
   */
  private void setup() throws Exception {
    if (config.oldRoot != null) {
      throw new NotImplementedException("oldRoot");
    }

    if (config.milestonesCSV == null) {
      throw new IllegalArgumentException("Need a milestone csv");
    }

    this.oldMilestones = Files.readAllLines(Paths.get(config.milestonesCSV)).stream().map((String s) -> {
      String[] chunks = s.split(",");
      long idx = Long.parseLong(chunks[0]);
      String tail = chunks[1];

      return new OldMilestone(idx, tail);
    })
        .filter(m -> m.milestoneIdx >= config.oldMinIndex && m.milestoneIdx <= config.oldMaxIndex)
        .sorted(Comparator.comparingLong(o -> o.milestoneIdx))
        .collect(Collectors.toList());

    log.info("Loaded {} old milestones", oldMilestones.size());
    log.info("Old milestone indices (min, max): [{}, {}]", oldMilestones.get(0).milestoneIdx, oldMilestones.get(oldMilestones.size() - 1).milestoneIdx);
  }

  /**
   * Broadcasts a list of transactions to an IRI node
   *
   * @param transactions
   * @throws ArgumentException
   */
  private void broadcast(List<Transaction> transactions) throws ArgumentException {
    log.info("Collected {} transactions for broadcast.", transactions.size());

    if (config.broadcast) {
      api.broadcastAndStore(transactions.stream().map(Transaction::toTrytes).toArray(String[]::new));
      log.info("Broadcasted {} transactions.", transactions.size());
    } else {
      log.info("Skipping broadcast.");
    }

    transactions.clear();
  }


  private void start() throws Exception {
    String trunk = config.initialTrunk;
    String branch;

    int newMilestoneIdx = config.index;
    log.info("Starting milestone index: {}", newMilestoneIdx);

    List<Transaction> transactions = new ArrayList<>();

    for (OldMilestone oldMilestone : oldMilestones) {
      branch = oldMilestone.tail;

      List<Transaction> txs = db.createMilestone(trunk, branch, newMilestoneIdx, config.MWM);
      transactions.addAll(txs);
      log.info("Created milestone {}({}) referencing {} and {}", newMilestoneIdx, Hasher.hashTrytes(db.getPoWMode(),
          txs.get(0).toTrytes()), trunk, branch);

      /*
       * If the current list of transactions exceeds the broadcast threshold,
       * broadcast all available transactions.
       * Before continuing the milestone generation, ensures that node has become solid on the new milestones.
       */
      if (transactions.size() >= config.broadcastBatch) {
        broadcast(transactions);

        GetNodeInfoResponse nodeInfo;
        int count = 0;
        while (true) {
          nodeInfo = api.getNodeInfo();
          Thread.sleep(200);
          count++;

          if (nodeInfo.getLatestSolidSubtangleMilestoneIndex() != newMilestoneIdx) {
            continue;
          }

          try {
            GetTransactionsToApproveResponse txToApprove = api.getTransactionsToApprove(3);
            log.info("{} Trunk: {} Branch: {}", count, txToApprove.getBranchTransaction(), txToApprove.getTrunkTransaction());
            if (txToApprove.getBranchTransaction() == null || txToApprove.getTrunkTransaction() == null) {
              throw new RuntimeException("Broke transactions to approve. Repeating check.");
            }

            break;
          } catch (Exception e) {
            log.error("Failed TX TO Approve at milestone: {}, {}", newMilestoneIdx, e.getMessage());
            continue;
          }
        }
      }


      newMilestoneIdx++;

      trunk = Hasher.hashTrytes(db.getPoWMode(), txs.get(0).toTrytes());
    }

    broadcast(transactions);

    log.info("Shadowing complete.");
  }

  class OldMilestone {
    String tail;
    long milestoneIdx;

    public OldMilestone(long milestoneIdx, String tail) {
      this.tail = tail;
      this.milestoneIdx = milestoneIdx;
    }
  }
}
