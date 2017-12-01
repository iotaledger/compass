package coo.conf;

import com.beust.jcommander.Parameter;

public class ShadowingConfiguration extends BaseConfiguration {
  @Parameter(names = "-milestonesCSV", description = "csv (index, tail) of old milestones")
  public String milestonesCSV;

  @Parameter(names = "-oldRoot", description = "Old milestone address")
  public String oldRoot;

  @Parameter(names = "-oldMinIndex", description = "Minimum old milestone index (inclusive)")
  public long oldMinIndex = 0;

  @Parameter(names = "-oldMaxIndex", description = "Maximum old milestone index (inclusive)")
  public long oldMaxIndex = Long.MAX_VALUE;

  @Parameter(names = "-index", description = "Starting milestone index (inclusive)", required = true)
  public Integer index;
}
