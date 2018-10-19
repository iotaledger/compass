package org.iota.compass.conf;

import com.beust.jcommander.Parameter;

public class SignatureSourceServerConfiguration extends InMemorySignatureSourceConfiguration {
  @Parameter(names = "-port", description = "Port to listen on.")
  public Integer port = 50051;


}
