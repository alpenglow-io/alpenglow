package org.letsencrypt.client;

import io.alpenglow.letsencrypt.sec.Pem;
import org.letsencrypt.LetsEncrypt;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.exception.AcmeException;

public final class Client implements LetsEncrypt {
  private final Server server;

  public Client(Server server) {
    this.server = server;
  }

  @Override
  public LetsEncrypt account(Pem pem) {
    try {
      return new Accounted(
        new AccountBuilder()
          .useKeyPair(pem.get())
          .create(new Session(server.toString()))
      );
    } catch (AcmeException e) {
      throw new RuntimeException(e);
    }
  }
}

