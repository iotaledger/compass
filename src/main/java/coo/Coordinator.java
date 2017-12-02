package coo;

import cfb.pearldiver.PearlDiverLocalPoW;
import com.beust.jcommander.JCommander;
import coo.conf.BaseConfiguration;
import coo.conf.Configuration;
import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;
import jota.dto.response.GetTransactionsToApproveResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Coordinator {
  private final URL node;
  private final MilestoneDatabase db;
  private final IotaAPI api;
  private final Configuration config;

  private final Logger log = Logger.getLogger("COO");
  private List<String> confirmedTips = new ArrayList<>();

  private int latestMilestone;
  private long latestMilestoneTime;

  private long MILESTONE_TICK;
  private int DEPTH;

  public Coordinator(Configuration config) throws IOException {
    this.config = config;
    this.node = new URL(config.host);
    this.db = new MilestoneDatabase(config.layersPath, config.seed);

    this.api = new IotaAPI.Builder().localPoW(new PearlDiverLocalPoW())
        .protocol(this.node.getProtocol())
        .host(this.node.getHost())
        .port(Integer.toString(this.node.getPort()))
        .build();
  }

  public static void main(String[] args) throws Exception {
    Configuration config = new Configuration();
    JCommander.newBuilder()
        .addObject(config)
        .build()
        .parse(args);

    Coordinator coo = new Coordinator(config);
    coo.setup();
    coo.start();
  }

  protected int getNextDepth(int depth, long lastTimestamp) {
    long now = System.currentTimeMillis();
    int nextDepth;

    log.info("Timestamp delta: " + ((now - lastTimestamp)));

    if ((now - lastTimestamp) > ((int) (config.depthScale * Long.valueOf(MILESTONE_TICK).floatValue()))) {
      nextDepth = depth * 2 / 3;
    } else {
      nextDepth = depth * 4 / 3;
    }

    if (nextDepth < 3) {
      nextDepth = 3;
    } else if (nextDepth > 1000) {
      nextDepth = 1000;
    }

    return nextDepth;
  }

  protected boolean nodeIsSolid(GetNodeInfoResponse nodeInfo) {
    if (nodeInfo.getLatestSolidSubtangleMilestoneIndex() != nodeInfo.getLatestMilestoneIndex())
      return false;

    if (!config.inception && (nodeInfo.getLatestSolidSubtangleMilestoneIndex() != latestMilestone))
      return false;

    if (nodeInfo.getLatestMilestone().equals(MilestoneDatabase.EMPTY_HASH) || nodeInfo.getLatestSolidSubtangleMilestone().equals(MilestoneDatabase.EMPTY_HASH))
      return false;

    return true;
  }

  public void setup() {
    log.info("Setup");
    GetNodeInfoResponse nodeInfoResponse = api.getNodeInfo();

    if (config.bootstrap) {
      log.info("Bootstrapping.");
      if (!nodeInfoResponse.getLatestSolidSubtangleMilestone().equals(MilestoneDatabase.EMPTY_HASH) || !nodeInfoResponse.getLatestMilestone().equals(MilestoneDatabase.EMPTY_HASH)) {
        throw new RuntimeException("Network already bootstrapped");
      }
    }

    if (config.index != null) {
      latestMilestone = config.index;
    } else {
      // = 0 if bootstrap
      latestMilestone = nodeInfoResponse.getLatestMilestoneIndex();
    }

    log.info("Starting index from: " + latestMilestone);
    if (nodeInfoResponse.getLatestMilestoneIndex() > latestMilestone && !config.inception) {
      throw new RuntimeException("Provided index is lower than latest seen milestone.");
    }

    MILESTONE_TICK = config.tick;
    if (MILESTONE_TICK <= 0) {
      throw new IllegalArgumentException("MILESTONE_TICK must be > 0");
    }
    log.info("Setting milestone tick rate to: " + MILESTONE_TICK);


    DEPTH = config.depth;
    if (DEPTH <= 0) {
      throw new IllegalArgumentException("DEPTH must be > 0");
    }
    log.info("Setting initial depth to: " + DEPTH);
  }

  public void start() throws ArgumentException, InterruptedException {
    boolean bootstrap = config.bootstrap;

    while (true) {
      String trunk, branch;
      int nextDepth;
      GetNodeInfoResponse nodeInfoResponse = api.getNodeInfo();

      if (!nodeIsSolid(nodeInfoResponse) && !bootstrap) {
        log.warning("Node not solid.");
        Thread.sleep(config.unsolidDelay);
        continue;
      }

      // Node is solid.
      if (bootstrap) {
        log.info("Bootstrapping network.");
        trunk = MilestoneDatabase.EMPTY_HASH;
        branch = MilestoneDatabase.EMPTY_HASH;
        bootstrap = false;
      } else {
        // As it's solid,
        // GetTransactionsToApprove will return tips referencing latest milestone.
        GetTransactionsToApproveResponse txToApprove = api.getTransactionsToApprove(DEPTH);
        trunk = txToApprove.getTrunkTransaction();
        branch = txToApprove.getBranchTransaction();
      }

      latestMilestone++;

      log.info("Issuing milestone: " + latestMilestone);
      List<Transaction> txs = db.createMilestone(trunk, branch, latestMilestone, config.MWM);


      if (config.broadcast) {
        for (Transaction tx : txs) {
          api.broadcastAndStore(tx.toTrytes());
        }
        log.info("Broadcasted milestone.");
      }

      log.info("Emitted milestone: " + latestMilestone);
      log.info("Trunk: " + trunk + " Branch: " + branch);


      nextDepth = getNextDepth(DEPTH, latestMilestoneTime);
      log.info("Depth: " + DEPTH + " -> " + nextDepth);

      DEPTH = nextDepth;
      latestMilestoneTime = System.currentTimeMillis();

      Thread.sleep(MILESTONE_TICK);
    }
  }
}
