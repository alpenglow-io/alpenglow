package org.acme.account;

import org.acme.Account;
import org.acme.LetsEncrypt;
import org.acme.Pem;
import org.acme.Signature;

public record Open(LetsEncrypt letsEncrypt, Pem pem) implements Account {
  @Override
  public Signature signature(String... domains) {
    return Signature.order(domains);
  }
}
