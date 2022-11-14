package org.acme;

import org.acme.certficate.Ordered;

public sealed interface Signature permits Ordered {
  static Signature order(String... domains) {
    return new Ordered(domains);
  }
  Challenge challenge();
}
