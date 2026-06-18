package tong.statmod.integration.puffish;

import tong.statmod.stats.StatFamily;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        for (StatFamily family : StatFamily.values()) {
            PuffishFamilyTreeBuilder.GeneratedCategoryFiles files = PuffishFamilyTreeBuilder.categoryFiles(family);
            Path familyRoot = categoriesRoot.resolve(family.slug);
            Files.createDirectories(familyRoot);
            Files.writeString(familyRoot.resolve("category.json"), files.categoryJson());
            Files.writeString(familyRoot.resolve("skills.json"), files.skillsJson());
            Files.writeString(familyRoot.resolve("definitions.json"), files.definitionsJson());
            Files.writeString(familyRoot.resolve("connections.json"), files.connectionsJson());
        }
    }
}
