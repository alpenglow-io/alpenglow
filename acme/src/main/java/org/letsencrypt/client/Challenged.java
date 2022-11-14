package org.letsencrypt.client;

import io.alpenglow.letsencrypt.sec.Pem;
import org.letsencrypt.LetsEncrypt;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class Challenged implements LetsEncrypt {
  private static final Logger log = LoggerFactory.getLogger(Challenged.class);
  private final Order order;
  private final String[] domains;

  Challenged(Order order, String... domains) {
    this.order = order;
    this.domains = domains;
  }

  @Override
  public LetsEncrypt sign(Pem pem) {
    try {
      var request = new CSRBuilder();
      request.addDomains(domains);
      request.sign(pem.get());
      order.execute(request.getEncoded());

      waitForValidation();

      return new Signed(order.getCertificate());
    } catch (AcmeException | IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void waitForValidation() throws InterruptedException, AcmeException {
    var attempts = 10;
    while (order.getStatus() != Status.VALID && attempts-- > 0) {
      if (order.getStatus() == Status.INVALID) {
        log.error("Order has failed, reason: {}", order.getError());
      }
      Thread.sleep(3000L);
      order.update();
    }
  }
}
