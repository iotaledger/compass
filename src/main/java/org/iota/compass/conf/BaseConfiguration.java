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
import jota.pow.SpongeFactory;
import org.iota.compass.SignatureSourceType;

public class BaseConfiguration {
  @Parameter(names = "-layers", description = "Path to folder containing Merkle Tree layers", required = true)
  public String layersPath;

  @Parameter(names = "-host", description = "URL for IRI host", required = true)
  public String host;

  @Parameter(names = "-mwm", description = "Minimum Weight Magnitude", required = true)
  public int MWM = 9;

  @Parameter(names = "-broadcast", description = "Should Coordinator really broadcast milestones?")
  public boolean broadcast = false;

  @Parameter(names = "-powMode", description = "Sponge mode to use for Proof of Work (one of CURLP81, KERL)", required = true,
      converter = SpongeModeConverter.class, validateValueWith = {POWModeValidator.class})
  public SpongeFactory.Mode powMode = SpongeFactory.Mode.CURLP81;

  @Parameter(names = "-signatureSource", description = "Signature source type (can be 'inmemory' or 'remote')", converter = SignatureSourceTypeConverter.class)
  public SignatureSourceType signatureSource = SignatureSourceType.INMEMORY;
}
