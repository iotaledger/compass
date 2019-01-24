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

import java.util.List;
import java.util.ArrayList;

public class CoordinatorConfiguration extends BaseConfiguration {
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

  @Parameter(names = "-validator", description = "Validator nodes to use")
  public List<String> validators = new ArrayList<>();

}
