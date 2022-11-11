package org.acme.pem;

import org.acme.LetsEncrypt;
import org.acme.Pem;
import org.acme.Session;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.io.BufferedWriter;
import java.io.IOException;

public final class Create implements Pem {
  private static final String algorithm = "secp256r1";
  private final BufferedWriter buffer;

  public Create(Silent<BufferedWriter> silent) {
    this(silent.get());
  }

  Create(BufferedWriter buffer) {
    this.buffer = buffer;
  }

  @Override
  public Session open(LetsEncrypt server) {
    try (var binary = buffer) {
      var keyPair = KeyPairUtils.createECKeyPair(algorithm);
      KeyPairUtils.writeKeyPair(keyPair, binary);
      return Session.open(server, keyPair);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
