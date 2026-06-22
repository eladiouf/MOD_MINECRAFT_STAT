package tong.statmod.integration.puffish;

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

        PuffishFamilyTreeBuilder.GeneratedCategoryFiles files = PuffishFamilyTreeBuilder.unifiedCategoryFiles();
        Path perkRoot = categoriesRoot.resolve("statmod_perks");
        Files.createDirectories(perkRoot);
        Files.writeString(perkRoot.resolve("category.json"), files.categoryJson());
        Files.writeString(perkRoot.resolve("skills.json"), files.skillsJson());
        Files.writeString(perkRoot.resolve("definitions.json"), files.definitionsJson());
        Files.writeString(perkRoot.resolve("connections.json"), files.connectionsJson());
    }
}
