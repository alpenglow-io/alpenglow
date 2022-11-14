package io.alpenglow.letsencrypt.func;

import java.util.function.Supplier;

@FunctionalInterface
public interface SneakySupplier<T> extends Supplier<T> {
  T tryGet() throws Throwable;

  @Override
  default T get() {
    try {
      return tryGet();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
