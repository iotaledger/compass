package org.iota.compass;

import jota.pow.SpongeFactory;

public abstract class SignatureSource {
  /**
   * Provides the signature for the given milestone index.
   *
   * @param index      the key / leaf index
   * @param bundleHash the hash to sign
   * @return trit-array containing the key
   */
  public abstract String getSignature(long index, String bundleHash);

  /**
   * The security level of this key provider
   *
   * @return security level (1 to 3 inclusive)
   */
  public abstract int getSecurity();

  /**
   * @return the signature mode for this key
   */
  public abstract SpongeFactory.Mode getSignatureMode();


  /**
   * @param index the key / leaf index
   * @return the address for the given key / leaf index
   */
  public abstract String getAddress(long index);

}
