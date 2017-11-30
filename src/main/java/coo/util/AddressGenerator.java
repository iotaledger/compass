package coo.util;

import coo.crypto.ISSInPlace;
import jota.pow.JCurl;
import jota.pow.SpongeFactory;
import jota.utils.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AddressGenerator {
    public static SpongeFactory.Mode MODE = SpongeFactory.Mode.CURLP27;
    public final int COUNT;
    private final String SEED;
    private final int[] SEEDt;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public AddressGenerator(String seed, int depth) {
        this.SEED = seed;
        this.SEEDt = Converter.trits(seed);
        this.COUNT = 1 << depth;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: <seed> <depth> <outfile>");
        }

        new AddressGenerator(args[0], Integer.parseInt(args[1])).work(args[2]);
    }

    public String calculateAddress(int idx) {
        int[] subseed = new int[JCurl.HASH_LENGTH];
        int[] key = new int[ISSInPlace.FRAGMENT_LENGTH];
        int[] digests = new int[key.length / ISSInPlace.FRAGMENT_LENGTH * JCurl.HASH_LENGTH];
        int[] address = new int[JCurl.HASH_LENGTH];

        System.arraycopy(SEEDt, 0, subseed, 0, subseed.length);
        ISSInPlace.subseed(MODE, subseed, idx);
        ISSInPlace.key(MODE, subseed, key);
        ISSInPlace.digests(MODE, key, digests);
        ISSInPlace.address(MODE, digests, address);

        return Converter.trytes(address);
    }

    public List<String> calculateAllAddresses() {
        log.info("Calculating " + COUNT + " addresses.");
        List<String> outList = IntStream.range(0, COUNT)
                .mapToObj(this::calculateAddress)
                .parallel()
                .collect(Collectors.toList());

        return outList;
    }

    public void work(String output) throws IOException {
        List<String> addresses = calculateAllAddresses();
        writeEntriesToFile(addresses, output);
        log.info("Work done.");
    }

    private void writeEntriesToFile(List<String> addresses, String output) throws IOException {
        log.info("Writing addresses to file " + output);
        FileWriter fw = new FileWriter(new File(output), false);
        int counter = 0;
        for (String add : addresses) {
            fw.append(counter++ + "," + add + "\n");
        }
        fw.flush();
        fw.close();
    }
}
