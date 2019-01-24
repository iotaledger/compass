package org.iota.compass;

import com.beust.jcommander.JCommander;
import org.iota.compass.conf.InMemorySignatureSourceConfiguration;
import org.iota.compass.conf.RemoteSignatureSourceConfiguration;

import javax.net.ssl.SSLException;

public class SignatureSourceHelper {
  public static SignatureSource signatureSourceFromArgs(SignatureSourceType type, String[] args) throws SSLException {
    switch (type) {
      case REMOTE: {
        RemoteSignatureSourceConfiguration sourceConf = new RemoteSignatureSourceConfiguration();
        JCommander.newBuilder().addObject(sourceConf).acceptUnknownOptions(true).build().parse(args);

        if (sourceConf.plaintext) {
          return new RemoteSignatureSource(sourceConf.uri);
        } else {
          return new RemoteSignatureSource(sourceConf.uri, sourceConf.trustCertCollection, sourceConf.clientCertChain, sourceConf.clientKey);
        }
      }
      case INMEMORY: {
        InMemorySignatureSourceConfiguration sourceConf = new InMemorySignatureSourceConfiguration();
        JCommander.newBuilder().addObject(sourceConf).acceptUnknownOptions(true).build().parse(args);

        return new InMemorySignatureSource(sourceConf.sigMode, sourceConf.seed, sourceConf.security);
      }
    }

    throw new IllegalArgumentException();
  }
}
