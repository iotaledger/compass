import coo.MilestoneDatabase;
import coo.MilestoneSource;
import coo.crypto.ISS;
import coo.util.AddressGenerator;
import coo.util.MerkleTreeCalculator;
import jota.model.Transaction;
import jota.pow.ICurl;
import jota.pow.SpongeFactory;
import jota.utils.Converter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MilestoneTest {
  private void runForMode(SpongeFactory.Mode mode) throws IOException {
    final String seed = Util.nextSeed();
    final int depth = 4;
    final int MWM = 4;

    final AddressGenerator gen = new AddressGenerator(mode, seed, depth);
    final List<String> addresses = gen.calculateAllAddresses();

    final MerkleTreeCalculator treeCalculator = new MerkleTreeCalculator(mode);
    final List<List<String>> layers = treeCalculator.calculateAllLayers(addresses);

    final MilestoneDatabase db = new MilestoneDatabase(mode, layers, seed);

    for (int i = 0; i < (1 << depth); i++) {
      final List<Transaction> txs = db.createMilestone(MilestoneSource.EMPTY_HASH, MilestoneSource.EMPTY_HASH, i, MWM);

      final Transaction tx0 = txs.get(0);
      final Transaction tx1 = txs.get(1);

      Assert.assertEquals(db.getRoot(), tx0.getAddress());
      int[] signatureTrits = Converter.trits(tx0.getSignatureFragments());
      int[] trunkTrits;
      if (mode == SpongeFactory.Mode.KERL) {
        ICurl sponge = SpongeFactory.create(SpongeFactory.Mode.KERL);
        trunkTrits = new int[243];
        sponge.absorb(Converter.trits(tx1.toTrytes()));
        sponge.squeeze(trunkTrits);
      } else {
        trunkTrits = Converter.trits(tx1.getHash());
      }
      trunkTrits = ISS.normalizedBundle(trunkTrits);

      int[] signatureAddress = ISS.address(mode, ISS.digest(mode, Arrays.copyOf(trunkTrits, 27), signatureTrits));
      Assert.assertEquals(addresses.get(i), Converter.trytes(signatureAddress));

      int[] siblingTrits = Converter.trits(tx1.getSignatureFragments());
      int[] root = ISS.getMerkleRoot(mode, signatureAddress, siblingTrits, 0, i, depth);
      Assert.assertEquals(db.getRoot(), Converter.trytes(root));
    }


  }

  @Test
  public void runCURLP27() throws IOException {
    runForMode(SpongeFactory.Mode.CURLP27);
  }

  @Test
  public void runCURLP81() throws IOException {
    runForMode(SpongeFactory.Mode.CURLP81);
  }

  @Test
  public void runKERL() throws IOException {
    runForMode(SpongeFactory.Mode.KERL);
  }
}
