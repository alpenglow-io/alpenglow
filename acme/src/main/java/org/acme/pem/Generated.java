package org.acme.pem;

import org.acme.Pem;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.security.KeyPair;
import java.util.Optional;

public record Generated() implements Pem {
  private static final String algorithm = "secp256r1";

  @Override
  public Optional<KeyPair> get() {
    return Optional.of(KeyPairUtils.createECKeyPair(algorithm));
  }
}
