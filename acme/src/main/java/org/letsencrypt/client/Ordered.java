package org.letsencrypt.client;

import org.letsencrypt.LetsEncrypt;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;
import static org.shredzone.acme4j.Status.VALID;

public final class Ordered implements LetsEncrypt {
  private static final Logger log = LoggerFactory.getLogger(Ordered.class);
  private final Order order;
  private final String[] domains;

  Ordered(Order order, String... domains) {
    this.order = order;
    this.domains = domains;
  }

  @Override
  public LetsEncrypt challenge(BiConsumer<String, String> consumer) {
    for (var authorization : order.getAuthorizations()) {
      var challenge = authorization.findChallenge(Dns01Challenge.class);

      var digest = requireNonNull(challenge).getDigest();
      var resourceRecord = Dns01Challenge.toRRName(authorization.getIdentifier());

      consumer.accept(resourceRecord, digest);

      if (challenge.getStatus() == VALID) break;

      trigger(challenge);
    }
    return new Challenged(order, domains);
  }

  private void trigger(Dns01Challenge challenge) {
    try {
      challenge.trigger();
    } catch (AcmeException e) {
      throw new RuntimeException(e);
    }
  }
}
