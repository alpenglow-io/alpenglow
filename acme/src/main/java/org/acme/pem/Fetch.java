package org.acme.pem;

import org.acme.LetsEncrypt;
import org.acme.Pem;
import org.acme.Session;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.io.BufferedReader;
import java.io.IOException;

public final class Fetch implements Pem {
  private final BufferedReader buffer;

  public Fetch(Silent<BufferedReader> silent) {
    this(silent.get());
  }

  Fetch(BufferedReader buffer) {
    this.buffer = buffer;
  }

  @Override
  public Session open(LetsEncrypt server) {
    try (final var binary = buffer) {
      return Session.open(server, KeyPairUtils.readKeyPair(binary));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
