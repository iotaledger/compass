package coo.shadow;

import cfb.pearldiver.PearlDiverLocalPoW;
import com.beust.jcommander.JCommander;
import coo.MilestoneDatabase;
import coo.MilestoneSource;
import coo.conf.ShadowingConfiguration;
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

public class ShadowingCoordinator {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ShadowingConfiguration config;
  private final IotaAPI api;
  private final URL node;
  private final MilestoneSource db;
  private List<OldMilestone> oldMilestones;

  public ShadowingCoordinator(ShadowingConfiguration config) throws IOException {
    this.config = config;
    this.db = new MilestoneDatabase(SpongeFactory.Mode.valueOf(config.mode), config.layersPath, config.seed);
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

  public void setup() throws Exception {
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


  public void start() throws Exception {
    String trunk = config.initialTrunk;
    String branch;

    int newMilestoneIdx = config.index;
    log.info("Starting milestone index: {}", newMilestoneIdx);

    List<Transaction> transactions = new ArrayList<>();

    for (OldMilestone oldMilestone : oldMilestones) {
      branch = oldMilestone.tail;

      List<Transaction> txs = db.createMilestone(trunk, branch, newMilestoneIdx, config.MWM);
      transactions.addAll(txs);
      log.info("Created milestone {}({}) referencing {} and {}", newMilestoneIdx, txs.get(0).getHash(), trunk, branch);

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
            if (txToApprove.getBranchTransaction() == null || txToApprove.getTrunkTransaction() == null)
              throw new RuntimeException("e");

            break;
          } catch (Exception e) {
            log.error("Failed TX TO Approve at milestone: {}, {}", newMilestoneIdx, e.getMessage());
            continue;
          }
        }
      }


      newMilestoneIdx++;

      trunk = txs.get(0).getHash();
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
