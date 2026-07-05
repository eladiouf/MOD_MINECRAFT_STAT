package tong.statmod.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssemblyWeaponCoverageResourcesTest {

    private static final Path LIBS = Paths.get("libs");
    private static final Path ASSEMBLY = Paths.get("src", "main", "resources", "data", "statmod", "recipe", "assembly");
    private static final Set<String> EXCLUDED_MODS = Set.of("minecraft", "overgeared", "statmod");
    private static final Set<String> STATION_MANAGED = Set.of(
            "simplyswords:runic_rapier",
            "simplyswords:runic_katana",
            "simplyswords:runic_claymore",
            "simplyswords:runic_spear",
            "simplyswords:runic_longsword",
            "simplyswords:runic_glaive",
            "simplyswords:runic_greathammer",
            "simplyswords:runic_greataxe"
    );

    private static final Pattern WEAPON_MODEL_HINT = Pattern.compile(
            "(sword|katana|rapier|cutlass|saber|sabre|scimitar|claymore|estoc|zweihander|bastardsword|longsword|short_sword|shortsword|great_sword|greatsword|broadsword|flamebladedsword|katzbalger|messer|falchion|sax|tachi|kodachi|odachi|wakizashi|gladius|warglaive|spear|halberd|glaive|polearm|pike|naginata|ranseur|trident|lance|ahlspiess|concavehalberd|chivalrylance|partizan|guisarme|fauchard|bardiche|voulge|axe|hammer|mace|warhammer|warmaul|maul|heavymace|heavywarhammer|lochaberaxe|battleaxe|tabar|tomahawk|lucernhammer|greataxe|dagger|knife|shuriken|kunai|stylet|stiletto|dirk|tanto|kris|misericorde|cinquedea|wand|staff|scepter|sceptre|rod|bow|crossbow|florett|milady|sickle|bat|glove|fist|bokken|uchigatana|morgenstern|club)"
    );
    private static final Pattern MODEL_VARIANT_EXCLUDE = Pattern.compile(
            "(^|_)(pulling_[0-9]+|blocking|raised|gui|desc|tooltip|exp|powers|firework|spectral_arrow|arrow|model|item|empty|sheath|handheld|inactive)$"
    );
    private static final Pattern NON_WEAPON_EXCLUDE = Pattern.compile(
            "(spawn_egg|helmet|chestplate|leggings|boots|pattern|decoration|template|tome|manual|schematic|haft|glider|ingot|nugget|soul|curio|example_loot_bag|example_passive_ability_spellbook)"
    );

    @Test
    void everyDetectedWeaponModel_hasAssemblyOrStationRecipe() throws IOException {
        List<String> missing = new ArrayList<>();

        try (Stream<Path> jars = Files.list(LIBS)) {
            for (Path jarPath : jars.filter(path -> path.toString().endsWith(".jar")).sorted().toList()) {
                try (ZipFile zip = new ZipFile(jarPath.toFile())) {
                    String modId = readModId(zip);
                    if (modId == null || EXCLUDED_MODS.contains(modId)) {
                        continue;
                    }
                    for (String itemPath : listWeaponModels(zip, modId)) {
                        String fullId = modId + ":" + itemPath;
                        if (STATION_MANAGED.contains(fullId)) {
                            continue;
                        }
                        Path recipePath = ASSEMBLY.resolve(modId).resolve(itemPath + ".json");
                        if (!Files.exists(recipePath)) {
                            missing.add(fullId);
                        }
                    }
                }
            }
        }

        assertTrue(missing.isEmpty(), "Missing generated weapon recipes: " + missing);
    }

    private static String readModId(ZipFile zip) throws IOException {
        ZipEntry entry = zip.getEntry("META-INF/neoforge.mods.toml");
        if (entry == null) {
            return null;
        }
        String toml = new String(zip.getInputStream(entry).readAllBytes());
        int idx = toml.indexOf("modId");
        if (idx < 0) {
            return null;
        }
        int quoteStart = toml.indexOf('"', idx);
        int quoteEnd = quoteStart >= 0 ? toml.indexOf('"', quoteStart + 1) : -1;
        if (quoteStart < 0 || quoteEnd < 0) {
            return null;
        }
        return toml.substring(quoteStart + 1, quoteEnd);
    }

    private static List<String> listWeaponModels(ZipFile zip, String modId) {
        String prefix = "assets/" + modId + "/models/item/";
        List<String> items = new ArrayList<>();
        zip.stream()
                .map(ZipEntry::getName)
                .filter(name -> name.startsWith(prefix) && name.endsWith(".json"))
                .map(name -> name.substring(prefix.length(), name.length() - 5))
                .filter(name -> !name.contains("/"))
                .filter(name -> WEAPON_MODEL_HINT.matcher(name).find())
                .filter(name -> !MODEL_VARIANT_EXCLUDE.matcher(name).find())
                .filter(name -> !NON_WEAPON_EXCLUDE.matcher(name).find())
                .distinct()
                .sorted()
                .forEach(items::add);
        return items;
    }
}
