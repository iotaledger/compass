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

import coo.MilestoneDatabase;
import coo.MilestoneSource;
import coo.crypto.Hasher;
import coo.crypto.ISS;
import coo.util.AddressGenerator;
import coo.util.MerkleTreeCalculator;
import jota.model.Transaction;
import jota.pow.SpongeFactory;
import jota.utils.Converter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static jota.pow.SpongeFactory.Mode.*;

/**
 * Tests milestone generation & verifies the signatures
 */
public class MilestoneTest {
  private void runForMode(SpongeFactory.Mode powMode, SpongeFactory.Mode sigMode) throws IOException {
    final String seed = TestUtil.nextSeed();
    final int depth = 4;
    final int MWM = 4;

    final AddressGenerator gen = new AddressGenerator(sigMode, seed, depth);
    final List<String> addresses = gen.calculateAllAddresses();

    final MerkleTreeCalculator treeCalculator = new MerkleTreeCalculator(sigMode);
    final List<List<String>> layers = treeCalculator.calculateAllLayers(addresses);

    final MilestoneDatabase db = new MilestoneDatabase(powMode, sigMode, layers, seed);

    for (int i = 0; i < (1 << depth); i++) {
      final List<Transaction> txs = db.createMilestone(MilestoneSource.EMPTY_HASH, MilestoneSource.EMPTY_HASH, i, MWM);

      final Transaction tx0 = txs.get(0);
      final Transaction tx1 = txs.get(1);

      Assert.assertEquals(db.getRoot(), tx0.getAddress());
      int[] signatureTrits = Converter.trits(tx0.getSignatureFragments());
      int[] trunkTrits = Converter.trits(Hasher.hashTrytes(powMode == KERL ? KERL : CURLP81, tx1.toTrytes()));
      trunkTrits = ISS.normalizedBundle(trunkTrits);

      int[] signatureAddress = ISS.address(sigMode, ISS.digest(sigMode, Arrays.copyOf(trunkTrits, ISS.NUMBER_OF_FRAGMENT_CHUNKS), signatureTrits));
      Assert.assertEquals(addresses.get(i), Converter.trytes(signatureAddress));

      int[] siblingTrits = Converter.trits(tx1.getSignatureFragments());
      int[] root = ISS.getMerkleRoot(sigMode, signatureAddress, siblingTrits, 0, i, depth);
      Assert.assertEquals(db.getRoot(), Converter.trytes(root));
    }


  }

  @Test
  public void runCURLP81_CURLP27() throws IOException {
    runForMode(CURLP81, CURLP27);
  }

  @Test
  public void runCURLP81_CURLP81() throws IOException {
    runForMode(CURLP81, CURLP81);
  }

  @Test
  public void runCURLP81_KERL() throws IOException {
    runForMode(CURLP81, KERL);
  }

  @Test
  public void runKERL_CURLP27() throws IOException {
    runForMode(KERL, CURLP27);
  }

  @Test
  public void runKERL_CURLP81() throws IOException {
    runForMode(KERL, CURLP81);
  }

  @Test
  public void runKERL_KERL() throws IOException {
    runForMode(KERL, KERL);
  }
}
