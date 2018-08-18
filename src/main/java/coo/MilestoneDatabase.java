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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class MilestoneDatabase extends MilestoneSource {

  private final Logger log = LoggerFactory.getLogger("MilestoneDatabase");

  private final SpongeFactory.Mode SIGMODE;
  private final SpongeFactory.Mode POWMODE;
  private final String SEED;
  private final String ROOT;
  private final List<List<String>> layers;
  private final int SECURITY;

  public MilestoneDatabase(SpongeFactory.Mode powMode, SpongeFactory.Mode sigMode, String path, String seed, int security) throws IOException {
    this(powMode, sigMode, loadLayers(path), seed, security);
  }

  public MilestoneDatabase(SpongeFactory.Mode powMode, SpongeFactory.Mode sigMode, List<List<String>> layers, String seed, int security) throws IOException {
    ROOT = layers.get(0).get(0);
    this.layers = layers;
    SEED = seed;
    SECURITY = security;
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
    final int NONCE_OFFSET = 2673 /* tx length in trytes */ - 27 /* nonce length in trytes */;
    final int SIGNATURE_LENGTH = 27 * 81;

    IotaLocalPoW pow = getPoWProvider();

    // Get the siblings in the current merkle tree
    List<String> leafSiblings = siblings(index, layers);
    String siblingsTrytes = leafSiblings.stream().collect(Collectors.joining(""));
    siblingsTrytes = Strings.padEnd(siblingsTrytes, ISS.FRAGMENT_LENGTH / ISS.TRYTE_WIDTH, '9');

    final String tag = getTagForIndex(index);

    // A milestone consists of two transactions.
    // The last transaction (currentIndex == lastIndex) contains the siblings for the merkle tree.
    Transaction txSiblings = new Transaction();
    txSiblings.setSignatureFragments(siblingsTrytes);
    txSiblings.setAddress(EMPTY_HASH);
    txSiblings.setCurrentIndex(SECURITY);
    txSiblings.setLastIndex(SECURITY);
    txSiblings.setTimestamp(System.currentTimeMillis() / 1000);
    txSiblings.setObsoleteTag(EMPTY_TAG);
    txSiblings.setValue(0);
    txSiblings.setBundle(EMPTY_HASH);
    txSiblings.setTrunkTransaction(trunk);
    txSiblings.setBranchTransaction(branch);
    txSiblings.setTag(EMPTY_TAG);
    txSiblings.setNonce(EMPTY_TAG);

    // The other transactions contain a signature that signs the siblings and thereby ensures the integrity.
    List<Transaction> txs =
        IntStream.range(0, SECURITY).mapToObj(i -> {
          Transaction tx = new Transaction();
          tx.setSignatureFragments(Strings.repeat("9", 27 * 81));
          tx.setAddress(ROOT);
          tx.setCurrentIndex(i);
          tx.setLastIndex(SECURITY);
          tx.setTimestamp(System.currentTimeMillis() / 1000);
          tx.setObsoleteTag(tag);
          tx.setValue(0);
          tx.setBundle(EMPTY_HASH);
          tx.setTrunkTransaction(EMPTY_HASH);
          tx.setBranchTransaction(trunk);
          tx.setTag(tag);
          tx.setNonce(Strings.repeat("9", 27));
          return tx;
        }).collect(Collectors.toList());

    txs.add(txSiblings);

    String signedHash;

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
        String bundleHash = generateBundleHash(txs);
        txs.forEach(tx -> tx.setBundle(bundleHash));

        attempts++;

        txSiblings.setNonce(pow.performPoW(txSiblings.toTrytes(), mwm).substring(NONCE_OFFSET));

        sponge.reset();
        sponge.absorb(Converter.trits(txSiblings.toTrytes()));
        sponge.squeeze(hashTrits);

        int[] normHash = ISS.normalizedBundle(hashTrits);

        hashContainsM = false;
        for (int i = 0; i < ISS.NUMBER_OF_FRAGMENT_CHUNKS; i++) {
          if (normHash[i] == 13) {
            hashContainsM = true;

            int[] tagTrits = Converter.trits(txSiblings.getObsoleteTag());
            Converter.increment(tagTrits, tagTrits.length);
            txSiblings.setObsoleteTag(Converter.trytes(tagTrits));
            break;
          }
        }
      } while (hashContainsM);
      log.info("KERL milestone generation took attempts: " + attempts);

      signedHash = Converter.trytes(hashTrits);
    } else {
      String bundleHash = generateBundleHash(txs);
      txs.forEach(tx -> tx.setBundle(bundleHash));

      txSiblings.setNonce(pow.performPoW(txSiblings.toTrytes(), mwm).substring(NONCE_OFFSET));
      signedHash = Hasher.hashTrytes(POWMODE, txSiblings.toTrytes());
    }

    String signature = createSignature(SIGMODE, index, signedHash);

    Collections.reverse(txs);

    txs.stream().skip(1).forEach(tx -> {
      // Get hash of previous tx.
      String prevHash = Hasher.hashTrytes(POWMODE, txs.get(((int) (tx.getCurrentIndex() + 1))).toTrytes());

      String sigSub = signature.substring((int) (tx.getCurrentIndex() * SIGNATURE_LENGTH));
      if (sigSub.length() > SIGNATURE_LENGTH) {
        sigSub = sigSub.substring(0, SIGNATURE_LENGTH);
      }

      tx.setSignatureFragments(sigSub);
      tx.setTrunkTransaction(prevHash);
    });


    Collections.reverse(txs);

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
    int[] normalizedBundle = ISS.normalizedBundle(hashTrits);

    int[] subseed = ISS.subseed(mode, Converter.trits(SEED), index);
    int[] key = ISS.key(mode, subseed, SECURITY);

    String fragment = "";

    for (int i = 0; i < SECURITY; i++) {
      int[] curFrag = ISS.signatureFragment(mode,
          Arrays.copyOfRange(normalizedBundle, i * ISS.NUMBER_OF_FRAGMENT_CHUNKS, (i + 1) * ISS.NUMBER_OF_FRAGMENT_CHUNKS),
          Arrays.copyOfRange(key, i * ISS.FRAGMENT_LENGTH, (i + 1) * ISS.FRAGMENT_LENGTH));
      fragment += Converter.trytes(curFrag);
    }

    return fragment;
  }

  private String generateBundleHash(List<Transaction> txs) {
    final int OFFSET = (ISS.FRAGMENT_LENGTH / 3);
    final int LENGTH = (243 + 81 + 81 + 27 + 27 + 27) / 3;

    ICurl sponge = SpongeFactory.create(SpongeFactory.Mode.KERL);

    for (Transaction tx : txs) {
      sponge.absorb(Converter.trits(tx.toTrytes().substring(OFFSET, OFFSET + LENGTH)));
    }

    int[] bundleHashTrits = new int[243];
    sponge.squeeze(bundleHashTrits, 0, JCurl.HASH_LENGTH);

    return Converter.trytes(bundleHashTrits, 0, JCurl.HASH_LENGTH);
  }
}
