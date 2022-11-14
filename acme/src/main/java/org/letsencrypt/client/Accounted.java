package org.letsencrypt.client;

import org.letsencrypt.LetsEncrypt;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.exception.AcmeException;

public final class Accounted implements LetsEncrypt {
  private final Account account;

  Accounted(Account account) {
    this.account = account;
  }

  @Override
  public LetsEncrypt order(String... domains) {
    try {
      return new Ordered(
        account
          .newOrder()
          .domains(domains)
          .create(),
        domains
      );
    } catch (AcmeException e) {
      throw new RuntimeException(e);
    }
  }
}
