package tong.statmod.command;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

final class StatsBackupFiles {
    private StatsBackupFiles() {
    }

    static File backupDirectory() {
        return new File("config/statmod/backups");
    }

    static File backupFile(File backupDir, UUID playerId, long timestamp) {
        return new File(backupDir, playerId + "_" + timestamp + ".nbt");
    }

    static void writeBackup(File target, String contents) throws IOException {
        File parent = target.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        Files.writeString(target.toPath(), contents, StandardCharsets.UTF_8);
    }

    static Optional<File> findLatestBackup(File backupDir, UUID playerId) {
        File[] matches = backupDir.listFiles((dir, name) -> name.startsWith(playerId + "_") && name.endsWith(".nbt"));
        if (matches == null || matches.length == 0) {
            return Optional.empty();
        }

        return Arrays.stream(matches)
            .filter(file -> extractTimestamp(playerId, file.getName()).isPresent())
            .max(Comparator.comparingLong(file -> extractTimestamp(playerId, file.getName()).orElse(Long.MIN_VALUE)));
    }

    private static Optional<Long> extractTimestamp(UUID playerId, String fileName) {
        String prefix = playerId + "_";
        String suffix = ".nbt";
        if (!fileName.startsWith(prefix) || !fileName.endsWith(suffix)) {
            return Optional.empty();
        }
        String value = fileName.substring(prefix.length(), fileName.length() - suffix.length());
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
