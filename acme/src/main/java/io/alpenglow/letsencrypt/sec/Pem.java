package io.alpenglow.letsencrypt.sec;

import io.alpenglow.letsencrypt.sec.pem.Generated;
import io.alpenglow.letsencrypt.sec.pem.Read;
import io.alpenglow.letsencrypt.sec.pem.Stored;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Optional;
import java.util.function.Supplier;

public sealed interface Pem extends Supplier<KeyPair> permits Generated, Read, Stored {
  static Pem read(Path path) {
    return new Read(path);
  }

  default Pem orCreate() {
    return this instanceof Read it && Files.notExists(it.path())
      ? new Stored(it.path(), new Generated())
      : this;
  }
}

