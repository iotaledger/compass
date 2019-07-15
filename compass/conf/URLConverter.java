package org.iota.compass.conf;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import java.net.MalformedURLException;
import java.net.URL;

public class URLConverter implements IStringConverter<URL> {
    @Override
    public URL convert(String s) {
      try {
        return new URL(s);
      } catch (MalformedURLException e) {
        throw new ParameterException("Invalid URL provided as Remote PoW host.");
      }
    }
}
