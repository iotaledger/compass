package coo.crypto;

import jota.pow.ICurl;
import jota.pow.JCurl;
import jota.pow.SpongeFactory;
import jota.utils.Converter;

public class Hasher {
  public static String hashTrytes(SpongeFactory.Mode mode, String trytes) {
    int[] hash = new int[JCurl.HASH_LENGTH];

    ICurl sponge = SpongeFactory.create(mode);
    sponge.absorb(Converter.trits(trytes));
    sponge.squeeze(hash);

    return Converter.trytes(hash);
  }
}
