package io.alpenglow.loom;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    try (final var tasks = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int tries = 0; tries < 10; tries++) {
        out.printf("Try number %s%n", tries + 1);
        var now = System.currentTimeMillis();

        for (int threads = 0; threads < 1_000_000; threads++) {
          tasks.submit((Sneaky) () -> Thread.sleep(1000));
        }

        out.printf("Elapsed time: %s%n", currentTimeMillis() - now);
      }

      tasks.shutdown();
      tasks.awaitTermination(1, HOURS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
