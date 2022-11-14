package org.acme.certficate;

import org.acme.Signature;
import org.acme.Challenge;

public record Ordered(String... domains) implements Signature {
  @Override
  public Challenge challenge() {
    return null;
  }
}
