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

import com.google.common.base.Strings;
import org.iota.jota.IotaPoW;
import org.iota.jota.model.Bundle;
import org.iota.jota.model.Transaction;
import org.iota.jota.pow.ICurl;
import org.iota.jota.pow.SpongeFactory;
import org.iota.jota.pow.pearldiver.PearlDiverLocalPoW;
import org.iota.jota.utils.Converter;
import org.iota.compass.crypto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.iota.jota.pow.JCurl.HASH_LENGTH;

public class MilestoneDatabase extends MilestoneSource {

  private static final Logger log = LoggerFactory.getLogger(MilestoneDatabase.class);
  private static final int NONCE_OFFSET = 2673 /* tx length in trytes */ - 27 /* nonce length in trytes */;
  private static final int SIGNATURE_LENGTH = 27 * 81;
  private static final int OFFSET = (ISS.FRAGMENT_LENGTH / 3);
  private static final int LENGTH = (243 + 81 + 81 + 27 + 27 + 27) / 3;

  private final SpongeFactory.Mode powMode;
  private final URL powHost;
  private final SignatureSource signatureSource;
  private final String root;
  private final List<List<String>> layers;


  public MilestoneDatabase(SpongeFactory.Mode powMode, URL powHost, SignatureSource signatureSource, String path) throws IOException {
    this(powMode, powHost, signatureSource, loadLayers(path));
  }

  public MilestoneDatabase(SpongeFactory.Mode powMode, URL powHost, SignatureSource signatureSource, List<List<String>> layers) {
    root = layers.get(0).get(0);
    this.layers = layers;
    this.signatureSource = signatureSource;
    this.powMode = powMode;
    this.powHost = powHost;
  }

