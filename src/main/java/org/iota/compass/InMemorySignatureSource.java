package org.iota.compass;


import org.iota.compass.crypto.ISS;
import jota.pow.SpongeFactory;
import jota.utils.Converter;

/**
 * A signature provider that holds the seed in local memory.
 */
public class InMemorySignatureSource extends SignatureSource {
  private final SpongeFactory.Mode mode;
  private final int[] seed;
  private final int security;

  public InMemorySignatureSource(SpongeFactory.Mode mode, String seed, int security) {
    this.mode = mode;
    this.seed = Converter.trits(seed);
    this.security = security;
  }

  @Override
  public int[] getKey(int index) {
    int[] subseed = ISS.subseed(mode, seed, index);
    return ISS.key(mode, subseed, security);

  }

  @Override
  public int getSecurity() {
    return security;
  }

  @Override
  public SpongeFactory.Mode getSignatureMode() {
    return mode;
  }
}
