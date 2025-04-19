package org.example;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Consumer implements Runnable {
    private final AtomicBoolean stopSignal;
    private final BlockingQueue<Optional<Path>> queue;
    private final int wordLimit;

    public Consumer(AtomicBoolean stopSignal, BlockingQueue<Optional<Path>> queue, int wordLimit) {
        this.stopSignal = stopSignal;
        this.queue = queue;
        this.wordLimit = wordLimit;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        System.out.printf("KONSUMENT %s URUCHOMIONY ...%n", name);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.printf("Konsument %s próbuje pobrać ścieżkę%n", name);
                Optional<Path> optPath = queue.take();
                if (!optPath.isPresent()) break;

                Path path = optPath.get();
                Map<String, Long> wordCount = WordStatistics.countWords(path, wordLimit);
                System.out.println("Statystyka dla pliku: " + path);
                wordCount.forEach((word, count) -> System.out.println(word + ": " + count));
            } catch (InterruptedException e) {
                System.out.printf("Oczekiwanie konsumenta %s przerwane!%n", name);
                Thread.currentThread().interrupt();
            }
        }

        System.out.printf("KONSUMENT %s ZAKOŃCZYŁ PRACĘ%n", name);
    }
}
