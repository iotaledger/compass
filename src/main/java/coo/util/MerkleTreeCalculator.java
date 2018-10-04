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

import com.google.common.math.IntMath;
import jota.pow.ICurl;
import jota.pow.SpongeFactory;
import jota.utils.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MerkleTreeCalculator {
  private static final Logger log = LoggerFactory.getLogger(MerkleTreeCalculator.class);
  private final SpongeFactory.Mode mode;

  public MerkleTreeCalculator(SpongeFactory.Mode mode) {
    this.mode = mode;
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 3) {
      throw new IllegalArgumentException("Usage: <sigMode> <addresses.csv> <out dir>");
    }

    Path layers = Paths.get(args[2]);
    try {
      Files.createDirectory(layers);
    } catch (IOException e) {
    }

    (new MerkleTreeCalculator(SpongeFactory.Mode.valueOf(args[0]))).process(args[1], layers);
  }

  private List<String> loadAddresses(String addressCsvPath) throws IOException {
    log.info("Loading addresses from: " + addressCsvPath);
    Stream<String> lines = Files.lines(Paths.get(addressCsvPath));
    return lines.parallel().map((String s) -> {
      String[] chunks = s.split(",");
      return chunks[1];
    }).collect(Collectors.toList());
  }

  public List<String> calculateNextLayer(List<String> inLayer) {
    log.info("Calculating");
    final List<String> layer = Collections.unmodifiableList(inLayer);

    return IntStream.range(0, layer.size() / 2).mapToObj((int idx) -> {
      ICurl sp = SpongeFactory.create(mode);

      int[] t1 = Converter.trits(layer.get(idx * 2));
      int[] t2 = Converter.trits(layer.get(idx * 2 + 1));

      sp.absorb(t1, 0, t1.length);
      sp.absorb(t2, 0, t2.length);

      sp.squeeze(t1, 0, t1.length);

      return Converter.trytes(t1);
    }).collect(Collectors.toList());
  }

  private void writeLayer(Path outputDir, int depth, List<String> elements) throws IOException {
    Path out = Paths.get(outputDir.toString(), ("layer." + depth + ".csv"));
    BufferedWriter writer = Files.newBufferedWriter(out, StandardOpenOption.CREATE);

    for (String node : elements) {
      writer.write(node + "\n");
    }

    writer.close();
  }

  public List<List<String>> calculateAllLayers(List<String> addresses) {
    int depth = IntMath.log2(addresses.size(), RoundingMode.FLOOR);
    List<List<String>> layers = new ArrayList<>(depth);
    List<String> last = addresses;
    layers.add(last);

    while (depth-- > 0) {
      log.info("Calculating nodes for depth " + depth);
      last = calculateNextLayer(last);

      layers.add(last);
    }


    Collections.reverse(layers);
    return layers;
  }

  public void process(String path, Path outputDir) throws IOException {
    List<String> leaves = loadAddresses(path);
    List<List<String>> layers = calculateAllLayers(leaves);

    for(int i = 0; i < layers.size(); i++) {
      writeLayer(outputDir, i, layers.get(i));
    }

    log.info("Successfully wrote merkle tree with root: " + layers.get(0).get(0));
  }
}
