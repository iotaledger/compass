package org.iota.compass;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jota.pow.SpongeFactory;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.iota.compass.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of a SignatureSource that talks to a remote gRPC service.
 */
public class RemoteSignatureSource extends SignatureSource {
  private static final Logger log = LoggerFactory.getLogger(RemoteSignatureSource.class);

  private final SignatureSourceGrpc.SignatureSourceBlockingStub serviceStub;

  private Optional<Integer> cachedSecurity = Optional.empty();
  private Optional<SpongeFactory.Mode> cachedSignatureMode = Optional.empty();

  /**
   * Constructs a RemoteSignatureSource using an encrypted gRPC channel.
   *
   * @param uri                         the URI of the host to connect to
   * @param trustCertCollectionFilePath
   * @param clientCertChainFilePath
   * @param clientPrivateKeyFilePath
   * @throws SSLException
   */
  public RemoteSignatureSource(String uri,
                               String trustCertCollectionFilePath,
                               String clientCertChainFilePath,
                               String clientPrivateKeyFilePath) throws SSLException {
    this(OkHttpChannelBuilder
        .forTarget(uri)
        .useTransportSecurity()
        .idleTimeout(5, TimeUnit.SECONDS)
        .sslSocketFactory(createClientCertSslSocketFactory(trustCertCollectionFilePath, clientCertChainFilePath, clientPrivateKeyFilePath))
        .build());
  }

  private static PrivateKey createPrivateKeyFromPemFile(final String keyFileName) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
      byte []pemContent;
      try (PemReader pemReader = new PemReader(new FileReader(keyFileName))) {
          PemObject pemObject = pemReader.readPemObject();
          pemContent = pemObject.getContent();
      }
      PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(pemContent);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePrivate(encodedKeySpec);
  }

  private static SSLSocketFactory createClientCertSslSocketFactory(String trustCertCollectionFilePath,
                                                            String clientCertChainFilePath,
                                                            String clientPrivateKeyFilePath ) throws RuntimeException {
      try {
          KeyStore appKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
          CertificateFactory cf = CertificateFactory.getInstance("X.509");
          appKeyStore.load(null, null);

          // Import X509 client cert in KeyStore
          X509Certificate clientCert;
          try (InputStream is = new FileInputStream(new File(clientCertChainFilePath))) {
              clientCert = (X509Certificate) cf.generateCertificate(is);
              appKeyStore.setCertificateEntry("clientCert", clientCert);
          }

          // Import and associate PrivateKey to client cert
          PrivateKey clientKey = createPrivateKeyFromPemFile(clientPrivateKeyFilePath);
          appKeyStore.setKeyEntry("clientKey", clientKey, null, new Certificate[]{clientCert});

          // Import CA
          try (InputStream is = new FileInputStream(new File(trustCertCollectionFilePath))) {
              X509Certificate CA = (X509Certificate) cf.generateCertificate(is);
              appKeyStore.setCertificateEntry("CA", CA);
          }

          // Initialize TrustManager against KeyStore
          TrustManagerFactory tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
          tm.init(appKeyStore);

          // Initialize KeyManager against KeyStore
          KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
          km.init(appKeyStore, null);

          // Finally build SSL Context
          SSLContext context = SSLContext.getInstance("TLS");
          context.init(km.getKeyManagers(), tm.getTrustManagers(), null);

          return context.getSocketFactory();
      } catch (Exception e) {
          throw new RuntimeException("Error building TLS context", e);
      }
  }

  /**
   * Constructs a RemoteSignatureSource using an *unencrypted* gRPC channel.
   *
   * @param uri the URI of the host to connect to
   */
  public RemoteSignatureSource(String uri) {
    this(ManagedChannelBuilder.forTarget(uri).usePlaintext().idleTimeout(5, TimeUnit.SECONDS).build());
  }

  private RemoteSignatureSource(ManagedChannel channel) {
    this.serviceStub = SignatureSourceGrpc.newBlockingStub(channel);
  }

  private static SslContext buildSslContext(
      String trustCertCollectionFilePath,
      String clientCertChainFilePath,
      String clientPrivateKeyFilePath) throws SSLException {
    SslContextBuilder builder = GrpcSslContexts.forClient();
    if (trustCertCollectionFilePath != null) {
      builder.trustManager(new File(trustCertCollectionFilePath));
    }
    if (clientCertChainFilePath != null && !clientCertChainFilePath.isEmpty()
        && clientPrivateKeyFilePath != null && !clientPrivateKeyFilePath.isEmpty()) {
      builder.keyManager(new File(clientCertChainFilePath), new File(clientPrivateKeyFilePath));
    }
    return builder.build();
  }

  @Override
  public String getSignature(long index, String hash) {
    log.trace("Requesting signature for index: " + index + " and hash: " + hash);

    GetSignatureResponse response = serviceStub.getSignature(GetSignatureRequest.newBuilder().setIndex(index).setHash(hash).build());


    return response.getSignature();
  }

  @Override
  public int getSecurity() {
    synchronized (cachedSecurity) {
      if (cachedSecurity.isPresent())
        return cachedSecurity.get();


      GetSecurityResponse response = serviceStub.getSecurity(GetSecurityRequest.getDefaultInstance());
      cachedSecurity = Optional.of(response.getSecurity());

      log.info("Caching security level: " + response.getSecurity());

      return response.getSecurity();
    }
  }

  @Override
  public SpongeFactory.Mode getSignatureMode() {
    synchronized (cachedSignatureMode) {
      if (cachedSignatureMode.isPresent()) return cachedSignatureMode.get();

      GetSignatureModeResponse response = serviceStub.getSignatureMode(GetSignatureModeRequest.getDefaultInstance());

      SpongeFactory.Mode spongeMode;
      switch (response.getMode()) {
        case CURLP27:
          spongeMode = SpongeFactory.Mode.CURLP27;
          break;
        case CURLP81:
          spongeMode = SpongeFactory.Mode.CURLP81;
          break;
        case KERL:
          spongeMode = SpongeFactory.Mode.KERL;
          break;
        default:
          throw new RuntimeException("Unknown remote signature mode: " + response.getMode());
      }

      cachedSignatureMode = Optional.of(spongeMode);

      log.info("Caching signature mode: " + spongeMode);

      return spongeMode;
    }
  }

  @Override
  public String getAddress(long index) {
    GetAddressResponse response = serviceStub.getAddress(GetAddressRequest.newBuilder().setIndex(index).build());
    return response.getAddress();
  }
}
