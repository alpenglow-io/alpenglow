package org.acme.pem;

import org.acme.Pem;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Optional;

public record Fetched(Path path) implements Pem {
  @Override
  public Optional<KeyPair> get() {
    if (Files.notExists(path)) return Optional.empty();
    try (final var binary = Files.newBufferedReader(path)) {
      return Optional.of(KeyPairUtils.readKeyPair(binary));
    } catch (IOException e) {
      return Optional.empty();
    }
  }
}
