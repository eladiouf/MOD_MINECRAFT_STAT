package tong.statmod;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RequiredProviderSmokeContractTest {
    @Test
    void smokeLoadsIronAndEveryMandatoryLibrary() throws Exception {
        String source = Files.readString(Path.of("scripts/smoke-gametest-server.ps1"));

        assertTrue(source.contains("irons_spellbooks-1.20.1-3.16.2.jar"));
        assertTrue(source.contains("irons_lib-1.20.1-2.1.0.jar"));
        assertTrue(source.contains("curios-forge-5.14.1+1.20.1.jar"));
        assertTrue(source.contains("geckolib-forge-1.20.1-4.8.4.jar"));
        assertTrue(source.contains("lootr-forge-1.20-0.7.35.94.jar"));
        assertTrue(source.contains("player-animation-lib-forge-1.0.2-rc1+1.20.jar"));
        assertTrue(source.contains("fg.deobf('local:irons_spellbooks:3.16.2')"));
        assertTrue(source.contains("fg.deobf('local:irons_lib:2.1.0')"));
        assertTrue(source.contains("fg.deobf('local:curios:5.14.1')"));
        assertTrue(source.contains("fg.deobf('local:geckolib:4.8.4')"));
        assertTrue(source.contains("fg.deobf('local:playeranimator:1.0.2-rc1')"));
        assertTrue(source.contains("fg.deobf('local:lootr:0.7.35.94')"));
    }
}
