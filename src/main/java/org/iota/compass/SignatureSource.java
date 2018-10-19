package org.iota.compass;

import org.iota.compass.crypto.ISS;
import jota.pow.SpongeFactory;
import jota.utils.Converter;

import java.util.Arrays;

public abstract class SignatureSource {

  /**
   * Provides the key for the given milestone index.
   *
   * @param index
   * @return trit-array containing the key
   */
  public abstract int[] getKey(int index);


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
   * @param index      key / tree leaf index to generate signature for
   * @param hashToSign the hash to be signed
   * @return
   */
  public String createSignature(int index, String hashToSign) {
    int[] key = getKey(index);
    int[] normalizedBundle = ISS.normalizedBundle(Converter.trits(hashToSign));

    StringBuilder fragment = new StringBuilder();

    for (int i = 0; i < getSecurity(); i++) {
      int[] curFrag = ISS.signatureFragment(getSignatureMode(),
          Arrays.copyOfRange(normalizedBundle, i * ISS.NUMBER_OF_FRAGMENT_CHUNKS, (i + 1) * ISS.NUMBER_OF_FRAGMENT_CHUNKS),
          Arrays.copyOfRange(key, i * ISS.FRAGMENT_LENGTH, (i + 1) * ISS.FRAGMENT_LENGTH));
      fragment.append(Converter.trytes(curFrag));
    }

    return fragment.toString();
  }


}
