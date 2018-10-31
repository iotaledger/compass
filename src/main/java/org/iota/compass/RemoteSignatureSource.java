package org.iota.compass;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jota.pow.SpongeFactory;
import org.iota.compass.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.Optional;

/**
 * An implementation of a SignatureSource that talks to a remote gRPC service.
 */
public class RemoteSignatureSource extends SignatureSource {
  private static final Logger log = LoggerFactory.getLogger(RemoteSignatureSource.class);

  private final ManagedChannel channel;
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
    this(NettyChannelBuilder
        .forTarget(uri)
        .useTransportSecurity()
        .sslContext(
            buildSslContext(trustCertCollectionFilePath, clientCertChainFilePath, clientPrivateKeyFilePath))
        .build());
  }


  /**
   * Constructs a RemoteSignatureSource using an *unencrypted* gRPC channel.
   *
   * @param uri
   */
  public RemoteSignatureSource(String uri) {
    this(ManagedChannelBuilder.forTarget(uri).usePlaintext().build());
  }

  RemoteSignatureSource(ManagedChannel channel) {
    this.channel = channel;
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
