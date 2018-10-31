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

public class InMemorySignatureSourceConfiguration {
  @Parameter(names = "-seed", description = "Seed", required = true)
  public String seed;

  @Parameter(names = "-sigMode", description = "Sponge mode to use for signature creation (one of CURLP27, CURLP81, KERL)",
      required = true, converter = SpongeModeConverter.class)
  public SpongeFactory.Mode sigMode = SpongeFactory.Mode.CURLP27;

  @Parameter(names = "-security", description = "Security level to use. Value must be in [1;3]")
  public Integer security = 1;
}
