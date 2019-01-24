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

package org.iota.compass;

import com.beust.jcommander.JCommander;
import com.google.common.math.IntMath;
import jota.pow.ICurl;
import jota.pow.SpongeFactory;
import jota.utils.Converter;
import org.iota.compass.conf.LayersCalculatorConfiguration;
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

public class LayersCalculator implements Runnable {
  private final static Logger log = LoggerFactory.getLogger(LayersCalculator.class);

  private final LayersCalculatorConfiguration config;
  private final SignatureSource signatureSource;
  private final int count;

  public LayersCalculator(LayersCalculatorConfiguration config, SignatureSource signatureSource) {
    this.count = 1 << config.depth;
    this.config = config;
    this.signatureSource = signatureSource;
  }

  public static void main(String[] args) throws IOException {
    LayersCalculatorConfiguration config = new LayersCalculatorConfiguration();

    JCommander.newBuilder()
        .addObject(config)
        .acceptUnknownOptions(true)
        .build()
        .parse(args);

    LayersCalculator calc = new LayersCalculator(config, SignatureSourceHelper.signatureSourceFromArgs(config.signatureSource, args));
    calc.run();
  }

  @Override
  public void run() {
    Path layersPath = Paths.get(config.layersPath);
    try {
      Files.createDirectory(layersPath);
    } catch (IOException e) {
    }

    List<String> addresses = calculateAllAddresses();
    log.info("Calculated all addresses.");
    List<List<String>> layers = calculateAllLayers(addresses);

    for (int i = 0; i < layers.size(); i++) {
      try {
        writeLayer(layersPath, i, layers.get(i));
      } catch (IOException e) {
        log.error("Error writing layer: " + i, e);
      }
    }
    log.info("Successfully wrote Merkle Tree with root: " + layers.get(0).get(0));
  }

  public List<String> calculateAllAddresses() {
    log.info("Calculating " + count + " addresses.");
    List<String> outList = IntStream.range(0, count)
        .mapToObj(signatureSource::getAddress)
        .parallel()
        .collect(Collectors.toList());

    return outList;
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

  private void writeLayer(Path outputDir, int depth, List<String> elements) throws IOException {
    Path out = Paths.get(outputDir.toString(), ("layer." + depth + ".csv"));
    BufferedWriter writer = Files.newBufferedWriter(out, StandardOpenOption.CREATE);

    for (String node : elements) {
      writer.write(node + "\n");
    }

    writer.close();
  }

  private List<String> calculateNextLayer(List<String> inLayer) {
    log.info("Calculating");
    final List<String> layer = Collections.unmodifiableList(inLayer);

    return IntStream.range(0, layer.size() / 2).mapToObj((int idx) -> {
      ICurl sp = SpongeFactory.create(signatureSource.getSignatureMode());

      int[] t1 = Converter.trits(layer.get(idx * 2));
      int[] t2 = Converter.trits(layer.get(idx * 2 + 1));

      sp.absorb(t1, 0, t1.length);
      sp.absorb(t2, 0, t2.length);

      sp.squeeze(t1, 0, t1.length);

      return Converter.trytes(t1);
    }).parallel().collect(Collectors.toList());
  }
}
