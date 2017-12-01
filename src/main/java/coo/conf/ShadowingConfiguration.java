package coo.conf;

import com.beust.jcommander.Parameter;
import com.google.common.base.Strings;

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

  @Parameter(names = "-broadcastBatch", description = "Rate at which broadcasts are batched")
  public int broadcastBatch = 666;

  @Parameter(names = "-initialTrunk", description = "Initial trunk that is referenced")
  public String initialTrunk = Strings.repeat("9", 81);
}
