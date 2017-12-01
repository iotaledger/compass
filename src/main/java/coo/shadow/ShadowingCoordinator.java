package coo.shadow;

import cfb.pearldiver.PearlDiverLocalPoW;
import com.beust.jcommander.JCommander;
import coo.MilestoneDatabase;
import coo.conf.ShadowingConfiguration;
import jota.IotaAPI;
import jota.model.Transaction;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ShadowingCoordinator {
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ShadowingConfiguration config;
  private final IotaAPI api;
  private final URL node;
  private final MilestoneDatabase db;
  private List<OldMilestone> oldMilestones;

  public ShadowingCoordinator(ShadowingConfiguration config) throws IOException {
    this.config = config;
    this.db = new MilestoneDatabase(config.layersPath, config.seed);
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
        .collect(Collectors.toList());

    log.info("Loaded {} old milestones", oldMilestones.size());
  }

  public void start() throws Exception {
    String trunk = MilestoneDatabase.EMPTY_HASH;
    String branch = MilestoneDatabase.EMPTY_HASH;

    System.err.println("config: " + config.index);
    int newMilestoneIdx = config.index;

    for (OldMilestone oldMilestone : oldMilestones) {
      branch = oldMilestone.tail;

      List<Transaction> txs = db.createMilestone(trunk, branch, newMilestoneIdx, config.MWM);
      log.info("Created milestone {}({}) referencing {} and {}", newMilestoneIdx, txs.get(0).getHash(), trunk, branch);

      if (config.broadcast) {
        for (Transaction tx : txs) {
          api.broadcastAndStore(tx.toTrytes());
        }
        log.info("Broadcasted milestone");

        Thread.sleep(500);
      }

      newMilestoneIdx++;

      trunk = txs.get(0).getHash();
    }

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
