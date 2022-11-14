package io.alpenglow.letsencrypt.sec.pem;

import io.alpenglow.letsencrypt.sec.Pem;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

public record Read(Path path) implements Pem {
  @Override
  public KeyPair get() {
    if (Files.notExists(path)) return null;
    try (final var binary = Files.newBufferedReader(path)) {
      return KeyPairUtils.readKeyPair(binary);
    } catch (IOException e) {
      return null;
    }
  }
}
