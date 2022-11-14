package org.acme;

import org.acme.letsencrypt.Remote;
import org.shredzone.acme4j.Session;

import java.util.function.Supplier;

public sealed interface LetsEncrypt extends Supplier<Session> permits Remote {
  enum Server {
    Stage("acme://letsencrypt.org/staging"), Prod("acme://letsencrypt.org");
    private final String uri;

    Server(String uri) {
      this.uri = uri;
    }

    @Override
    public String toString() {
      return uri;
    }
  }

  static LetsEncrypt stage() {
    return new Remote(Server.Stage);
  }
  static LetsEncrypt prod() {
    return new Remote(Server.Prod);
  }
}

