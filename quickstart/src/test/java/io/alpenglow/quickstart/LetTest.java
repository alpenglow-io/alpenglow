package io.alpenglow.quickstart;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LetTest {
  @BeforeAll
  static void beforeAll() {
    System.out.println("Yeeeh!");
  }

  @AfterAll
  static void afterAll() {
    System.out.println("Damn it!");
  }

  @BeforeEach
  void setUp() {
    System.out.println("Hi there!");
  }

  @AfterEach
  void tearDown() {
    System.out.println("Goodbye!");
  }

  @Test
  @DisplayName("should test something")
  void shouldTestSomething() {
    System.out.println("So hecking great!");
  }

  @Test
  void hadoken() {
    System.out.println("Hadoken!");
  }
}
