package org.letsencrypt.client;

import io.alpenglow.letsencrypt.func.SneakySupplier;
import org.letsencrypt.LetsEncrypt;
import org.shredzone.acme4j.Certificate;

import java.io.IOException;
import java.io.Writer;

public final class Signed implements LetsEncrypt {
  private final Certificate certificate;

  Signed(Certificate certificate) {
    this.certificate = certificate;
  }

  @Override
  public LetsEncrypt certificate(SneakySupplier<Writer> writer) {
    try {
      certificate.writeCertificate(writer.get());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }
}
