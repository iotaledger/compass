package org.iota.compass.conf;

import com.beust.jcommander.Parameter;

public class SignatureSourceServerConfiguration extends InMemorySignatureSourceConfiguration {
  @Parameter(names = "-port", description = "Port to listen on.")
  public Integer port = 50051;

  @Parameter(names = "-plaintext", description = "Whether to communicate with signatureSource in plaintext")
  public boolean plaintext = false;

  @Parameter(names = "-trustCertCollection", description = "Path to trust cert collection")
  public String trustCertCollection = null;

  @Parameter(names = "-certChain", description = "Path to certificate chain")
  public String certChain = null;

  @Parameter(names = "-privateKey ", description = "Path to the server's certificate's private key")
  public String privateKey = null;
}
