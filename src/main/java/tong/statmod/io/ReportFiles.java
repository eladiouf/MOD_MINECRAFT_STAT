package tong.statmod.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class ReportFiles {
    private ReportFiles() {
    }

    public static void writeUtf8(File target, String contents) throws IOException {
        File parent = target.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        Files.writeString(target.toPath(), contents, StandardCharsets.UTF_8);
    }
}
