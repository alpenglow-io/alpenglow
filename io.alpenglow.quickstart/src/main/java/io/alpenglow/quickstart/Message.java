package io.alpenglow.quickstart;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class Message {

  private String message;

  private String greeting;

  public Message() {
  }

  public String getMessage() {
    return this.message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @JsonInclude(Include.NON_NULL)
  public String getGreeting() {
    return this.greeting;
  }

  public void setGreeting(String greeting) {
    this.greeting = greeting;
  }
}
