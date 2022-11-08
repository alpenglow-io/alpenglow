package io.alpenglow.loom;

import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;
import static java.util.concurrent.TimeUnit.HOURS;

interface Sneaky extends Runnable {
  void sneak() throws Throwable;

  @Override
  default void run() {
    try {
      sneak();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}

interface Main {
  static void main(String[] args) {
    for (int tries = 0; tries < 30; tries++) {
      final var mills = currentTimeMillis();
      try (final var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
        for (int index = 0; index < 1_000_000; index++) {
          tasks.submit((Sneaky) () -> LockSupport.parkNanos(1_000_000_000L));
        }
        tasks.shutdown();
        tasks.awaitTermination(1, HOURS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      out.printf("Total: %s%n", currentTimeMillis() - mills);
    }
  }
}
