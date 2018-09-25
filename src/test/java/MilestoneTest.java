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

import com.google.common.base.Strings;
import coo.MilestoneDatabase;
import coo.crypto.Hasher;
import coo.crypto.ISS;
import coo.util.AddressGenerator;
import coo.util.MerkleTreeCalculator;
import jota.model.Transaction;
import jota.pow.SpongeFactory;
import jota.utils.Converter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static jota.pow.SpongeFactory.Mode.*;

/**
 * Tests milestone generation & verifies the signatures
 */
public class MilestoneTest {
  private void runForMode(SpongeFactory.Mode powMode, SpongeFactory.Mode sigMode, int security) {
    final String seed = TestUtil.nextSeed();
    final int depth = 4;
    final int MWM = 4;

    final AddressGenerator gen = new AddressGenerator(sigMode, seed, security, depth);
    final List<String> addresses = gen.calculateAllAddresses();

    final MerkleTreeCalculator treeCalculator = new MerkleTreeCalculator(sigMode);
    final List<List<String>> layers = treeCalculator.calculateAllLayers(addresses);
    final MilestoneDatabase db = new MilestoneDatabase(powMode, sigMode, layers, seed, security);

    for (int i = 0; i < (1 << depth); i++) {
      final List<Transaction> txs = db.createMilestone(TestUtil.nextSeed(), TestUtil.nextSeed(), i, MWM);

      final Transaction txFirst = txs.get(0);
      final Transaction txSiblings = txs.get(txs.size() - 1);

      txs.forEach(tx -> Assert.assertTrue("Transaction PoW MWM not met", tx.getHash().endsWith(Strings.repeat("9", MWM / 3))));
      Assert.assertEquals(db.getRoot(), txFirst.getAddress());
      final int[] trunkTrits = ISS.normalizedBundle(Converter.trits(Hasher.hashTrytes(powMode, txSiblings.toTrytes())));

      // Get digest of each individual signature.
      int[] signatureTrits = Converter.trits(
          txs.stream()
              .limit(txs.size() - 1)
              .map(t -> Converter.trytes(ISS.digest(sigMode,
                  Arrays.copyOfRange(trunkTrits, (int) t.getCurrentIndex() * ISS.NUMBER_OF_FRAGMENT_CHUNKS,
                      (int) (t.getCurrentIndex() + 1) * ISS.NUMBER_OF_FRAGMENT_CHUNKS),
                  Converter.trits(t.getSignatureFragments()))))
              .collect(Collectors.joining("")));

      int[] signatureAddress = ISS.address(sigMode, signatureTrits);
      Assert.assertEquals(addresses.get(i), Converter.trytes(signatureAddress));

      int[] siblingTrits = Converter.trits(txSiblings.getSignatureFragments());
      int[] root = ISS.getMerkleRoot(sigMode, signatureAddress, siblingTrits, 0, i, depth);
      Assert.assertEquals(db.getRoot(), Converter.trytes(root));
    }
  }

  @Test
  public void runTests() {
    int from = 1, to = 3;
    SpongeFactory.Mode[] powModes = new SpongeFactory.Mode[]{
        // Jota's LocalPoWProvider only supports CURLP81
        // CURLP27,
        CURLP81,
        KERL
    };

    SpongeFactory.Mode[] sigModes = new SpongeFactory.Mode[]{
        KERL,
        CURLP27,
        CURLP81
    };

    for (SpongeFactory.Mode powMode : powModes) {
      for (SpongeFactory.Mode sigMode : sigModes) {
        for (int security = from; security <= to; security++) {
          System.err.println("Running: " + powMode + " : " + sigMode + " : " + security);
          runForMode(powMode, sigMode, security);
        }
      }
    }

  }


}
