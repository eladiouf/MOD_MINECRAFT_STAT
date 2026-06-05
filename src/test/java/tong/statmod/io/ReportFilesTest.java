package tong.statmod.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void writeUtf8_createsParentDirectories() throws IOException {
        File target = tempDir.resolve("nested/reports/perf-report.json").toFile();

        ReportFiles.writeUtf8(target, "hello");

        assertTrue(target.isFile());
        assertEquals("hello", Files.readString(target.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    void writeUtf8_overwritesExistingContents() throws IOException {
        File target = tempDir.resolve("compatibility-report.txt").toFile();
        Files.writeString(target.toPath(), "old", StandardCharsets.UTF_8);

        ReportFiles.writeUtf8(target, "new");

        assertEquals("new", Files.readString(target.toPath(), StandardCharsets.UTF_8));
    }
}
