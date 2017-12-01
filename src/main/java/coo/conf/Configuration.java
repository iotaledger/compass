package coo.conf;

import com.beust.jcommander.Parameter;

public class Configuration extends BaseConfiguration {
  @Parameter(names = "-bootstrap", description = "Bootstrap network")
  public boolean bootstrap = false;

  @Parameter(names = "-tick", description = "Milestone tick in milliseconds", required = true)
  public int tick = 15000;

  @Parameter(names = "-depth", description = "Starting depth")
  public int depth = 3;

  @Parameter(names = "-depthScale", description = "Time scale factor for depth decrease")
  public float depthScale = 1.01f;

  @Parameter(names = "-unsolidDelay", description = "Delay if node is not solid in milliseconds")
  public int unsolidDelay = 5000;

  @Parameter(names = "-inception", description = "Only use this if you know what you're doing.")
  public boolean inception = false;

  @Parameter(names = "-index", description = "Starting milestone index (inclusive)")
  public Integer index;
}
