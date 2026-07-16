package tong.statmod.network;

import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import tong.statmod.magic.LearnedSpellState;

public record LearnedSpellEntry(String id, int level) {
    public LearnedSpellEntry {
        ResourceLocation parsed = id == null ? null : ResourceLocation.tryParse(id);
        if (parsed == null
                || id.getBytes(StandardCharsets.UTF_8).length > LearnedSpellState.MAX_ID_LENGTH
                || level <= 0) {
            throw new IllegalArgumentException("invalid learned spell entry");
        }
        id = parsed.toString();
    }
}
