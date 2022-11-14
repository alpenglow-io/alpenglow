package org.acme;

import org.acme.account.Open;

public sealed interface Account permits Open {
  static Account open(LetsEncrypt letsEncrypt, Pem pem) {
    return new Open(letsEncrypt, pem);
  }
  Signature signature(String... domains);
}

