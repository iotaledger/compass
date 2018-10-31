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

public class ISS {

  public static final int NUMBER_OF_FRAGMENT_CHUNKS = 27;
  public static final int FRAGMENT_LENGTH = JCurl.HASH_LENGTH * NUMBER_OF_FRAGMENT_CHUNKS;
  public static final int TRYTE_WIDTH = 3;
  private static final int NUMBER_OF_SECURITY_LEVELS = 3;
  public static final int NORMALIZED_FRAGMENT_LENGTH = JCurl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS;
  private static final int MIN_TRIT_VALUE = -1, MAX_TRIT_VALUE = 1;
  private static final int MIN_TRYTE_VALUE = -13, MAX_TRYTE_VALUE = 13;

  public static int[] subseed(SpongeFactory.Mode mode, final int[] seed, long index) {

    if (index < 0) {
      throw new RuntimeException("Invalid subseed index: " + index);
    }

    final int[] subseedPreimage = Arrays.copyOf(seed, seed.length);

    while (index-- > 0) {

      for (int i = 0; i < subseedPreimage.length; i++) {

        if (++subseedPreimage[i] > MAX_TRIT_VALUE) {
          subseedPreimage[i] = MIN_TRIT_VALUE;
        } else {
          break;
        }
      }
    }

    final int[] subseed = new int[JCurl.HASH_LENGTH];

    final ICurl hash = SpongeFactory.create(mode);
    hash.absorb(subseedPreimage, 0, subseedPreimage.length);
    hash.squeeze(subseed, 0, subseed.length);
    return subseed;
  }

  public static int[] key(SpongeFactory.Mode mode, final int[] subseed, final int numberOfFragments) {

    if (subseed.length != JCurl.HASH_LENGTH) {
      throw new RuntimeException("Invalid subseed length: " + subseed.length);
    }
    if (numberOfFragments <= 0) {
      throw new RuntimeException("Invalid number of key fragments: " + numberOfFragments);
    }

    final int[] key = new int[FRAGMENT_LENGTH * numberOfFragments];

    final ICurl hash = SpongeFactory.create(mode);
    hash.absorb(subseed, 0, subseed.length);
    hash.squeeze(key, 0, key.length);
    return key;
  }

  public static int[] digests(SpongeFactory.Mode mode, final int[] key) {

    if (key.length == 0 || key.length % FRAGMENT_LENGTH != 0) {
      throw new RuntimeException("Invalid key length: " + key.length);
    }

    final int[] digests = new int[key.length / FRAGMENT_LENGTH * JCurl.HASH_LENGTH];
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

    return digests;
  }

  public static int[] address(SpongeFactory.Mode mode, final int[] digests) {

    if (digests.length == 0 || digests.length % JCurl.HASH_LENGTH != 0) {
      throw new RuntimeException("Invalid digests length: " + digests.length);
    }

    final int[] address = new int[JCurl.HASH_LENGTH];

    final ICurl hash = SpongeFactory.create(mode);
    hash.absorb(digests, 0, digests.length);
    hash.squeeze(address, 0, address.length);

    return address;
  }

  public static int[] normalizedBundle(final int[] bundle) {

    if (bundle.length != JCurl.HASH_LENGTH) {
      throw new RuntimeException("Invalid bundleValidator length: " + bundle.length);
    }

    final int[] normalizedBundle = new int[JCurl.HASH_LENGTH / TRYTE_WIDTH];

    for (int i = 0; i < NUMBER_OF_SECURITY_LEVELS; i++) {

      int sum = 0;
      for (int j = i * (JCurl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j < (i + 1) * (JCurl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j++) {

        normalizedBundle[j] = bundle[j * TRYTE_WIDTH] + bundle[j * TRYTE_WIDTH + 1] * 3 + bundle[j * TRYTE_WIDTH + 2] * 9;
        sum += normalizedBundle[j];
      }
      if (sum > 0) {

        while (sum-- > 0) {

          for (int j = i * (JCurl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j < (i + 1) * (JCurl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j++) {

            if (normalizedBundle[j] > MIN_TRYTE_VALUE) {
              normalizedBundle[j]--;
              break;
            }
          }
        }

      } else {

        while (sum++ < 0) {

          for (int j = i * (JCurl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j < (i + 1) * (JCurl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j++) {

            if (normalizedBundle[j] < MAX_TRYTE_VALUE) {
              normalizedBundle[j]++;
              break;
            }
          }
        }
      }
    }

    return normalizedBundle;
  }

  public static int[] signatureFragment(SpongeFactory.Mode mode, final int[] normalizedBundleFragment, final int[] keyFragment) {

    if (normalizedBundleFragment.length != NORMALIZED_FRAGMENT_LENGTH) {
      throw new RuntimeException("Invalid normalized bundleValidator fragment length: " + normalizedBundleFragment.length);
    }
    if (keyFragment.length != FRAGMENT_LENGTH) {
      throw new RuntimeException("Invalid key fragment length: " + keyFragment.length);
    }

    final int[] signatureFragment = Arrays.copyOf(keyFragment, keyFragment.length);
    final ICurl hash = SpongeFactory.create(mode);

    for (int j = 0; j < NUMBER_OF_FRAGMENT_CHUNKS; j++) {

      for (int k = MAX_TRYTE_VALUE - normalizedBundleFragment[j]; k-- > 0; ) {
        hash.reset();
        hash.absorb(signatureFragment, j * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH);
        hash.squeeze(signatureFragment, j * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH);
      }
    }

    return signatureFragment;
  }

  public static int[] digest(SpongeFactory.Mode mode, final int[] normalizedBundleFragment, final int[] signatureFragment) {

    if (normalizedBundleFragment.length != JCurl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS) {
      throw new RuntimeException("Invalid normalized bundleValidator fragment length: " + normalizedBundleFragment.length);
    }
    if (signatureFragment.length != FRAGMENT_LENGTH) {
      throw new RuntimeException("Invalid signature fragment length: " + signatureFragment.length);
    }

    final int[] digest = new int[JCurl.HASH_LENGTH];
    final int[] buffer = Arrays.copyOf(signatureFragment, FRAGMENT_LENGTH);
    final ICurl hash = SpongeFactory.create(mode);
    for (int j = 0; j < NUMBER_OF_FRAGMENT_CHUNKS; j++) {

      for (int k = normalizedBundleFragment[j] - MIN_TRYTE_VALUE; k-- > 0; ) {
        hash.reset();
        hash.absorb(buffer, j * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH);
        hash.squeeze(buffer, j * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH);
      }
    }
    hash.reset();
    hash.absorb(buffer, 0, buffer.length);
    hash.squeeze(digest, 0, digest.length);

    return digest;
  }

  public static int[] getMerkleRoot(SpongeFactory.Mode mode, final int[] inHash, int[] trits, int offset, final int indexIn, int size) {
    int index = indexIn;
    int[] hash = inHash.clone();
    final ICurl curl = SpongeFactory.create(mode);
    for (int i = 0; i < size; i++) {
      curl.reset();
      if ((index & 1) == 0) {
        curl.absorb(hash, 0, hash.length);
        curl.absorb(trits, offset + i * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH);
      } else {
        curl.absorb(trits, offset + i * JCurl.HASH_LENGTH, JCurl.HASH_LENGTH);
        curl.absorb(hash, 0, hash.length);
      }
      curl.squeeze(hash, 0, hash.length);

      index >>= 1;
    }
    if (index != 0) {
      return new int[JCurl.HASH_LENGTH];
    }
    return hash;
  }
}
