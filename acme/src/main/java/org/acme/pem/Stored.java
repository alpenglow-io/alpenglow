package org.acme.pem;

import org.acme.Pem;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Optional;

public record Stored(Path path, Pem pem) implements Pem {
  @Override
  public Optional<KeyPair> get() {
    return pem.get().map(this::store);
  }

  private KeyPair store(KeyPair keyPair) {
    try (var binary = Files.newBufferedWriter(path)) {
      KeyPairUtils.writeKeyPair(keyPair, binary);
      return keyPair;
    } catch (IOException e) {
      return null;
    }
  }
}