  private static List<String> readLines(Path p, int totalSize) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(p.toString()));
    List<String> result = new ArrayList<>(totalSize);
    String line;
    do {
        line = br.readLine();
        if (line != null) {
            result.add(line);
        }
    } while (line != null);

    return result;
  }

  private static List<List<String>> loadLayers(String path) throws IOException {
    Map<Integer, List<String>> result = new HashMap<>();

    for (Path p : Files.newDirectoryStream(Paths.get(path))) {
      int idx = Integer.parseInt(p.toString().split("\\.")[1]);
      int totalSize = 1 << idx;
      try {
        result.put(idx, readLines(p, totalSize));
      } catch (IOException e) {
        log.error("failed to load layers from: {}", path, e);
      }
    }

    return IntStream.range(0, result.size())
        .mapToObj(result::get)
        .peek(list -> Objects.requireNonNull(list, "Found a missing layer. please check: " + path))
        .collect(Collectors.toList());
  }

  /**
   * Calculates a list of siblings
   *
   * @param leafIdx index of leaf
   * @param layers the Merkle tree in layers structure
   * @return a list of siblings
   */
  private static List<String> siblings(int leafIdx, List<List<String>> layers) {
    List<String> siblings = new ArrayList<>(layers.size());

    int curLayer = layers.size() - 1;

    while (curLayer > 0) {
      List<String> layer = layers.get(curLayer);
      if ((leafIdx & 1) == 1) {
        // odd
        siblings.add(layer.get(leafIdx - 1));
      } else {
        siblings.add(layer.get(leafIdx + 1));
      }

      leafIdx /= 2;
      curLayer--;
    }

    return siblings;
  }

  @Override
  public SpongeFactory.Mode getPoWMode() {
    return powMode;
  }

  @Override
  public String getRoot() {
    return root;
  }

  private IotaPoW getPoWProvider() {
    if (powMode == SpongeFactory.Mode.KERL) {
      return new KerlPoW();
    } else {
      if (powHost != null) {
        return new RemoteCURLP81PoW(powHost);
      } else {
        return new PearlDiverLocalPoW();
      }
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

    IotaPoW pow = getPoWProvider();

    // Get the siblings in the current merkle tree
    List<String> leafSiblings = siblings(index, layers);
    String siblingsTrytes = String.join("", leafSiblings);
    String paddedSiblingsTrytes = Strings.padEnd(siblingsTrytes, ISS.FRAGMENT_LENGTH / ISS.TRYTE_WIDTH, '9');

    final String tag = getTagForIndex(index);

    // A milestone consists of two transactions.
    // The last transaction (currentIndex == lastIndex) contains the siblings for the merkle tree.
    Transaction txSiblings = new Transaction();
    txSiblings.setSignatureFragments(paddedSiblingsTrytes);
    txSiblings.setAddress(root);
    txSiblings.setCurrentIndex(signatureSource.getSecurity());
    txSiblings.setLastIndex(signatureSource.getSecurity());
    txSiblings.setTimestamp(System.currentTimeMillis() / 1000);
    txSiblings.setObsoleteTag(tag);
    txSiblings.setValue(0);
    txSiblings.setBundle(EMPTY_HASH);
    txSiblings.setTrunkTransaction(trunk);
    txSiblings.setBranchTransaction(branch);
    txSiblings.setTag(tag);
    txSiblings.setNonce(EMPTY_TAG);

    // The other transactions contain a signature that signs the siblings and thereby ensures the integrity.
    List<Transaction> txs =
        IntStream.range(0, signatureSource.getSecurity()).mapToObj(i -> {
          Transaction tx = new Transaction();
          tx.setSignatureFragments(Strings.repeat("9", 27 * 81));
          tx.setAddress(root);
          tx.setCurrentIndex(i);
          tx.setLastIndex(signatureSource.getSecurity());
          tx.setTimestamp(System.currentTimeMillis() / 1000);
          tx.setObsoleteTag(tag);
          tx.setValue(0);
          tx.setBundle(EMPTY_HASH);
          tx.setTrunkTransaction(EMPTY_HASH);
          tx.setBranchTransaction(trunk);
          tx.setTag(tag);
          tx.setNonce(EMPTY_TAG);
          return tx;
        }).collect(Collectors.toList());

    txs.add(txSiblings);

    Transaction tPoW;
    String hashToSign;

    //calculate the bundle hash (same for Curl & Kerl)
    String bundleHash = calculateBundleHash(txs);
    txs.forEach(tx -> tx.setBundle(bundleHash));

    txSiblings.setAttachmentTimestamp(System.currentTimeMillis());
    tPoW = new Transaction(pow.performPoW(txSiblings.toTrytes(), mwm));
    txSiblings.setAttachmentTimestamp(tPoW.getAttachmentTimestamp());
    txSiblings.setAttachmentTimestampLowerBound(tPoW.getAttachmentTimestampLowerBound());
    txSiblings.setAttachmentTimestampUpperBound(tPoW.getAttachmentTimestampUpperBound());
    txSiblings.setNonce(tPoW.getNonce());

    // We need to avoid the M bug we we are signing with KERL
    if (signatureSource.getSignatureMode() == SpongeFactory.Mode.KERL) {
      /*
      In the case that the signature is created using KERL, we need to ensure that there exists no 'M'(=13) in the
      normalized fragment that we're signing.
       */
      boolean hashContainsM;
      int attempts = 0;
      do {
        int[] hashTrits = Hasher.hashTrytesToTrits(powMode, txSiblings.toTrytes());
        int[] normHash = ISS.normalizedBundle(hashTrits);

        hashContainsM = Arrays.stream(normHash).limit(ISS.NUMBER_OF_FRAGMENT_CHUNKS * signatureSource.getSecurity()).anyMatch(elem -> elem == 13);
        if (hashContainsM) {
          txSiblings.setAttachmentTimestamp(System.currentTimeMillis());
          tPoW = new Transaction(pow.performPoW(txSiblings.toTrytes(), mwm));
          txSiblings.setAttachmentTimestamp(tPoW.getAttachmentTimestamp());
          txSiblings.setAttachmentTimestampLowerBound(tPoW.getAttachmentTimestampLowerBound());
          txSiblings.setAttachmentTimestampUpperBound(tPoW.getAttachmentTimestampUpperBound());
          txSiblings.setNonce(tPoW.getNonce());
        }
        attempts++;
      } while (hashContainsM);

      log.info("KERL milestone generation took {} attempts.", attempts);

    }

    hashToSign = Hasher.hashTrytes(powMode, txSiblings.toTrytes());
    String signature = signatureSource.getSignature(index, hashToSign);
    txSiblings.setHash(hashToSign);

    validateSignature(root, index, hashToSign, signature, siblingsTrytes);

    chainTransactionsFillSignatures(mwm, txs, signature);

    return txs;
  }

  private void validateSignature(String root, int index, String hashToSign, String signature, String siblingsTrytes) {
    int[] rootTrits = Converter.trits(root);
    int[] signatureTrits = Converter.trits(signature);
    int[] siblingsTrits = Converter.trits(siblingsTrytes);
    SpongeFactory.Mode mode = signatureSource.getSignatureMode();

    int[][] normalizedBundleFragments = new int[3][27];


    {
      int[] normalizedBundleHash = new Bundle().normalizedBundle(hashToSign);

      // Split hash into 3 fragments
      for (int i = 0; i < 3; i++) {
        normalizedBundleFragments[i] = Arrays.copyOfRange(normalizedBundleHash, i * 27, (i + 1) * 27);
      }
    }

    // Get digests
    int[] digests = new int[signatureSource.getSecurity() * HASH_LENGTH];
    for (int i = 0; i < signatureSource.getSecurity(); i++) {
      int[] digestBuffer = ISS.digest(mode, normalizedBundleFragments[i % 3], Arrays.copyOfRange(signatureTrits, i * ISS.FRAGMENT_LENGTH, (i + 1) * ISS.FRAGMENT_LENGTH));
      System.arraycopy(digestBuffer, 0, digests, i * HASH_LENGTH, HASH_LENGTH);
    }
    int[] addressTrits = ISS.address(mode, digests);

    int[] calculatedRootTrits = ISS.getMerkleRoot(mode, addressTrits, siblingsTrits,
        0, index, siblingsTrits.length / HASH_LENGTH);

    if (!Arrays.equals(rootTrits, calculatedRootTrits)) {
      String msg = "Calculated root does not match expected! Aborting. " + root + " :: " + Converter.trytes(calculatedRootTrits);
      log.error(msg);
      throw new RuntimeException(msg);
    }
  }

  private void chainTransactionsFillSignatures(int mwm, List<Transaction> txs, String signature) {
    //to chain transactions we start from the LastIndex and move towards index 0.
    Collections.reverse(txs);

    txs.stream().skip(1).forEach(tx -> {
      //copy signature fragment
      String sigFragment = signature.substring((int) (tx.getCurrentIndex() * SIGNATURE_LENGTH),
          (int) (tx.getCurrentIndex() + 1) * SIGNATURE_LENGTH);
      tx.setSignatureFragments(sigFragment);

      //chain bundle
      String prevHash = txs.get((int) (tx.getLastIndex() - tx.getCurrentIndex() - 1)).getHash();
      tx.setTrunkTransaction(prevHash);

      //perform PoW
      Transaction tPoW = new Transaction(getPoWProvider().performPoW(tx.toTrytes(), mwm));
      tx.setAttachmentTimestamp(tPoW.getAttachmentTimestamp());
      tx.setAttachmentTimestampLowerBound(tPoW.getAttachmentTimestampLowerBound());
      tx.setAttachmentTimestampUpperBound(tPoW.getAttachmentTimestampUpperBound());
      tx.setNonce(tPoW.getNonce());
      tx.setHash(Hasher.hashTrytes(powMode, tx.toTrytes()));
    });

    Collections.reverse(txs);
  }

  private String calculateBundleHash(List<Transaction> txs) {

    ICurl sponge = SpongeFactory.create(SpongeFactory.Mode.KERL);

    for (Transaction tx : txs) {
      sponge.absorb(Converter.trits(tx.toTrytes().substring(OFFSET, OFFSET + LENGTH)));
    }

    int[] bundleHashTrits = new int[HASH_LENGTH];
    sponge.squeeze(bundleHashTrits, 0, HASH_LENGTH);

    return Converter.trytes(bundleHashTrits, 0, HASH_LENGTH);
  }
}
