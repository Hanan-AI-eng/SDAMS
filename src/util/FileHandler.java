package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class FileHandler {

    public List<String> readAll(String filePath) throws IOException {
        Path path = Path.of(filePath);

        if (!Files.exists(path)) {
            return List.of();
        }

        return Files.readAllLines(path);
    }

    public void append(String filePath, String line) throws IOException {
        Path path = Path.of(filePath);

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        Files.writeString(
            path,
            line + System.lineSeparator(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }
}
