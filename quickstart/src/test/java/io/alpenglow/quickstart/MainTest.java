package io.alpenglow.quickstart;

import io.helidon.media.jackson.JacksonSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@TestMethodOrder(MethodOrderer.MethodName.class)
class MainTest {


  private static WebServer webServer;
  private static WebClient webClient;

  @BeforeAll
  public static void startTheServer() {
    System.out.println("Hello!");
    webServer = Main.startServer().await();

    webClient = WebClient.builder()
      .baseUri("http://localhost:" + webServer.port())
      .addMediaSupport(JacksonSupport.create())
      .build();
  }

  @AfterAll
  public static void stopServer() throws Exception {
    if (webServer != null) {
      webServer.shutdown()
        .toCompletableFuture()
        .get(10, TimeUnit.SECONDS);
    }
  }


  @Test
  void testMicroprofileMetrics() {
    String get = webClient.get()
      .path("/simple-greet/greet-count")
      .request(String.class)
      .await();

    assertThat(get, containsString("Hello World!"));

    String openMetricsOutput = webClient.get()
      .path("/metrics")
      .request(String.class)
      .await();

    assertThat("Metrics output", openMetricsOutput, containsString("application_accessctr_total"));
  }

  @Test
  void testMetrics() throws Exception {
    WebClientResponse response = webClient.get()
      .path("/metrics")
      .request()
      .await();
    assertThat(response.status().code(), is(200));
  }

  @Test
  void testHealth() throws Exception {
    WebClientResponse response = webClient.get()
      .path("health")
      .request()
      .await();
    assertThat(response.status().code(), is(200));
  }

  @Test
  void testSimpleGreet() {
    Message json = webClient.get()
      .path("/simple-greet")
      .request(Message.class)
      .await();
    assertThat(json.getMessage(), is("Hello World!"));
  }

  @Test
  void testGreetings() {
    Message json;
    WebClientResponse response;

    json = webClient.get()
      .path("/greet/Joe")
      .request(Message.class)
      .await();
    assertThat(json.getMessage(), is("Hello Joe!"));

    response = webClient.put()
      .path("/greet/greeting")
      .submit("{\"greeting\" : \"Hola\"}")
      .await();
    assertThat(response.status().code(), is(204));

    json = webClient.get()
      .path("/greet/Joe")
      .request(Message.class)
      .await();
    assertThat(json.getMessage(), is("Hola Joe!"));
  }
}
