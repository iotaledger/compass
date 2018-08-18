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


/**
 * Given a seed, calculates a list of addresses to be used for Milestone Merkle Tree generation
 */
public class AddressGenerator {
    public final SpongeFactory.Mode MODE;
    public final int COUNT;
    private final int[] SEEDt;
    private final int SECURITY;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public AddressGenerator(SpongeFactory.Mode mode, String seed, int security, int depth) {
        this.SEEDt = Converter.trits(seed);
        this.COUNT = 1 << depth;
        this.MODE = mode;
        this.SECURITY = security;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: <sigMode> <seed> <security> <depth> <outfile>");
        }

        new AddressGenerator(SpongeFactory.Mode.valueOf(args[0]), args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3])).work(args[3]);
    }

    public String calculateAddress(int idx) {
        int[] subseed = new int[JCurl.HASH_LENGTH];
        int[] key = new int[ISSInPlace.FRAGMENT_LENGTH * SECURITY];
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
