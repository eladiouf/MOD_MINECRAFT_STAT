package tong.statmod.dungeon.party;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

    public static boolean isPartyFloor(int floor) {
        return floor > 0 && floor % 5 != 0 && floor % 10 != 0
                && Math.floorMod(floor * 7919 + 17, 5) == 0;
    }

    public static int partyRoomIndex(int floor) {
        return 1 + Math.floorMod(floor * 31, DungeonLayout.ROOM_COUNT - 2);
    }

    private static String tierPrefix(int floor) {
        if (floor < 20) return "IRON";
        if (floor < 50) return "DIAMOND";
        if (floor < 80) return "NETHERITE";
        return "OBSIDIAN";
    }

    public static void spawnParty(ServerLevel level, BlockPos islandSpawn, int floor) {
        int roomIndex = partyRoomIndex(floor);
        DungeonLayout.Room room = DungeonLayout.rooms().get(roomIndex);
        if (room == null) return;

        int yOffset = DungeonRoomChain.roomYOffset(roomIndex, floor);
        BlockPos center = islandSpawn.offset(room.centerX(), yOffset, room.centerZ());

        int[][] offsets = {{-4, -4}, {4, -4}, {4, 4}, {-4, 4}};

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

            equipForRole(entity, role, floor);

            var followRange = entity.getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange != null) followRange.setBaseValue(48.0);

            applySpawnEffects(entity, role);

            DungeonSpawnGuard.spawnAuthorized(() -> {
                level.addFreshEntity(entity);
                return entity;
            });

            applyRoleAi(entity, role);
        }
    }

    private static void applySpawnEffects(Mob entity, PartyRole role) {
        switch (role) {
            case TANK -> entity.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE, 6000, 0, false, false));
            case ASSASSIN -> entity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, 6000, 1, false, false));
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

    private static void equipForRole(Mob mob, PartyRole role, int floor) {
        switch (role) {
            case TANK -> equipTank(mob, floor);
            case ASSASSIN -> equipAssassin(mob, floor);
            case MAGE -> equipMage(mob, floor);
            case HEALER -> equipHealer(mob, floor);
        }
    }

    private static ItemStack swordForTier(int floor) {
        String tier = tierPrefix(floor);
        return switch (tier) {
            case "IRON" -> new ItemStack(Items.STONE_SWORD);
            case "DIAMOND" -> new ItemStack(Items.IRON_SWORD);
            default -> new ItemStack(Items.DIAMOND_SWORD);
        };
    }

    private static ItemStack helmetForTier(int floor) {
        String tier = tierPrefix(floor);
        return switch (tier) {
            case "IRON" -> new ItemStack(Items.IRON_HELMET);
            case "DIAMOND" -> new ItemStack(Items.DIAMOND_HELMET);
            default -> new ItemStack(Items.NETHERITE_HELMET);
        };
    }

    private static ItemStack chestForTier(int floor) {
        String tier = tierPrefix(floor);
        return switch (tier) {
            case "IRON" -> new ItemStack(Items.IRON_CHESTPLATE);
            case "DIAMOND" -> new ItemStack(Items.DIAMOND_CHESTPLATE);
            default -> new ItemStack(Items.NETHERITE_CHESTPLATE);
        };
    }

    private static ItemStack legsForTier(int floor) {
        String tier = tierPrefix(floor);
        return switch (tier) {
            case "IRON" -> new ItemStack(Items.IRON_LEGGINGS);
            case "DIAMOND" -> new ItemStack(Items.DIAMOND_LEGGINGS);
            default -> new ItemStack(Items.NETHERITE_LEGGINGS);
        };
    }

    private static ItemStack bootsForTier(int floor) {
        String tier = tierPrefix(floor);
        return switch (tier) {
            case "IRON" -> new ItemStack(Items.IRON_BOOTS);
            case "DIAMOND" -> new ItemStack(Items.DIAMOND_BOOTS);
            default -> new ItemStack(Items.NETHERITE_BOOTS);
        };
    }

    private static void equipTank(Mob mob, int floor) {
        mob.setItemSlot(EquipmentSlot.HEAD, helmetForTier(floor));
        mob.setItemSlot(EquipmentSlot.CHEST, chestForTier(floor));
        mob.setItemSlot(EquipmentSlot.LEGS, legsForTier(floor));
        mob.setItemSlot(EquipmentSlot.FEET, bootsForTier(floor));
        mob.setItemSlot(EquipmentSlot.MAINHAND, swordForTier(floor));
        mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);

        double baseHp = 60.0 + floor * 0.5;
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(baseHp);

        var armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(Math.min(30.0, 15.0 + floor * 0.2));

        var toughness = mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null) toughness.setBaseValue(Math.min(10.0, 4.0 + floor * 0.1));

        var knockback = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) knockback.setBaseValue(1.0);

        mob.setHealth((float) baseHp);
    }

    private static void equipAssassin(Mob mob, int floor) {
        mob.setItemSlot(EquipmentSlot.HEAD, helmetForTier(floor));
        mob.setItemSlot(EquipmentSlot.CHEST, chestForTier(floor));
        mob.setItemSlot(EquipmentSlot.LEGS, legsForTier(floor));
        mob.setItemSlot(EquipmentSlot.FEET, bootsForTier(floor));
        mob.setItemSlot(EquipmentSlot.MAINHAND, swordForTier(floor));
        mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);

        var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.35);

        var dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        double baseDmg = 8.0 + floor * 0.3;
        if (dmg != null) dmg.setBaseValue(baseDmg);

        double baseHp = 24.0 + floor * 0.3;
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(baseHp);
        mob.setHealth((float) baseHp);
    }

    private static void equipMage(Mob mob, int floor) {
        mob.setItemSlot(EquipmentSlot.HEAD, helmetForTier(floor));
        mob.setItemSlot(EquipmentSlot.CHEST, chestForTier(floor));
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOOK));
        for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);

        double baseHp = 30.0 + floor * 0.4;
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(baseHp);

        double baseArmor = Math.min(20.0, 6.0 + floor * 0.15);
        var armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(baseArmor);

        mob.setHealth((float) baseHp);
    }

    private static void equipHealer(Mob mob, int floor) {
        mob.setItemSlot(EquipmentSlot.HEAD, helmetForTier(floor));
        mob.setItemSlot(EquipmentSlot.CHEST, chestForTier(floor));
        mob.setItemSlot(EquipmentSlot.LEGS, legsForTier(floor));
        mob.setItemSlot(EquipmentSlot.FEET, bootsForTier(floor));

        ItemStack healPotion = PotionUtils.setPotion(
                new ItemStack(Items.SPLASH_POTION), Potions.STRONG_HEALING);
        mob.setItemSlot(EquipmentSlot.MAINHAND, healPotion);
        for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);

        double baseHp = 40.0 + floor * 0.4;
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(baseHp);

        double baseArmor = Math.min(20.0, 8.0 + floor * 0.15);
        var armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(baseArmor);

        mob.setHealth((float) baseHp);
    }
}
