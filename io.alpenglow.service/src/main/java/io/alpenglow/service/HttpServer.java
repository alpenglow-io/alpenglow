package io.alpenglow.service;


import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.staticcontent.StaticContentSupport;

public interface HttpServer {
  static HttpServer staticContent(Config config) {
    return new StaticContent(config);
  }

  Single<WebServer> start();
}

final class StaticContent implements HttpServer {
  private final Config config;

  StaticContent(Config config) {
    this.config = config;
  }

  private Routing staticContent() {
    return Routing.builder()
      .register("/",
        StaticContentSupport.builder("/webapp")
          .welcomeFileName("index.html")
          .build()
      )
      .build();
  }

  @Override
  public Single<WebServer> start() {
    return WebServer.builder(this::staticContent)
      .config(config.get("server"))
      .build()
      .start();
  }

}
