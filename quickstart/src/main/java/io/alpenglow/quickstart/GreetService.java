package io.alpenglow.quickstart;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.webserver.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple service to greet you. Examples:
 * <p>
 * Get default greeting message:
 * curl -X GET http://localhost:8080/greet
 * <p>
 * Get greeting message for Joe:
 * curl -X GET http://localhost:8080/greet/Joe
 * <p>
 * Change greeting
 * curl -X PUT -H "Content-Type: application/json" -d '{"greeting" : "Howdy"}' http://localhost:8080/greet/greeting
 * <p>
 * The message is returned as a JSON object
 */
public class GreetService implements Service {

  private static final Logger LOGGER = Logger.getLogger(GreetService.class.getName());
  /**
   * The config value for the key {@code greeting}.
   */
  private final AtomicReference<String> greeting = new AtomicReference<>();

  GreetService(Config config) {
    greeting.set(config.get("app.greeting").asString().orElse("Ciao"));
  }

  private static <T> T processErrors(Throwable ex, ServerRequest request, ServerResponse response) {

    LOGGER.log(Level.FINE, "Internal error", ex);
    Message jsonError = new Message();
    jsonError.setMessage("Internal error");
    response.status(Http.Status.INTERNAL_SERVER_ERROR_500).send(jsonError);

    return null;
  }

  /**
   * A service registers itself by updating the routing rules.
   *
   * @param rules the routing rules.
   */
  @Override
  public void update(Routing.Rules rules) {
    rules
      .get("/", this::getDefaultMessageHandler)
      .get("/{name}", this::getMessageHandler)
      .put("/greeting", Handler.create(Message.class, this::updateGreeting));
  }

  /**
   * Return a worldly greeting message.
   *
   * @param request  the server request
   * @param response the server response
   */
  private void getDefaultMessageHandler(ServerRequest request, ServerResponse response) {
    sendResponse(response, "World");
  }

  /**
   * Return a greeting message using the name that was provided.
   *
   * @param request  the server request
   * @param response the server response
   */
  private void getMessageHandler(ServerRequest request, ServerResponse response) {
    String name = request.path().param("name");
    sendResponse(response, name);
  }

  private void sendResponse(ServerResponse response, String name) {
    String msg = String.format("%s %s!", greeting.get(), name);

    Message message = new Message();
    message.setMessage(msg);
    response.send(message);
  }

  /**
   * Set the greeting to use in future messages.
   *
   * @param request  the server request
   * @param response the server response
   * @param message  the client message
   */
  private void updateGreeting(ServerRequest request,
                              ServerResponse response,
                              Message message) {

    if (message.getGreeting() == null) {
      Message jsonError = new Message();
      jsonError.setMessage("No greeting provided");
      response.status(Http.Status.BAD_REQUEST_400)
        .send(jsonError);
      return;
    }

    greeting.set(message.getGreeting());
    response.status(Http.Status.NO_CONTENT_204).send();
  }
}
