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

import jota.IotaLocalPoW;
import jota.pow.ICurl;
import jota.pow.SpongeFactory;
import jota.utils.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A simple man's naive and single-threaded implementation of Kerl-based Proof-of-Work
 */
public class KerlPoW implements IotaLocalPoW {
  private static final Logger log = LoggerFactory.getLogger("KerlPoW");

  public final static int NONCE_START_TRIT = 7938;
  public final static int NONCE_LENGTH_TRIT = 81;
  public final static int NONCE_START_TRYTE = NONCE_START_TRIT / 3;
  public final static int NONCE_LENGTH_TRYTE = NONCE_LENGTH_TRIT / 3;

  private KerlPoWSettings settings;

  public KerlPoW() {
    this(new KerlPoWSettings());
  }

  public KerlPoW(KerlPoWSettings settings) {
    this.settings = settings;
    if (settings.numberOfThreads <= 0) {
      int available = Runtime.getRuntime().availableProcessors();
      settings.numberOfThreads = Math.max(1, Math.floorDiv(available * 8, 10));
    }

    // TODO (th0br0): fix PoW offsetting so we can use multiple threads
    settings.numberOfThreads = 1;
  }

  @Override
  public String performPoW(String trytes, int minWeightMagnitude) {
    final ExecutorService executorService = Executors.newFixedThreadPool(settings.numberOfThreads);
    final AtomicBoolean resultFound = new AtomicBoolean(false);
    final List<Searcher> searchers = IntStream.range(0, settings.numberOfThreads)
        .mapToObj((idx) -> new Searcher(trytes, resultFound, minWeightMagnitude))
        .collect(Collectors.toList());
    final List<Future<String>> searcherFutures = searchers.stream()
        .map(executorService::submit)
        .collect(Collectors.toList());

    executorService.shutdown();
    try {
      executorService.awaitTermination(10, TimeUnit.MINUTES);

      for (Future<String> f : searcherFutures) {
        if (f.isDone() && f.get() != null) {
          return trytes.substring(0, NONCE_START_TRYTE) + f.get();
        }
      }
    } catch (ExecutionException | InterruptedException e) {
      log.error("failed to calculate PoW with MWM: {} , trytes: {}", trytes, minWeightMagnitude, e);
      return null;
    }

    return null;
  }

  private static class KerlPoWSettings {
    private int numberOfThreads = 1;
  }

  class Searcher implements Callable<String> {

    private final AtomicBoolean resultFound;
    private final int targetZeros;

    private int[] trits;
    private int[] hashTrits = new int[243];

    public Searcher(String inputTrytes, AtomicBoolean resultFound, int targetZeros) {
      this.resultFound = resultFound;
      this.trits = Converter.trits(inputTrytes);
      this.targetZeros = targetZeros;
    }

    private boolean shouldAbort() {
      return resultFound.get();
    }

    private void increment(int[] trits, int offset, int size) {
      for (int i = offset; i < (offset + size) && ++trits[i] > 1; ++i) {
        trits[i] = -1;
      }
    }

    private int trailingZeros(int[] trits) {
      int count = 0;
      for (int i = trits.length - 1; i >= 0 && trits[i] == 0; i--) {
        count++;
      }

      return count;
    }

    private void search() {
      ICurl sponge = SpongeFactory.create(SpongeFactory.Mode.KERL);
      increment(trits, NONCE_START_TRIT, NONCE_LENGTH_TRIT);

      sponge.absorb(trits);
      sponge.squeeze(hashTrits);
    }

    @Override
    public String call() {
      String result = null;
      while (!shouldAbort()) {
        search();

        if (trailingZeros(hashTrits) >= targetZeros) {
          result = Converter.trytes(trits, NONCE_START_TRIT, NONCE_LENGTH_TRIT);
          resultFound.set(true);
          break;
        }
      }

      return result;
    }
  }
}
