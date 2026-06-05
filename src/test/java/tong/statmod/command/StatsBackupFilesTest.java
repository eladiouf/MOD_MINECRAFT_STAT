package tong.statmod.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsBackupFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void findLatestBackup_selectsHighestTimestampRegardlessOfFileOrder() throws IOException {
        UUID playerId = UUID.randomUUID();
        Files.createFile(tempDir.resolve(playerId + "_100.nbt"));
        Files.createFile(tempDir.resolve(playerId + "_300.nbt"));
        Files.createFile(tempDir.resolve(playerId + "_200.nbt"));

        Optional<File> latest = StatsBackupFiles.findLatestBackup(tempDir.toFile(), playerId);

        assertTrue(latest.isPresent());
        assertEquals(playerId + "_300.nbt", latest.get().getName());
    }

    @Test
    void findLatestBackup_ignoresMalformedAndForeignFiles() throws IOException {
        UUID playerId = UUID.randomUUID();
        Files.createFile(tempDir.resolve(playerId + "_not-a-number.nbt"));
        Files.createFile(tempDir.resolve(UUID.randomUUID() + "_999.nbt"));
        Files.createFile(tempDir.resolve(playerId + "_250.nbt"));

        Optional<File> latest = StatsBackupFiles.findLatestBackup(tempDir.toFile(), playerId);

        assertTrue(latest.isPresent());
        assertEquals(playerId + "_250.nbt", latest.get().getName());
    }
}
