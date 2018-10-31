package org.iota.compass;


import jota.pow.JCurl;
import jota.pow.SpongeFactory;
import jota.utils.Converter;
import org.iota.compass.crypto.ISS;
import org.iota.compass.crypto.ISSInPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * A signature provider that holds the seed in local memory.
 */
public class InMemorySignatureSource extends SignatureSource {
  private static final Logger log = LoggerFactory.getLogger(InMemorySignatureSource.class);

  private final SpongeFactory.Mode mode;
  private final int[] seed;
  private final int security;

  public InMemorySignatureSource(SpongeFactory.Mode mode, String seed, int security) {
    this.mode = mode;
    this.seed = Converter.trits(seed);
    this.security = security;
  }

  @Override
  public int getSecurity() {
    return security;
  }

  @Override
  public SpongeFactory.Mode getSignatureMode() {
    return mode;
  }

  @Override
  public String getAddress(long index) {
    int[] subseed = new int[JCurl.HASH_LENGTH];
    int[] key = new int[ISSInPlace.FRAGMENT_LENGTH * security];
    int[] digests = new int[key.length / ISSInPlace.FRAGMENT_LENGTH * JCurl.HASH_LENGTH];
    int[] address = new int[JCurl.HASH_LENGTH];

    System.arraycopy(seed, 0, subseed, 0, subseed.length);
    ISSInPlace.subseed(mode, subseed, index);
    ISSInPlace.key(mode, subseed, key);
    Arrays.fill(subseed, 0);
    ISSInPlace.digests(mode, key, digests);
    Arrays.fill(key, 0);
    ISSInPlace.address(mode, digests, address);

    return Converter.trytes(address);
  }

  /**
   * @param index      key / tree leaf index to generate signature for
   * @param hashToSign the hash to be signed
   * @return
   */
  @Override
  public String getSignature(long index, String hashToSign) {
    int[] subseed = ISS.subseed(mode, seed, index);
    int[] key = ISS.key(mode, subseed, security);
    Arrays.fill(subseed, 0);

    int[] normalizedBundle = ISS.normalizedBundle(Converter.trits(hashToSign));

    StringBuilder fragment = new StringBuilder();

    for (int i = 0; i < getSecurity(); i++) {
      int[] curFrag = ISS.signatureFragment(getSignatureMode(),
          Arrays.copyOfRange(normalizedBundle, i * ISS.NUMBER_OF_FRAGMENT_CHUNKS, (i + 1) * ISS.NUMBER_OF_FRAGMENT_CHUNKS),
          Arrays.copyOfRange(key, i * ISS.FRAGMENT_LENGTH, (i + 1) * ISS.FRAGMENT_LENGTH));
      fragment.append(Converter.trytes(curFrag));
    }

    Arrays.fill(key, 0);

    return fragment.toString();
  }
}
