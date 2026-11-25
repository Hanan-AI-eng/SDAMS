package util;

import service.Reportable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Saves any Reportable to data/exports/ (extra feature, owner: S1).
 * Polymorphism: works for StaffReport, RevenueReport or any future report.
 */
public final class ReportExporter {

    private ReportExporter() {
    }

    public static Path export(Reportable report) throws IOException {
        Path dir = Paths.get(FileHandler.DATA_DIR, "exports");
        Files.createDirectories(dir);
        String safeTitle = report.getTitle().replaceAll("[^A-Za-z0-9]+", "_").replaceAll("_+$", "");
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path file = dir.resolve(safeTitle + "_" + stamp + ".txt");
        Files.write(file, report.toReportLines(), StandardCharsets.UTF_8);
        return file;
    }
}
