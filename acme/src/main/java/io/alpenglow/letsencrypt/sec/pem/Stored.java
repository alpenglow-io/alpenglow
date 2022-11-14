package io.alpenglow.letsencrypt.sec.pem;

import io.alpenglow.letsencrypt.sec.Pem;
import org.shredzone.acme4j.util.KeyPairUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

public record Stored(Path path, Pem pem) implements Pem {
  @Override
  public KeyPair get() {
    var pair = pem.get();
    try (var binary = Files.newBufferedWriter(path)) {
      KeyPairUtils.writeKeyPair(pair, binary);
      return pair;
    } catch (IOException e) {
      return null;
    }
  }
}
