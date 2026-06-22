package tong.statmod.integration.puffish;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

public final class PuffishFamilyTreeExporter {
    private PuffishFamilyTreeExporter() {}

    public static void main(String[] args) throws IOException {
        exportTo(Path.of("src/main/resources/data/statmod/puffish_skills"));
    }

    public static void exportTo(Path root) throws IOException {
        Files.createDirectories(root);
        Files.writeString(root.resolve("config.json"), PuffishFamilyTreeBuilder.configJson());

        Path categoriesRoot = root.resolve("categories");
        Files.createDirectories(categoriesRoot);
        purgeObsoleteCategories(categoriesRoot, Set.of("statmod_perks", "statmod_magic"));

        PuffishFamilyTreeBuilder.GeneratedCategoryFiles files = PuffishFamilyTreeBuilder.unifiedCategoryFiles();
        Path perkRoot = categoriesRoot.resolve("statmod_perks");
        Files.createDirectories(perkRoot);
        Files.writeString(perkRoot.resolve("category.json"), files.categoryJson());
        Files.writeString(perkRoot.resolve("skills.json"), files.skillsJson());
        Files.writeString(perkRoot.resolve("definitions.json"), files.definitionsJson());
        Files.writeString(perkRoot.resolve("connections.json"), files.connectionsJson());

        PuffishMagicTreeBuilder.GeneratedCategoryFiles magicFiles = PuffishMagicTreeBuilder.unifiedCategoryFiles();
        Path magicRoot = categoriesRoot.resolve("statmod_magic");
        Files.createDirectories(magicRoot);
        Files.writeString(magicRoot.resolve("category.json"), magicFiles.categoryJson());
        Files.writeString(magicRoot.resolve("skills.json"), magicFiles.skillsJson());
        Files.writeString(magicRoot.resolve("definitions.json"), magicFiles.definitionsJson());
        Files.writeString(magicRoot.resolve("connections.json"), magicFiles.connectionsJson());
    }

    private static void purgeObsoleteCategories(Path categoriesRoot, Set<String> keep) throws IOException {
        try (Stream<Path> paths = Files.list(categoriesRoot)) {
            for (Path categoryPath : paths.toList()) {
                if (!Files.isDirectory(categoryPath)) {
                    continue;
                }
                String categoryName = categoryPath.getFileName().toString();
                if (keep.contains(categoryName)) {
                    continue;
                }
                deleteRecursively(categoryPath);
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            for (Path child : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(child);
            }
        }
    }
}
