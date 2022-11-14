package org.letsencrypt;

import io.alpenglow.letsencrypt.func.SneakySupplier;
import io.alpenglow.letsencrypt.sec.Pem;
import org.letsencrypt.client.Challenged;
import org.letsencrypt.client.Accounted;
import org.letsencrypt.client.Client;
import org.letsencrypt.client.Ordered;
import org.letsencrypt.client.Signed;

import java.io.Writer;
import java.util.function.BiConsumer;

public sealed interface LetsEncrypt permits Ordered, Accounted, Client, Challenged, Signed {
  enum Server {
    Stage("acme://letsencrypt.org/staging"), Prod("acme://letsencrypt.org");
    private final String uri;

    Server(String uri) {
      this.uri = uri;
    }

    @Override
    public String toString() {
      return uri;
    }
  }

  static LetsEncrypt stage() {
    return new Client(Server.Stage);
  }
  static LetsEncrypt prod() {
    return new Client(Server.Prod);
  }

  default LetsEncrypt account(Pem pem) {
    throw new IllegalStateException("Can't authenticate client, inconsistent state.");
  }
  default LetsEncrypt order(String... domains) { throw new IllegalStateException("Can't authenticate client, inconsistent state."); }
  default LetsEncrypt challenge(BiConsumer<String, String> consumer) {
    throw new IllegalStateException("Can't authenticate client, inconsistent state.");
  }
  default LetsEncrypt sign(Pem pem) { throw new IllegalStateException("Can't authenticate client, inconsistent state."); }
  default LetsEncrypt certificate(SneakySupplier<Writer> writer) { throw new IllegalStateException("Can't authenticate client, inconsistent state."); }
}
