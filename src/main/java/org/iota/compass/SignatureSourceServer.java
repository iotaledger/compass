package org.iota.compass;

import com.beust.jcommander.JCommander;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import org.iota.compass.conf.SignatureSourceServerConfiguration;
import org.iota.compass.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class SignatureSourceServer {
  private static final Logger log = LoggerFactory.getLogger(SignatureSourceServer.class);

  private final SignatureSourceServerConfiguration config;
  private final SignatureSource signatureSource;

  private Server server;

  public SignatureSourceServer(SignatureSourceServerConfiguration config) {
    this.config = config;
    this.signatureSource = new InMemorySignatureSource(config.sigMode, config.seed, config.security);
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    SignatureSourceServerConfiguration config = new SignatureSourceServerConfiguration();

    JCommander.newBuilder()
        .addObject(config)
        .build()
        .parse(args);

    final SignatureSourceServer server = new SignatureSourceServer(config);
    server.start();
    server.blockUntilShutdown();
  }

  public void start() throws IOException {
    NettyServerBuilder builder =
        NettyServerBuilder.forPort(config.port)
            .addService(new SignatureSourceImpl(signatureSource));

    if (!config.plaintext) {
      if (config.certChain == null || config.certChain.isEmpty()) {
        throw new IllegalArgumentException("-certChain is required if not running in plaintext mode");
      }

      if (config.privateKey == null || config.privateKey.isEmpty()) {
        throw new IllegalArgumentException("-privateKey is required if not running in plaintext mode");
      }

      SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(new File(config.certChain),
          new File(config.privateKey));
      if (config.trustCertCollection != null) {
        sslClientContextBuilder.trustManager(new File(config.trustCertCollection));
        sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
      }

      builder = builder.sslContext(GrpcSslContexts.configure(sslClientContextBuilder,
          SslProvider.OPENSSL).build());
    }

    server = builder.build();
    server.start();

    log.info("Server started, listening on " + config.port);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.err.println("*** shutting down gRPC server since JVM is shutting down");
      SignatureSourceServer.this.stop();
      System.err.println("*** server shut down");
    }));
  }

  public void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  public void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }


  static class SignatureSourceImpl extends SignatureSourceGrpc.SignatureSourceImplBase {
    private final SignatureSource signatureSource;

    public SignatureSourceImpl(SignatureSource signatureSource) {
      super();
      this.signatureSource = signatureSource;
    }

    @Override
    public void getSecurity(GetSecurityRequest request, StreamObserver<GetSecurityResponse> responseObserver) {
      log.info("Responding to getSecurity");
      responseObserver.onNext(GetSecurityResponse.newBuilder()
          .setSecurity(signatureSource.getSecurity())
          .build());
      responseObserver.onCompleted();
    }

    @Override
    public void getSignatureMode(GetSignatureModeRequest request, StreamObserver<GetSignatureModeResponse> responseObserver) {
      log.info("Responding to getSignatureMode");
      SignatureMode mode;

      switch (signatureSource.getSignatureMode()) {
        case CURLP27:
          mode = SignatureMode.CURLP27;
          break;
        case CURLP81:
          mode = SignatureMode.CURLP81;
          break;
        case KERL:
          mode = SignatureMode.KERL;
          break;
        default:
          throw new RuntimeException();
      }

      responseObserver.onNext(GetSignatureModeResponse.newBuilder()
          .setMode(mode)
          .build());
      responseObserver.onCompleted();
    }

    @Override
    public void getSignature(GetSignatureRequest request, StreamObserver<GetSignatureResponse> responseObserver) {
      log.info("Responding to getSignature for index: " + request.getIndex() + " and hash: " + request.getHash());

      responseObserver.onNext(GetSignatureResponse.newBuilder()
          .setSignature(signatureSource.getSignature(request.getIndex(), request.getHash()))
          .build());
      responseObserver.onCompleted();
    }

    @Override
    public void getAddress(GetAddressRequest request, StreamObserver<GetAddressResponse> responseObserver) {
      log.info("Responding to getAddress for index: " + request.getIndex());

      responseObserver.onNext(GetAddressResponse.newBuilder()
          .setAddress(signatureSource.getAddress(request.getIndex()))
          .build());
      responseObserver.onCompleted();
    }
  }

}
