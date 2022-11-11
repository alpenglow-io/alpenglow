package org.acme;

import org.acme.pem.Unread;
import org.acme.pem.None;
import org.acme.pem.Fetch;
import org.acme.pem.Create;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public sealed interface Pem permits Unread, None, Fetch, Create {
  static Optional<Pem> from(String first, String... rest) {
    return Optional.of(Path.of(first, rest))
      .filter(path -> path.endsWith(".pem"))
      .map(Unread::new);
  }
  default Pem fetch() {
      return switch (this) {
        case Unread it -> new Fetch(() -> Files.newBufferedReader(it.path()));
        default -> this;
      };
  }
  default Pem create() {
    return switch (this) {
      case Unread it -> new Create(() -> Files.newBufferedWriter(it.path()));
      default -> this;
    };
  }
  default Pem createIfNotExists() {
    return switch (this) {
      case Unread it when Files.exists(it.path()) -> new Fetch(() -> Files.newBufferedReader(it.path()));
      case Unread it when Files.notExists(it.path()) -> new Create(() -> Files.newBufferedWriter(it.path()));
      default -> this;
    };
  }

  Session open(LetsEncrypt server);
}

