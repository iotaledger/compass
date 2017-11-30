import coo.MilestoneDatabase;
import coo.crypto.ISS;
import coo.util.AddressGenerator;
import coo.util.MerkleTreeCalculator;
import jota.model.Transaction;
import jota.utils.Converter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static jota.pow.SpongeFactory.Mode.CURLP27;

public class MilestoneTest {
    @Test
    public void runMilestoneTests() throws IOException {
        final String seed = Util.nextSeed();
        final int depth = 4;
        final int MWM = 9;

        final AddressGenerator gen = new AddressGenerator(seed, depth);
        final List<String> addresses = gen.calculateAllAddresses();

        final MerkleTreeCalculator treeCalculator = new MerkleTreeCalculator();
        final List<List<String>> layers = treeCalculator.calculateAllLayers(addresses);

        final MilestoneDatabase db = new MilestoneDatabase(layers, seed);

        for (int i = 0; i < (1 << depth); i++) {
            final List<Transaction> txs = db.createMilestone(MilestoneDatabase.EMPTY_HASH, MilestoneDatabase.EMPTY_HASH, i, MWM);

            final Transaction tx0 = txs.get(0);
            final Transaction tx1 = txs.get(1);

            Assert.assertEquals(db.getRoot(), tx0.getAddress());
            int[] signatureTrits = Converter.trits(tx0.getSignatureFragments());
            int[] trunkTrits = ISS.normalizedBundle(Converter.trits(tx1.getHash()));
            int[] signatureAddress = ISS.address(CURLP27, ISS.digest(CURLP27, Arrays.copyOf(trunkTrits, 27), signatureTrits));
            Assert.assertEquals(addresses.get(i), Converter.trytes(signatureAddress));

            int[] siblingTrits = Converter.trits(tx1.getSignatureFragments());
            int[] root = ISS.getMerkleRoot(CURLP27, signatureAddress, siblingTrits, 0, i, depth);
            Assert.assertEquals(db.getRoot(), Converter.trytes(root));
        }

    }
}
