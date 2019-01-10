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

package org.iota.compass;

import java.security.SecureRandom;
import java.util.Random;

public class TestUtil {

  public static final String ALPHABET = "9ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final int randomnessSeed = 1;
  public static final Random random = new SecureRandom();

  {
    //for deterministic testing
    random.setSeed(randomnessSeed);
  }

  public static String nextSeed() {
    return nextTrytes(81);
  }

  public static String nextTrytes(int count) {
    char[] buf = new char[count];

    for (int idx = 0; idx < buf.length; ++idx)
      buf[idx] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));

    return new String(buf);
  }
}
