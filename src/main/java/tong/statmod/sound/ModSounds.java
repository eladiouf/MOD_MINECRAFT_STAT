package tong.statmod.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import tong.statmod.STATMod;

import java.util.function.Supplier;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, STATMod.MODID);

    public static final Supplier<SoundEvent> LEVEL_UP = register("level_up");
    public static final Supplier<SoundEvent> PERK_UNLOCK = register("perk_unlock");
    public static final Supplier<SoundEvent> PERK_TREE_OPEN = register("perk_tree_open");
    public static final Supplier<SoundEvent> STAT_UP = register("stat_up");
    public static final Supplier<SoundEvent> DUNGEON_PORTAL_ENTER = register("dungeon_portal_enter");
    public static final Supplier<SoundEvent> DUNGEON_BOSS_KILL = register("dungeon_boss_kill");
    public static final Supplier<SoundEvent> DUNGEON_FLOOR_COMPLETE = register("dungeon_floor_complete");

    private static Supplier<SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(STATMod.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
