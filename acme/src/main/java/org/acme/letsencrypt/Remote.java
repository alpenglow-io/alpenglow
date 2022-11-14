package org.acme.letsencrypt;

import org.acme.LetsEncrypt;
import org.shredzone.acme4j.Session;

public record Remote(LetsEncrypt.Server server) implements LetsEncrypt {
  @Override
  public Session get() {
    return new Session(server.toString());
  }
}
