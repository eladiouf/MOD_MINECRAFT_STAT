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

    // Doors
    public static Block ironJailDoor() {
        return resolve("mcwdoors:iron_jail_door");
    }

    public static Block whisperOakDoor() {
        return resolve("mcwdoors:whisper_oak_door");
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

    // Lights
    public static Block chainLamp() {
        return resolve("mcwlights:chain_lamp");
    }

    public static Block wallLantern() {
        return resolve("mcwlights:wall_lantern");
    }

    public static Block stoneLamp() {
        return resolve("mcwlights:stone_lamp");
    }
}
