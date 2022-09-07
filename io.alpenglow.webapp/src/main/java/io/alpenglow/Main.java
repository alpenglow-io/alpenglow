package io.alpenglow.webapp;

import com.sun.net.httpserver.SimpleFileServer;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static com.sun.net.httpserver.SimpleFileServer.OutputLevel.VERBOSE;
import static java.util.Objects.requireNonNull;

public enum Main {;
  public static void main(String[] args) throws URISyntaxException {
    final var server = SimpleFileServer.createFileServer(
      new InetSocketAddress(8080),
      Path.of(requireNonNull(Main.class.getResource("/webapp"), "Can't get any resource from root").toURI()),
      VERBOSE
    );

    server.start();
  }
}
