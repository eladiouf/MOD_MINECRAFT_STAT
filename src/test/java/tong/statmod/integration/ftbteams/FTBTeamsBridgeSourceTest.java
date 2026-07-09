package tong.statmod.integration.ftbteams;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Intégration FTB Teams (2026-07-09) — verrouille le pattern de garde (jamais de classe FTB
 * chargée si le mod est absent) et le câblage équipe dans la conquête et les assists.
 */
class FTBTeamsBridgeSourceTest {

    private static final Path BRIDGE = Path.of("src", "main", "java",
            "tong", "statmod", "integration", "ftbteams", "FTBTeamsBridge.java");
    private static final Path PROGRESS = Path.of("src", "main", "java",
            "tong", "statmod", "dungeon", "DungeonProgress.java");
    private static final Path POINTS = Path.of("src", "main", "java",
            "tong", "statmod", "dungeon", "DungeonPoints.java");

    @Test
    void bridgeGuardsFtbAccessBehindModListCheck() throws IOException {
        String source = Files.readString(BRIDGE);

        assertTrue(source.contains("ModList.get().isLoaded(\"ftbteams\")"),
                "le bridge doit vérifier la présence du mod avant tout accès");
        assertTrue(source.contains("if (!loaded()) return true;"),
                "sans FTB Teams, sameTeam doit retomber sur « tout le monde co-op »");
        assertTrue(source.contains("catch (Throwable"),
                "une erreur FTB ne doit jamais casser la conquête");
        // Les classes FTB ne doivent être référencées QUE dans la classe interne Hook.
        String beforeHook = source.substring(0, source.indexOf("class Hook"));
        assertFalse(beforeHook.contains("dev.ftb.mods"),
                "aucune classe FTB ne doit être référencée hors de Hook (classloading safe)");
    }

    @Test
    void conquestIsTeamScoped() throws IOException {
        String source = Files.readString(PROGRESS);
        assertTrue(source.contains("FTBTeamsBridge.sameTeam"),
                "la conquête partagée doit filtrer les participants par équipe FTB");
        assertTrue(source.contains("dungeon.coop.rival_conquered"),
                "les rivaux présents doivent voir la victoire adverse");
    }

    @Test
    void assistPointsAreTeamScoped() throws IOException {
        String source = Files.readString(POINTS);
        assertTrue(source.contains("FTBTeamsBridge.sameTeam"),
                "les points d'assist doivent être réservés aux coéquipiers FTB");
    }
}
