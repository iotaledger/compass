package coo;

import cfb.pearldiver.PearlDiverLocalPoW;
import com.google.common.base.Strings;
import coo.crypto.ISS;
import jota.model.Transaction;
import jota.pow.ICurl;
import jota.pow.JCurl;
import jota.pow.SpongeFactory;
import jota.utils.Converter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static jota.pow.SpongeFactory.Mode.CURLP27;

public class MilestoneDatabase {
    public final static String EMPTY_HASH = Strings.repeat("9", 81);
    public final static String EMPTY_TAG = Strings.repeat("9", 27);
    private final String SEED;
    private final String ROOT;
    private final List<List<String>> layers;

    public MilestoneDatabase(String path, String seed) throws IOException {
        layers = loadLayers(path);
        ROOT = layers.get(0).get(0);
        SEED = seed;
    }

    public MilestoneDatabase(List<List<String>> layers, String seed) throws IOException {
        this.layers = layers;
        ROOT = layers.get(0).get(0);
        SEED = seed;
    }

    public String getRoot() {
        return ROOT;
    }

    public List<Transaction> createMilestone(String trunk, String branch, int index, int mwm) {
        List<Transaction> txs = new ArrayList<>();
        PearlDiverLocalPoW pow = new PearlDiverLocalPoW();


        List<String> leafSiblings = siblings(index, layers);
        String siblingsTrytes = leafSiblings.stream().collect(Collectors.joining(""));
        siblingsTrytes = Strings.padEnd(siblingsTrytes, 27 * 81, '9');

        String tag;
        {
            int[] trits = new int[15];
            for (int i = 0; i < index; i++) {
                Converter.increment(trits, trits.length);
            }
            tag = Converter.trytes(trits);
        }
        tag = Strings.padEnd(tag, 27, '9');

        Transaction tx1 = new Transaction();
        tx1.setSignatureFragments(siblingsTrytes);
        tx1.setAddress(EMPTY_HASH);
        tx1.setCurrentIndex(1);
        tx1.setLastIndex(1);
        tx1.setTimestamp(System.currentTimeMillis() / 1000);
        tx1.setObsoleteTag(EMPTY_TAG);
        tx1.setValue(0);
        tx1.setBundle(EMPTY_HASH);
        tx1.setTrunkTransaction(trunk);
        tx1.setBranchTransaction(branch);
        tx1.setTag(EMPTY_TAG);
        tx1.setNonce(EMPTY_TAG);

        Transaction tx0 = new Transaction();
        tx0.setSignatureFragments(Strings.repeat("9", 27 * 81));
        tx0.setAddress(ROOT);
        tx0.setCurrentIndex(0);
        tx0.setLastIndex(1);
        tx0.setTimestamp(System.currentTimeMillis() / 1000);
        tx0.setObsoleteTag(tag);
        tx0.setValue(0);
        tx0.setBundle(EMPTY_HASH);
        tx0.setTrunkTransaction(EMPTY_HASH);
        tx0.setBranchTransaction(trunk);
        tx0.setTag(tag);
        tx0.setNonce(Strings.repeat("9", 27));

        String bundleHash = generateBundleHash(tx0.toTrytes(), tx1.toTrytes());

        tx0.setBundle(bundleHash);
        tx1.setBundle(bundleHash);

        tx1 = new Transaction(pow.performPoW(tx1.toTrytes(), mwm));
        tx0.setTrunkTransaction(tx1.getHash());
        tx0.setSignatureFragments(createSignature(index, tx1.getHash()));

        tx0 = new Transaction(pow.performPoW(tx0.toTrytes(), mwm));

        txs.add(tx0);
        txs.add(tx1);
        return txs;
    }

    private String createSignature(int index, String tx1Hash) {
        int[] tx1HashTrits = Converter.trits(tx1Hash);
        int[] normalizedBundle = Arrays.copyOf(ISS.normalizedBundle(tx1HashTrits), 27);

        int[] subseed = ISS.subseed(CURLP27, Converter.trits(SEED), index);
        int[] key = ISS.key(CURLP27, subseed, 1);
        int[] signatureFragment = ISS.signatureFragment(CURLP27, normalizedBundle, key);

        return Converter.trytes(signatureFragment);
    }

    private String generateBundleHash(String tx0, String tx1) {
        final int OFFSET = (6561 / 3);
        final int LENGTH = (243 + 81 + 81 + 27 + 27 + 27) / 3;

        int[] t0 = Converter.trits(tx0.substring(OFFSET, OFFSET + LENGTH));
        int[] t1 = Converter.trits(tx1.substring(OFFSET, OFFSET + LENGTH));

        ICurl sponge = SpongeFactory.create(SpongeFactory.Mode.KERL);
        sponge.absorb(t0);
        sponge.absorb(t1);
        sponge.squeeze(t0, 0, JCurl.HASH_LENGTH);

        return Converter.trytes(t0, 0, JCurl.HASH_LENGTH);
    }

    private List<String> siblings(int leafIdx, List<List<String>> layers) {
        List<String> siblings = new ArrayList<>();

        int curLayer = layers.size() - 1;

        while (curLayer > 0) {
            List<String> layer = layers.get(curLayer);
            if ((leafIdx & 1) == 1) {
                // odd
                siblings.add(layer.get(--leafIdx));
            } else {
                siblings.add(layer.get(leafIdx + 1));
            }

            leafIdx >>= 1;
            curLayer--;
        }

        return siblings;
    }

    private List<List<String>> loadLayers(String path) throws IOException {
        Map<Integer, List<String>> result = new ConcurrentHashMap<>();

        StreamSupport.stream(Files.newDirectoryStream(Paths.get(path)).spliterator(), true)
                .forEach((Path p) -> {
                    int idx = Integer.parseInt(p.toString().split("\\.")[1]);
                    try {
                        result.put(idx, Files.readAllLines(p));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        return result.entrySet().stream().map((e) -> e.getValue()).collect(Collectors.toList());
    }
}
