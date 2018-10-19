package org.iota.compass;

import com.beust.jcommander.JCommander;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import jota.pow.SpongeFactory;
import jota.utils.Converter;
import org.iota.compass.conf.SignatureSourceServerConfiguration;
import org.iota.compass.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SignatureSourceServer {
  private static final Logger log = LoggerFactory.getLogger(SignatureSourceServer.class);

  private final SignatureSourceServerConfiguration config;
  private final SignatureSource signatureSource;

  private void start() throws IOException {
    server = ServerBuilder.forPort(config.port)
        .addService(new SignatureSourceImpl(signatureSource))
        .build()
        .start();
    log.info("Server started, listening on " + config.port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        SignatureSourceServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }


  private Server server;

  public SignatureSourceServer(SignatureSourceServerConfiguration config) {
    this.config = config;
    this.signatureSource = new InMemorySignatureSource(SpongeFactory.Mode.valueOf(config.sigMode), config.seed, config.security);
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    SignatureSourceServerConfiguration config = new SignatureSourceServerConfiguration();

    JCommander.newBuilder().addObject(config).build().parse(args);

    final SignatureSourceServer server = new SignatureSourceServer(config);
    server.start();
    server.blockUntilShutdown();
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
    public void getKey(GetKeyRequest request, StreamObserver<GetKeyResponse> responseObserver) {
      log.info("Responding to getKey for index: " + request.getIndex());

      responseObserver.onNext(GetKeyResponse.newBuilder()
          .setKey(Converter.trytes(signatureSource.getKey(request.getIndex())))
          .build());
      responseObserver.onCompleted();
    }
  }

}
