package org.acme;

import java.security.KeyPair;

public sealed interface Session {

  static Session open(LetsEncrypt server, KeyPair pair) {
    return new Remote(server);
  }
}

final class Remote implements Session {
  private final LetsEncrypt server;
  public Remote(LetsEncrypt server) {
    this.server = server;
  }
}
