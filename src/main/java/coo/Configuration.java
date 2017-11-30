package coo;

import com.beust.jcommander.Parameter;

public class Configuration {
  @Parameter(names = "-layers", description = "Path to folder containing Merkle Tree layers", required=true)
  public String layersPath;

  @Parameter(names = "-bootstrap", description = "Bootstrap network")
  public boolean bootstrap = false;

  @Parameter(names = "-host", description = "URL for IRI host", required = true)
  public String host;

  @Parameter(names = "-mwm", description = "Minimum Weight Magnitude", required = true)
  public int MWM = 9;

  @Parameter(names = "-seed", description = "Seed", required = true)
  public String seed;

  @Parameter(names = "-index", description = "Starting milestone index")
  public Integer index;

  @Parameter(names="-depth", description="Starting depth")
  public int depth = 3;

  @Parameter(names = "-tick", description = "Milestone tick in milliseconds", required = true)
  public int tick = 15000;

  @Parameter(names = "-unsolidDelay", description = "Delay if node is not solid in milliseconds")
  public int unsolidDelay = 5000;

  @Parameter(names="-inception", description = "Only use this if you know what you're doing.")
  public boolean inception = false;

  @Parameter(names="-broadcast", description = "Should Coordinator really broadcast milestones?")
  public boolean broadcast = false;

  @Parameter(names="-depthScale", description = "Time scale factor for depth decrease")
  public float depthScale = 1.01f;
}
