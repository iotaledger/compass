import coo.util.AddressGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class AddressGeneratorTest {
    @Test
    public void testGeneration() throws IOException {
        String seed = Util.nextSeed();

        int depth = 3;

        AddressGenerator gen = new AddressGenerator(seed, depth);
        List<String> addresses = gen.calculateAllAddresses();

        Assert.assertEquals(addresses.size(), 1 << depth);
    }

    // TODO (th0br0) verify against sample address list
}
