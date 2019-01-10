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

package org.iota.compass.crypto;

import jota.pow.ICurl;
import jota.pow.JCurl;
import jota.pow.SpongeFactory;
import jota.utils.Converter;

/**
 *
 */
public class Hasher {
  /**
   * Hashes a provided Tryte string using the given method
   *
   * @param mode the sponge method to use
   * @param trytes
   * @return 81 tryte hash
   */
  public static String hashTrytes(SpongeFactory.Mode mode, String trytes) {
    return Converter.trytes(hashTrytesToTrits(mode, trytes));
  }

  public static int[] hashTrytesToTrits(SpongeFactory.Mode mode, String trytes) {
    int[] hash = new int[JCurl.HASH_LENGTH];

    ICurl sponge = SpongeFactory.create(mode);
    sponge.absorb(Converter.trits(trytes));
    sponge.squeeze(hash);

    return hash;
  }
}
