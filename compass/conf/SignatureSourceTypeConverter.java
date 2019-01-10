package org.iota.compass.conf;

import com.beust.jcommander.IStringConverter;
import org.iota.compass.SignatureSourceType;

public class SignatureSourceTypeConverter implements IStringConverter<SignatureSourceType> {
  @Override
  public SignatureSourceType convert(String s) {
    return SignatureSourceType.valueOf(s.toUpperCase());
  }
}
