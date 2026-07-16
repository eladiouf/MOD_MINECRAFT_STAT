package tong.statmod.dungeon.party;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.fml.ModList;
import tong.statmod.dungeon.DungeonLayout;
import tong.statmod.dungeon.DungeonRoomChain;
import tong.statmod.dungeon.DungeonSpawnGuard;
import tong.statmod.dungeon.ModdedMobPool;
import tong.statmod.dungeon.party.goal.AssassinAttackGoal;
import tong.statmod.dungeon.party.goal.HealPartyGoal;
import tong.statmod.dungeon.party.goal.MageRangedGoal;
import tong.statmod.dungeon.party.goal.TankDefendGoal;

public final class AdventurerPartyHelper {

    private AdventurerPartyHelper() {}

    /** ~1 étage de combat sur 5 a une pièce groupe d'aventurier. */
    public static boolean isPartyFloor(int floor) {
        return floor > 0 && floor % 5 != 0 && floor % 10 != 0
                && Math.floorMod(floor * 7919 + 17, 5) == 0;
    }

    /** Index de la pièce qui contiendra le groupe d'aventuriers. */
    public static int partyRoomIndex(int floor) {
        return 1 + Math.floorMod(floor * 31, DungeonLayout.ROOM_COUNT - 2);
    }

    /** Invoque les 4 membres du groupe dans la pièce désignée. */
    public static void spawnParty(ServerLevel level, BlockPos islandSpawn, int floor) {
        int roomIndex = partyRoomIndex(floor);
        DungeonLayout.Room room = DungeonLayout.rooms().get(roomIndex);
        if (room == null) return;

        int yOffset = DungeonRoomChain.roomYOffset(roomIndex, floor);
        BlockPos center = islandSpawn.offset(room.centerX(), yOffset, room.centerZ());

        int[][] offsets = {{-3, -3}, {3, -3}, {3, 3}, {-3, 3}};

        for (int i = 0; i < 4; i++) {
            PartyRole role = PartyRole.byIndex(i);
            BlockPos pos = center.offset(offsets[i][0], 0, offsets[i][1]);
            EntityType<?> type = entityTypeForRole(role);
            if (type == null) continue;

            Mob entity = (Mob) type.create(level);
            if (entity == null) continue;

            entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            entity.getPersistentData().putString(PartyRole.TAG, role.name());
            entity.setPersistenceRequired();

            equipForRole(entity, role);

            var followRange = entity.getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange != null) followRange.setBaseValue(48.0);

            DungeonSpawnGuard.spawnAuthorized(() -> {
                level.addFreshEntity(entity);
                return entity;
            });

            applyRoleAi(entity, role);
        }
    }

    private static EntityType<?> entityTypeForRole(PartyRole role) {
        return switch (role) {
            case TANK -> {
                if (ModList.get().isLoaded("slu")) {
                    EntityType<?> t = ModdedMobPool.resolve("slu:knight");
                    if (t != null) yield t;
                }
                yield EntityType.ZOMBIE;
            }
            case ASSASSIN -> {
                if (ModList.get().isLoaded("slu")) {
                    EntityType<?> t = ModdedMobPool.resolve("slu:thief");
                    if (t != null) yield t;
                }
                yield EntityType.VINDICATOR;
            }
            case MAGE -> {
                if (ModList.get().isLoaded("irons_spellbooks")) {
                    EntityType<?> t = ModdedMobPool.resolve("irons_spellbooks:pyromancer");
                    if (t != null) yield t;
                }
                yield EntityType.WITCH;
            }
            case HEALER -> EntityType.WITCH;
        };
    }

    private static void applyRoleAi(Mob entity, PartyRole role) {
        switch (role) {
            case HEALER -> entity.goalSelector.addGoal(1, new HealPartyGoal(entity));
            case MAGE -> entity.goalSelector.addGoal(3, new MageRangedGoal(entity));
            case ASSASSIN -> entity.goalSelector.addGoal(2, new AssassinAttackGoal(entity));
            case TANK -> entity.goalSelector.addGoal(2, new TankDefendGoal(entity));
        }
    }

    private static void equipForRole(Mob mob, PartyRole role) {
        switch (role) {
            case TANK -> {
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
                mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);
                var hp = mob.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(60.0);
                var armor = mob.getAttribute(Attributes.ARMOR);
                if (armor != null) armor.setBaseValue(15.0);
                var toughness = mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
                if (toughness != null) toughness.setBaseValue(4.0);
                var knockback = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (knockback != null) knockback.setBaseValue(1.0);
                mob.setHealth(60.0f);
            }
            case ASSASSIN -> {
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
                mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_SWORD));
                for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);
                var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) speed.setBaseValue(0.35);
                var dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                if (dmg != null) dmg.setBaseValue(8.0);
                var hp = mob.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(24.0);
                mob.setHealth(24.0f);
            }
            case MAGE -> {
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOOK));
                for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);
                var hp = mob.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(30.0);
                var armor = mob.getAttribute(Attributes.ARMOR);
                if (armor != null) armor.setBaseValue(6.0);
                mob.setHealth(30.0f);
            }
            case HEALER -> {
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS));
                mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
                ItemStack healPotion = PotionUtils.setPotion(
                        new ItemStack(Items.SPLASH_POTION), Potions.STRONG_HEALING);
                mob.setItemSlot(EquipmentSlot.MAINHAND, healPotion);
                for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);
                var hp = mob.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(40.0);
                var armor = mob.getAttribute(Attributes.ARMOR);
                if (armor != null) armor.setBaseValue(8.0);
                mob.setHealth(40.0f);
            }
        }
    }
}
