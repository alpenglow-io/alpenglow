package org.acme;

public enum LetsEncrypt {
  Stage("acme://letsencrypt.org/staging"), Prod("acme://letsencrypt.org");
  private final String uri;

  LetsEncrypt(String uri) {
    this.uri = uri;
  }

  @Override
  public String toString() {
    return uri;
  }
}
