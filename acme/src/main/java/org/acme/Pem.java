package org.acme;

import org.acme.pem.Generated;
import org.acme.pem.Fetched;
import org.acme.pem.Stored;
import org.bouncycastle.util.Store;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Optional;
import java.util.function.Supplier;

public sealed interface Pem extends Supplier<Optional<KeyPair>> permits Stored, Fetched, Generated {
  static Optional<Pem> fetch(String first, String... rest) {
    return Optional.of(Path.of(first, rest))
      .filter(path -> path.endsWith(".pem"))
      .map(Fetched::new);
  }

  default Pem orCreate() {
    return this instanceof Fetched it && Files.notExists(it.path())
      ? new Stored(it.path(), new Generated())
      : this;
  }
}

