package org.acme.pem;

import org.acme.LetsEncrypt;
import org.acme.Pem;
import org.acme.Session;

import java.nio.file.Path;

public record Unread(Path path) implements Pem {
  @Override
  public Session open(LetsEncrypt server) {
    throw new IllegalStateException("Can't open session, pem is not either read or written.");
  }
}
