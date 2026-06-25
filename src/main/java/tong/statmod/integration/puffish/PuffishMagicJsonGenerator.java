package tong.statmod.integration.puffish;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Regénère les 4 fichiers JSON statiques que Puffish charge depuis les resources :
 * {@code category.json}, {@code skills.json}, {@code definitions.json},
 * {@code connections.json}. À lancer manuellement quand le catalog ou les descriptions sont
 * modifiés.
 *
 * <p>Usage : {@code ./gradlew runPuffishJsonGen} (alias gradle ajouté) ou directement
 * {@code java -cp build/classes tong.statmod.integration.puffish.PuffishMagicJsonGenerator}.
 *
 * <p>Écrit dans {@code src/main/resources/data/statmod/puffish_skills/categories/statmod_magic/}
 * en utilisant le répertoire courant comme racine.
 */
public final class PuffishMagicJsonGenerator {

    private static final Path TARGET_DIR = Path.of(
            "src", "main", "resources", "data", "statmod",
            "puffish_skills", "categories", "statmod_magic");

    public static void main(String[] args) throws IOException {
        PuffishMagicTreeBuilder.GeneratedCategoryFiles files =
                PuffishMagicTreeBuilder.unifiedCategoryFiles();

        Files.createDirectories(TARGET_DIR);
        write("category.json", files.categoryJson());
        write("skills.json", files.skillsJson());
        write("definitions.json", files.definitionsJson());
        write("connections.json", files.connectionsJson());

        System.out.println("Generated 4 JSON files into " + TARGET_DIR.toAbsolutePath());
    }

    private static void write(String filename, String content) throws IOException {
        Path target = TARGET_DIR.resolve(filename);
        Files.writeString(target, content);
        System.out.println("  wrote " + target + " (" + content.length() + " bytes)");
    }

    private PuffishMagicJsonGenerator() {}
}
