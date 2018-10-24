package org.iota.compass.conf;

import com.beust.jcommander.IStringConverter;
import jota.pow.SpongeFactory;

public class SpongeModeConverter implements IStringConverter<SpongeFactory.Mode> {
  @Override
  public SpongeFactory.Mode convert(String s) {
    return SpongeFactory.Mode.valueOf(s);
  }
}
