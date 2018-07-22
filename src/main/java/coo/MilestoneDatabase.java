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

package coo;

import cfb.pearldiver.PearlDiverLocalPoW;
import com.google.common.base.Strings;
import coo.crypto.Hasher;
import coo.crypto.ISS;
import jota.IotaLocalPoW;
import jota.model.Transaction;
import jota.pow.ICurl;
import jota.pow.JCurl;
import jota.pow.SpongeFactory;
import jota.utils.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MilestoneDatabase extends MilestoneSource {

  private final Logger log = LoggerFactory.getLogger("MilestoneDatabase");

  private final SpongeFactory.Mode SIGMODE;
  private final SpongeFactory.Mode POWMODE;
  private final String SEED;
  private final String ROOT;
  private final List<List<String>> layers;

  public MilestoneDatabase(SpongeFactory.Mode powMode, SpongeFactory.Mode sigMode, String path, String seed) throws IOException {
    this(powMode, sigMode, loadLayers(path), seed);
  }

  public MilestoneDatabase(SpongeFactory.Mode powMode, SpongeFactory.Mode sigMode, List<List<String>> layers, String seed) throws IOException {
    this.layers = layers;

    SEED = seed;
    ROOT = layers.get(0).get(0);
    SIGMODE = sigMode;
    POWMODE = powMode;
  }

  private static List<List<String>> loadLayers(String path) throws IOException {
    Map<Integer, List<String>> result = new ConcurrentHashMap<>();

    StreamSupport.stream(Files.newDirectoryStream(Paths.get(path)).spliterator(), true)
        .forEach((Path p) -> {
          int idx = Integer.parseInt(p.toString().split("\\.")[1]);
          try {
            result.put(idx, Files.readAllLines(p));
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

    return result.entrySet().stream().map((e) -> e.getValue()).collect(Collectors.toList());
  }

  /**
   * Computes a bundle hash given two transactions in Tryte format
   *
   * @param tx0
   * @param tx1
   * @return
   */
  private static String generateBundleHash(String tx0, String tx1) {
    final int OFFSET = (ISS.FRAGMENT_LENGTH / ISS.TRYTE_WIDTH);

    /* address + timestamp + value + tag + currentIndex + lastIndex */
    final int LENGTH = (243 + 81 + 81 + 27 + 27 + 27) / ISS.TRYTE_WIDTH;

    int[] t0 = Converter.trits(tx0.substring(OFFSET, OFFSET + LENGTH));
    int[] t1 = Converter.trits(tx1.substring(OFFSET, OFFSET + LENGTH));

    // TODO(th0br0): make bundle sponge mode a paramter
    ICurl sponge = SpongeFactory.create(SpongeFactory.Mode.KERL);
    sponge.absorb(t0);
    sponge.absorb(t1);
    sponge.squeeze(t0, 0, JCurl.HASH_LENGTH);

    return Converter.trytes(t0, 0, JCurl.HASH_LENGTH);
  }

  /**
   * Calculates a list of siblings
   *
   * @param leafIdx
   * @param layers
   * @return
   */
  private static List<String> siblings(int leafIdx, List<List<String>> layers) {
    List<String> siblings = new ArrayList<>();

    int curLayer = layers.size() - 1;

    while (curLayer > 0) {
      List<String> layer = layers.get(curLayer);
      if ((leafIdx & 1) == 1) {
        // odd
        siblings.add(layer.get(--leafIdx));
      } else {
        siblings.add(layer.get(leafIdx + 1));
      }

      leafIdx >>= 1;
      curLayer--;
    }

    return siblings;
  }

  @Override
  public SpongeFactory.Mode getSignatureMode() {
    return SIGMODE;
  }

  @Override
  public SpongeFactory.Mode getPoWMode() {
    return POWMODE;
  }

  @Override
  public String getRoot() {
    return ROOT;
  }

  private IotaLocalPoW getPoWProvider() {
    if (POWMODE == SpongeFactory.Mode.KERL) {
      return new KerlPoW();
    } else {
      return new PearlDiverLocalPoW();
    }
  }

  private String getTagForIndex(int index) {
    String tag;
    int[] trits = new int[15];
    for (int i = 0; i < index; i++) {
      Converter.increment(trits, trits.length);
    }
    tag = Converter.trytes(trits);
    return Strings.padEnd(tag, 27, '9');
  }

  @Override
  public List<Transaction> createMilestone(String trunk, String branch, int index, int mwm) {
    List<Transaction> txs = new ArrayList<>();

    IotaLocalPoW pow = getPoWProvider();

    // Get the siblings in the current merkle tree
    List<String> leafSiblings = siblings(index, layers);
    String siblingsTrytes = leafSiblings.stream().collect(Collectors.joining(""));
    siblingsTrytes = Strings.padEnd(siblingsTrytes, ISS.FRAGMENT_LENGTH / ISS.TRYTE_WIDTH, '9');

    // A milestone consists of two transactions.
    // The second transaction (index = 1) contains the siblings for the merkle tree.
    Transaction tx1 = new Transaction();
    tx1.setSignatureFragments(siblingsTrytes);
    tx1.setAddress(EMPTY_HASH);
    tx1.setCurrentIndex(1);
    tx1.setLastIndex(1);
    tx1.setTimestamp(System.currentTimeMillis() / 1000);
    tx1.setObsoleteTag(EMPTY_TAG);
    tx1.setValue(0);
    tx1.setBundle(EMPTY_HASH);
    tx1.setTrunkTransaction(trunk);
    tx1.setBranchTransaction(branch);
    tx1.setTag(EMPTY_TAG);
    tx1.setNonce(EMPTY_TAG);

    // The first transaction contains a signature that signs the siblings and thereby ensures the integrity.
    Transaction tx0 = new Transaction();
    tx0.setSignatureFragments(MilestoneSource.EMPTY_MSG);
    tx0.setAddress(ROOT);
    tx0.setCurrentIndex(0);
    tx0.setLastIndex(1);
    tx0.setTimestamp(System.currentTimeMillis() / 1000);
    tx0.setObsoleteTag(getTagForIndex(index));
    tx0.setValue(0);
    tx0.setBundle(EMPTY_HASH);
    tx0.setTrunkTransaction(EMPTY_HASH);
    tx0.setBranchTransaction(trunk);
    tx0.setTag(tx0.getObsoleteTag());
    tx0.setNonce(MilestoneSource.EMPTY_TAG);

    // We support separate signature methods
    if (SIGMODE == SpongeFactory.Mode.KERL) {
      /*
      In the case that the signature is created using KERL, we need to ensure that there exists no 'M'(=13) in the
      normalized fragment that we're signing.
       */
      boolean hashContainsM;
      int attempts = 0;
      ICurl sponge = SpongeFactory.create(POWMODE);
      int[] hashTrits = new int[JCurl.HASH_LENGTH];

      do {
        String bundleHash = generateBundleHash(tx0.toTrytes(), tx1.toTrytes());

        tx0.setBundle(bundleHash);
        tx1.setBundle(bundleHash);

        attempts++;

        tx1 = new Transaction(pow.performPoW(tx1.toTrytes(), mwm));

        sponge.reset();
        sponge.absorb(Converter.trits(tx1.toTrytes()));
        sponge.squeeze(hashTrits);

        int[] normHash = ISS.normalizedBundle(hashTrits);

        hashContainsM = false;
        for (int i = 0; i < ISS.NUMBER_OF_FRAGMENT_CHUNKS; i++) {
          if (normHash[i] == 13) {
            hashContainsM = true;

            int[] tagTrits = Converter.trits(tx1.getObsoleteTag());
            Converter.increment(tagTrits, tagTrits.length);
            tx1.setObsoleteTag(Converter.trytes(tagTrits));
            break;
          }
        }
      } while (hashContainsM);
      log.info("KERL milestone generation took attempts: " + attempts);
    } else {
      String bundleHash = generateBundleHash(tx0.toTrytes(), tx1.toTrytes());
      tx0.setBundle(bundleHash);
      tx1.setBundle(bundleHash);
    }

    tx1 = new Transaction(pow.performPoW(tx1.toTrytes(), mwm));
    String tx1Hash = Hasher.hashTrytes(POWMODE, tx1.toTrytes());

    tx0.setSignatureFragments(createSignature(SIGMODE, index, tx1Hash));
    tx0.setTrunkTransaction(tx1Hash);
    tx0 = new Transaction(pow.performPoW(tx0.toTrytes(), mwm));

    txs.add(tx0);
    txs.add(tx1);
    return txs;
  }

  /**
   * @param mode       sponge mode to use for signature creation
   * @param index      key / tree leaf index to generate signature for
   * @param hashToSign the hash to be signed
   * @return
   */
  private String createSignature(SpongeFactory.Mode mode, int index, String hashToSign) {
    int[] hashTrits = Converter.trits(hashToSign);
    int[] normalizedBundle = Arrays.copyOf(ISS.normalizedBundle(hashTrits), ISS.NUMBER_OF_FRAGMENT_CHUNKS);

    int[] subseed = ISS.subseed(mode, Converter.trits(SEED), index);
    int[] key = ISS.key(mode, subseed, 1);
    int[] signatureFragment = ISS.signatureFragment(mode, normalizedBundle, key);

    return Converter.trytes(signatureFragment);
  }
}
