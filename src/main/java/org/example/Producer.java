package org.example;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Producer implements Runnable {
    private final AtomicBoolean fajrant;
    private final BlockingQueue<Optional<Path>> queue;
    private final String directory;
    private final int delaySeconds;

    public Producer(AtomicBoolean fajrant, BlockingQueue<Optional<Path>> queue, String directory, int delaySeconds) {
        this.fajrant = fajrant;
        this.queue = queue;
        this.directory = directory;
        this.delaySeconds = delaySeconds;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        System.out.printf("PRODUCENT %s URUCHOMIONY ...%n", name);

        while (!Thread.currentThread().isInterrupted() && !fajrant.get()) {
            if (fajrant.get()) {
                try {
                    queue.put(Optional.empty());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                try {
                    Files.walkFileTree(Paths.get(directory), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (file.toString().endsWith(".txt")) {
                                try {
                                    queue.put(Optional.of(file));
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    return FileVisitResult.TERMINATE;
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.printf("Producent %s ponownie sprawdzi katalogi za %d sekund%n", name, delaySeconds);
            try {
                TimeUnit.SECONDS.sleep(delaySeconds);
            } catch (InterruptedException e) {
                System.out.printf("Przerwa producenta %s przerwana!%n", name);
                if (!fajrant.get()) Thread.currentThread().interrupt();
            }
        }

        System.out.printf("PRODUCENT %s SKOŃCZYŁ PRACĘ%n", name);
    }
}
