package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WordStatistics {
    public static Map<String, Long> countWords(Path path, int limit) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return reader.lines()
                    .flatMap(line -> Arrays.stream(line.split("\\s+")))
                    .map(word -> word.toLowerCase().replaceAll("[^a-zA-Z0-9ąęóśćżńźĄĘÓŚĆŻŃŹ]{3,}", ""))
                    .filter(word -> word.matches("[a-zA-Z0-9ąęóśćżńźĄĘÓŚĆŻŃŹ]{3,}"))
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(limit)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (k, v) -> {
                                throw new IllegalStateException(String.format("Błąd! Duplikat klucza %s.", k));
                            },
                            LinkedHashMap::new));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
