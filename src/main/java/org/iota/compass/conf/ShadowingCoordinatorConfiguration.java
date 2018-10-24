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

package org.iota.compass.conf;

import com.beust.jcommander.Parameter;
import org.iota.compass.MilestoneSource;

public class ShadowingCoordinatorConfiguration extends BaseConfiguration {
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
  public String initialTrunk = MilestoneSource.EMPTY_HASH;

  @Parameter(names = "-depth", description = "depth from which to start the random walk")
  public Integer depth = 3;
}
