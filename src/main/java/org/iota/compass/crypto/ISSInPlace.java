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

import java.util.Arrays;

/**
 * (c) 2016 Come-from-Beyond
 */
public class ISSInPlace {

  public static final int NUMBER_OF_FRAGMENT_CHUNKS = 27;
  public static final int FRAGMENT_LENGTH = JCurl.HASH_LENGTH * NUMBER_OF_FRAGMENT_CHUNKS;
  public static final int TRYTE_WIDTH = 3;
  private static final int NUMBER_OF_SECURITY_LEVELS = 3;
  public static final int NORMALIZED_FRAGMENT_LENGTH = JCurl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS;
  private static final int MIN_TRIT_VALUE = -1, MAX_TRIT_VALUE = 1;
  private static final int MIN_TRYTE_VALUE = -13, MAX_TRYTE_VALUE = 13;

  public static void subseed(SpongeFactory.Mode mode, int[] subseed, long index) {

    if (index < 0) {
      throw new RuntimeException("Invalid subseed index: " + index);
    }

    if (subseed.length != JCurl.HASH_LENGTH) {
      throw new IllegalArgumentException("Subseed array is not of HASH_LENGTH");
    }

    while (index-- > 0) {
      for (int i = 0; i < subseed.length; i++) {

        if (++subseed[i] > MAX_TRIT_VALUE) {
          subseed[i] = MIN_TRIT_VALUE;
        } else {
          break;
        }
      }
    }

    final ICurl hash = SpongeFactory.create(mode);
    hash.absorb(subseed, 0, subseed.length);
    hash.squeeze(subseed, 0, subseed.length);
  }

  public static void key(SpongeFactory.Mode mode, final int[] subseed, int[] key) {

    if (subseed.length != JCurl.HASH_LENGTH) {
      throw new RuntimeException("Invalid subseed length: " + subseed.length);
    }

    if ((key.length % FRAGMENT_LENGTH) != 0) {
      throw new IllegalArgumentException("key length must be multiple of fragment length");
    }

    int numberOfFragments = key.length / FRAGMENT_LENGTH;

    if (numberOfFragments <= 0) {
      throw new RuntimeException("Invalid number of key fragments: " + numberOfFragments);
    }

    final ICurl hash = SpongeFactory.create(mode);
    hash.absorb(subseed, 0, subseed.length);
    hash.squeeze(key, 0, key.length);
  }

  public static void digests(SpongeFactory.Mode mode, final int[] key, int[] digests) {

    if (key.length == 0 || key.length % FRAGMENT_LENGTH != 0) {
      throw new RuntimeException("Invalid key length: " + key.length);
    }

    if (digests.length != (key.length / FRAGMENT_LENGTH * JCurl.HASH_LENGTH)) {
      throw new IllegalArgumentException("Invalid digests length");
    }

    final ICurl hash = SpongeFactory.create(mode);

    for (int i = 0; i < key.length / FRAGMENT_LENGTH; i++) {

      final int[] buffer = Arrays.copyOfRange(key, i * FRAGMENT_LENGTH, (i + 1) * FRAGMENT_LENGTH);
      for (int j = 0; j < NUMBER_OF_FRAGMENT_CHUNKS; j++) {

        for (int k = MAX_TRYTE_VALUE - MIN_TRYTE_VALUE; k-- > 0; ) {
          hash.reset();
          hash.absorb(buffer, j * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH);
          hash.squeeze(buffer, j * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH);
        }
      }
      hash.reset();
      hash.absorb(buffer, 0, buffer.length);
      hash.squeeze(digests, i * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH);
    }
  }

  public static void address(SpongeFactory.Mode mode, final int[] digests, int[] address) {

    if (digests.length == 0 || digests.length % JCurl.HASH_LENGTH != 0) {
      throw new RuntimeException("Invalid digests length: " + digests.length);
    }

    if (address.length != JCurl.HASH_LENGTH) {
      throw new IllegalArgumentException("Invalid address length");
    }

    final ICurl hash = SpongeFactory.create(mode);
    hash.absorb(digests, 0, digests.length);
    hash.squeeze(address, 0, address.length);
  }
}
