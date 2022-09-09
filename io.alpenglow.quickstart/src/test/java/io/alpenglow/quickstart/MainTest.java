package io.alpenglow.quickstart;

import io.helidon.media.jackson.JacksonSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class MainTest {


  private static WebServer webServer;
  private static WebClient webClient;

  @BeforeAll
  public static void startTheServer() {
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
  public void testMicroprofileMetrics() {
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
  public void testMetrics() throws Exception {
    WebClientResponse response = webClient.get()
      .path("/metrics")
      .request()
      .await();
    assertThat(response.status().code(), is(200));
  }

  @Test
  public void testHealth() throws Exception {
    WebClientResponse response = webClient.get()
      .path("health")
      .request()
      .await();
    assertThat(response.status().code(), is(200));
  }

  @Test
  public void testSimpleGreet() {
    Message json = webClient.get()
      .path("/simple-greet")
      .request(Message.class)
      .await();
    assertThat(json.getMessage(), is("Hello World!"));
  }

  @Test
  public void testGreetings() {
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
