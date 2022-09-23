package io.alpenglow.service;

import io.helidon.common.LogConfig;
import io.helidon.config.Config;

import java.util.logging.Logger;

public enum Main {
  ;
  private static final Logger log = Logger.getLogger(Main.class.getSimpleName());

  static void main(String... args) {
    LogConfig.configureRuntime();

    HttpServer.staticContent(Config.create())
      .start()
      .thenAccept(server -> {
        log.info("Http Server started on port " + server.port());
        server.whenShutdown().thenRun(() -> System.out.println("Http Server stopped. Good bye!"));
      })
      .exceptionallyAccept(t -> log.severe("Can't start Http Server: " + t.getMessage()));
  }
}
