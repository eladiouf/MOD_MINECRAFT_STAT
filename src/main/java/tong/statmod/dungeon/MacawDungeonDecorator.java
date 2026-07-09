package tong.statmod.dungeon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * Decorator to safely resolve blocks from Macaw's mod series (Bridges, Doors, Furniture, Lights, Fences).
 * Ensures zero hard dependencies and safe vanilla fallbacks.
 */
public final class MacawDungeonDecorator {

    private MacawDungeonDecorator() {}

    public static Block resolve(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc != null && BuiltInRegistries.BLOCK.containsKey(loc)) {
            return BuiltInRegistries.BLOCK.get(loc);
        }
        return null;
    }

    // Fences
    public static Block oakStockadeFence() {
        return resolve("mcwfences:oak_stockade_fence");
    }

    // Bridges
    public static Block oakRopeBridge() {
        return resolve("mcwbridges:rope_oak_bridge");
    }

    // Doors — IDs vérifiés contre le jar mcw-doors 1.1.5 (audit 2026-07-09 : les anciens ids
    // `iron_jail_door`/`whisper_oak_door` n'existaient pas → fallback fer+levier permanent).
    public static Block ironJailDoor() {
        return resolve("mcwdoors:jail_door");
    }

    public static Block whisperOakDoor() {
        return resolve("mcwdoors:oak_whispering_door");
    }

    // Furniture
    public static Block oakChair() {
        return resolve("mcwfurnitures:oak_chair");
    }

    public static Block oakDesk() {
        return resolve("mcwfurnitures:oak_desk");
    }

    public static Block oakTable() {
        return resolve("mcwfurnitures:oak_table");
    }

    // Lights — IDs vérifiés contre le jar mcw-lights 1.1.5 (`chain_lamp`/`wall_lantern`
    // n'existaient pas → jamais résolus).
    public static Block chainLamp() {
        return resolve("mcwlights:chain_lantern");
    }

    public static Block wallLantern() {
        return resolve("mcwlights:covered_wall_lantern");
    }
}
