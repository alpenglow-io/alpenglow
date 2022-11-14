package org.acme;

import org.acme.challenge.Accepted;
import org.acme.challenge.Found;
import org.shredzone.acme4j.Authorization;

public sealed interface Challenge permits Accepted, Found {
  static Challenge find(Authorization authorization) {
    return new Found(authorization);
  }

  Challenge accept();
  Challenge verify();
  Certificate sign(Pem pem);
}
