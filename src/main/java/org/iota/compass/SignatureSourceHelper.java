package org.iota.compass;

import com.beust.jcommander.JCommander;
import org.iota.compass.conf.InMemorySignatureSourceConfiguration;
import org.iota.compass.conf.RemoteSignatureSourceConfiguration;

import javax.net.ssl.SSLException;

public class SignatureSourceHelper {
  public static SignatureSource signatureSourceFromArgs(String signatureSourceType, String[] args) throws SSLException {
    SignatureSource signatureSource;
    if ("remote".equals(signatureSourceType)) {
      RemoteSignatureSourceConfiguration sourceConf = new RemoteSignatureSourceConfiguration();
      JCommander.newBuilder().addObject(sourceConf).acceptUnknownOptions(true).build().parse(args);

      if (sourceConf.plaintext) {
        signatureSource = new RemoteSignatureSource(sourceConf.uri);
      } else {
        signatureSource = new RemoteSignatureSource(sourceConf.uri, sourceConf.trustCertCollection, sourceConf.clientCertChain, sourceConf.clientKey);

      }
    } else if ("inmemory".equals(signatureSourceType)) {
      InMemorySignatureSourceConfiguration sourceConf = new InMemorySignatureSourceConfiguration();
      JCommander.newBuilder().addObject(sourceConf).acceptUnknownOptions(true).build().parse(args);

      signatureSource = new InMemorySignatureSource(sourceConf.sigMode, sourceConf.seed, sourceConf.security);
    } else {
      throw new IllegalArgumentException("Invalid signatureSource type: " + signatureSourceType);
    }

    return signatureSource;
  }
}
