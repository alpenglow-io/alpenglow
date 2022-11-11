package org.acme.pem;

import java.util.function.Supplier;

public interface Silent<T> extends Supplier<T> {
  T tryGet() throws Throwable;

  default T get() {
    try {
      return tryGet();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
