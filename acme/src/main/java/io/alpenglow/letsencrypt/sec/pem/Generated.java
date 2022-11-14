package io.alpenglow.letsencrypt.sec.pem;

import io.alpenglow.letsencrypt.sec.Pem;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.security.KeyPair;

public record Generated() implements Pem {
  private static final String algorithm = "secp256r1";

  @Override
  public KeyPair get() {
    return KeyPairUtils.createECKeyPair(algorithm);
  }
}
