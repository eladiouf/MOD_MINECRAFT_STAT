package tong.statmod.integration.ironspells;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for the virtual inscription menu provider.
 * Validates the title and identity of the provider without spinning up
 * a full Minecraft client. Construction of the actual {@code InscriptionTableMenu}
 * is exercised at runtime in-game and through the existing inscription mixins.
 */
class VirtualInscriptionMenuProviderTest {
    @Test
    void provider_advertises_translatable_title_with_expected_key() {
        VirtualInscriptionMenuProvider provider = new VirtualInscriptionMenuProvider();
        Component name = provider.getDisplayName();
        assertNotNull(name);
        MutableComponent mutable = assertInstanceOf(MutableComponent.class, name);
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, mutable.getContents());
        assertEquals("statmod.menu.virtual_inscription", contents.getKey());
    }
}
