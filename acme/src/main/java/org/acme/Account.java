package org.acme;

public sealed interface Account {
  static Account from(Pem pem) {
    return new Keys(pem);
  }

  static Account load(Pem pem) {
    return new Keys(pem);
  }

  Session open(LetsEncrypt server);
}

enum Empty implements Account {Default}

final class Local implements Account {
  private final Pem pem;
  Local(Pem pem) {
    this.pem = pem;
  }

  @Override
  public Session open(LetsEncrypt server) {
    return null;
  }
}

