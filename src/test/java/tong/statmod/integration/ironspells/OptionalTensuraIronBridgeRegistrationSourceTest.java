package tong.statmod.integration.ironspells;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalTensuraIronBridgeRegistrationSourceTest {
    @Test
    void statModRegistersTensuraIronWrappersOnlyWhenBothOptionalModsAreLoaded() throws IOException {
        String source = Files.readString(Path.of("src/main/java/tong/statmod/STATMod.java"));

        int ironCheck = source.indexOf("boolean ironSpellsLoaded = ModList.get().isLoaded(\"irons_spellbooks\")");
        int tensuraCheck = source.indexOf("boolean tensuraLoaded = ModList.get().isLoaded(\"tensura\")");
        int wrapperRegister = source.indexOf("TensuraSpellWrapperRegistry.register(modBus)");

        assertTrue(ironCheck >= 0, "STATMod must check Iron's before touching wrapper registry");
        assertTrue(tensuraCheck >= 0, "STATMod must also check Tensura for the reverse bridge");
        assertTrue(source.contains("if (ironSpellsLoaded && tensuraLoaded)"),
                "Tensura→Iron bridge must require both optional mods");
        assertTrue(wrapperRegister > Math.max(ironCheck, tensuraCheck),
                "TensuraSpellWrapperRegistry.register must be inside the Iron's + Tensura guarded block");
    }

    @Test
    void tensuraSpellLevelModifierIsNotAutoSubscribedWithoutOptionalMods() throws IOException {
        String modifier = Files.readString(Path.of(
                "src/main/java/tong/statmod/integration/ironspells/bridge/TensuraSpellLevelModifier.java"));
        String compat = Files.readString(Path.of(
                "src/main/java/tong/statmod/STATMod.java"));

        assertFalse(modifier.contains("@EventBusSubscriber"),
                "TensuraSpellLevelModifier imports Iron's and Tensura classes, so it must not auto-subscribe globally");
        assertTrue(compat.contains("NeoForge.EVENT_BUS.register(TensuraSpellLevelModifier.class)"),
                "TensuraSpellLevelModifier must be registered from the guarded Iron's + Tensura block");
    }

    @Test
    void statModUsesLocalLoadedFlagsBeforeTouchingOptionalCompatEntrypoints() throws IOException {
        String source = Files.readString(Path.of("src/main/java/tong/statmod/STATMod.java"));

        assertTrue(source.contains("boolean tensuraLoaded = ModList.get().isLoaded(\"tensura\")"));
        assertTrue(source.contains("boolean epicFightLoaded = ModList.get().isLoaded(\"epicfight\")"));
        assertTrue(source.contains("boolean parcoolLoaded = ModList.get().isLoaded(\"parcool\")"));
        assertTrue(source.contains("boolean overgearedLoaded = ModList.get().isLoaded(\"overgeared\")"));
        assertTrue(source.contains("boolean puffishLoaded = ModList.get().isLoaded(\"puffish_skills\")"));
        assertTrue(source.contains("boolean ironSpellsLoaded = ModList.get().isLoaded(\"irons_spellbooks\")"));
        assertTrue(source.contains("if (tensuraLoaded)"));
        assertTrue(source.contains("if (epicFightLoaded)"));
        assertTrue(source.contains("if (parcoolLoaded)"));
        assertTrue(source.contains("if (overgearedLoaded)"));
        assertTrue(source.contains("if (puffishLoaded)"));
        assertTrue(source.contains("if (ironSpellsLoaded)"));
    }
}
