package tong.statmod.integration.ironspells;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Gère la transformation des créatures apparaissant dans le Trial Dungeon
 * en variantes de mages avec des équipements (robes de mages d'Iron's Spells)
 * et des intelligences artificielles tactiques à sorts multiples.
 */
public final class DungeonMagicMobs {

    /**
     * Garde-fou anti-doublon volontairement en mémoire (pas en NBT/persistentData) : le
     * goalSelector d'un Mob n'est JAMAIS sérialisé, contrairement à getPersistentData(). Un
     * ancien flag NBT "statmod_magic_mob" bloquait tout rechargement de mob (redémarrage,
     * déchargement de chunk) — l'entité recréée gardait nom/équipement (propriétés natives
     * persistées) mais perdait le goal de cast pour toujours, puisque le code sautait la
     * transformation entière en le voyant déjà "traité". Une WeakHashMap ne protège que contre
     * les doubles EntityJoinLevelEvent sur la MÊME instance vivante, et s'auto-nettoie au GC.
     */
    private static final Set<Mob> processedThisSession = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        // Limiter cette transformation uniquement à notre dimension du Trial Dungeon
        String dim = mob.level().dimension().location().toString();
        if (!"statmod:trial_dungeon".equals(dim)) return;

        // Lire le tag de mage custom
        String mageType = mob.getPersistentData().getString("statmod_custom_mage_type");
        if (mageType.isEmpty()) return; // Pas un mage planifié !

        // Éviter d'appliquer les objectifs ou équipements plusieurs fois SUR CETTE INSTANCE
        if (!processedThisSession.add(mob)) return;

        if ("statmod:cleric_mob".equals(mageType)) {
            transformToCleric(mob);
        } else if ("statmod:pyromancer_mob".equals(mageType)) {
            if (mob instanceof Zombie zombie) transformToPyromancer(zombie);
        } else if ("statmod:cryomancer_mob".equals(mageType)) {
            if (mob instanceof Skeleton skeleton) transformToCryomancer(skeleton);
        } else if ("statmod:electromancer_mob".equals(mageType)) {
            if (mob instanceof Skeleton skeleton) transformToElectromancer(skeleton);
        } else if ("statmod:wither_mage_mob".equals(mageType)) {
            if (mob instanceof WitherSkeleton wither) transformToWitherMage(wither);
        } else if ("statmod:dungeon_knight_fallback".equals(mageType)) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            mob.setCustomName(Component.literal("§7Chevalier de Fer"));
            mob.setCustomNameVisible(true);
        } else if ("statmod:hollow_witch_mob".equals(mageType) || "statmod:mage_knight_mob".equals(mageType) || "statmod:void_knight_mob".equals(mageType)) {
            String entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
            transformSluMob(mob, entityId);
        }
    }

    private static void transformToCleric(Mob mob) {
        mob.setCustomName(Component.literal("§aClerc de Combat"));
        mob.setCustomNameVisible(true);

        ItemStack book = getModdedItem("irons_spellbooks:priest_spellbook", Items.BOOK);
        mob.setItemSlot(EquipmentSlot.MAINHAND, book);
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.08f);

        equipFullMageSet(mob,
            "irons_spellbooks:priest_helmet",
            "irons_spellbooks:priest_chestplate",
            "irons_spellbooks:priest_leggings",
            "irons_spellbooks:priest_boots"
        );

        // Clerc : Divine Smite (basic), Wisp (heavy), No Escape, No Summon, Blessing of Life (heal), Fortify (shield)
        mob.goalSelector.addGoal(1, new DungeonMobSpellcastGoal(mob,
            DungeonMobSpellcastGoal.MageRole.HEALER,
            "irons_spellbooks:divine_smite", 1, 120,
            "irons_spellbooks:wisp", 2, 160,
            null, 0, 0,
            null, 0, 0,
            "irons_spellbooks:blessing_of_life", 2, 160,
            "irons_spellbooks:fortify", 2, 300
        ));
    }

    private static void transformToPyromancer(Zombie zombie) {
        zombie.setCustomName(Component.literal("§cZombie Pyromancien"));
        zombie.setCustomNameVisible(true);

        ItemStack book = getModdedItem("irons_spellbooks:blaze_spellbook", Items.BOOK);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, book);
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.05f);

        equipFullMageSet(zombie,
            "irons_spellbooks:pyromancer_helmet",
            "irons_spellbooks:pyromancer_chestplate",
            "irons_spellbooks:pyromancer_leggings",
            "irons_spellbooks:pyromancer_boots"
        );

        // Pyromancien : Firebolt (basic), Fireball (heavy), No Escape, No Summon, No Heal, Shield (shield)
        zombie.goalSelector.addGoal(1, new DungeonMobSpellcastGoal(zombie,
            DungeonMobSpellcastGoal.MageRole.ATTACKER,
            "irons_spellbooks:firebolt", 2, 50,
            "irons_spellbooks:fireball", 2, 160,
            null, 0, 0,
            null, 0, 0,
            null, 0, 0,
            "irons_spellbooks:shield", 1, 240
        ));
    }

    private static void transformToCryomancer(Skeleton skeleton) {
        skeleton.setCustomName(Component.literal("§bSquelette Cryomancien"));
        skeleton.setCustomNameVisible(true);

        ItemStack book = getModdedItem("irons_spellbooks:iron_spellbook", Items.BOOK);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, book);
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.05f);

        equipFullMageSet(skeleton,
            "irons_spellbooks:cryomancer_helmet",
            "irons_spellbooks:cryomancer_chestplate",
            "irons_spellbooks:cryomancer_leggings",
            "irons_spellbooks:cryomancer_boots"
        );

        // Cryomancien : Icicle (basic), Blizzard (heavy), Frost Step (escape), Summon Polar Bear (summon), No Heal, Shield (shield)
        skeleton.goalSelector.addGoal(1, new DungeonMobSpellcastGoal(skeleton,
            DungeonMobSpellcastGoal.MageRole.SUPPORT,
            "irons_spellbooks:icicle", 2, 60,
            "irons_spellbooks:blizzard", 2, 240,
            "irons_spellbooks:frost_step", 1, 200,
            "irons_spellbooks:summon_polar_bear", 1, 600,
            null, 0, 0,
            "irons_spellbooks:shield", 1, 240
        ));
    }

    private static void transformToElectromancer(Skeleton skeleton) {
        skeleton.setCustomName(Component.literal("§eSquelette Électromancien"));
        skeleton.setCustomNameVisible(true);

        ItemStack book = getModdedItem("irons_spellbooks:gold_spellbook", Items.BOOK);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, book);
        skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.05f);

        equipFullMageSet(skeleton,
            "irons_spellbooks:electromancer_helmet",
            "irons_spellbooks:electromancer_chestplate",
            "irons_spellbooks:electromancer_leggings",
            "irons_spellbooks:electromancer_boots"
        );

        // Électromancien : Chain Lightning (basic), Electrocute (heavy), Charge (escape), No Summon, No Heal, Shockwave (shield)
        skeleton.goalSelector.addGoal(1, new DungeonMobSpellcastGoal(skeleton,
            DungeonMobSpellcastGoal.MageRole.ATTACKER,
            "irons_spellbooks:chain_lightning", 1, 80,
            "irons_spellbooks:electrocute", 1, 240,
            "irons_spellbooks:charge", 1, 160,
            null, 0, 0,
            null, 0, 0,
            "irons_spellbooks:shockwave", 1, 140
        ));
    }

    private static void transformToWitherMage(WitherSkeleton wither) {
        wither.setCustomName(Component.literal("§dMage du Wither"));
        wither.setCustomNameVisible(true);

        ItemStack book = getModdedItem("irons_spellbooks:evoker_spellbook", Items.BOOK);
        wither.setItemSlot(EquipmentSlot.MAINHAND, book);
        wither.setDropChance(EquipmentSlot.MAINHAND, 0.08f);

        equipFullMageSet(wither,
            "irons_spellbooks:arcanist_helmet",
            "irons_spellbooks:arcanist_chestplate",
            "irons_spellbooks:arcanist_leggings",
            "irons_spellbooks:arcanist_boots"
        );

        // Wither Mage : Blood Slash (basic), Black Hole (heavy), Abyssal Shroud (escape), Summon Skeleton (summon), No Heal, Shield (shield)
        wither.goalSelector.addGoal(1, new DungeonMobSpellcastGoal(wither,
            DungeonMobSpellcastGoal.MageRole.SUPPORT,
            "irons_spellbooks:blood_slash", 2, 70,
            "irons_spellbooks:black_hole", 1, 400,
            "irons_spellbooks:abyssal_shroud", 1, 240,
            "irons_spellbooks:raise_dead", 2, 600,
            null, 0, 0,
            "irons_spellbooks:shield", 2, 240
        ));
    }

    private static void transformSluMob(Mob mob, String entityId) {
        if (entityId.equals("slu:hollow") || entityId.equals("slu:armed_hollow") || entityId.equals("slu:thief") || entityId.equals("slu:hollow_soldier_sword") || entityId.equals("slu:hollow_soldier_spear")) {
            mob.setCustomName(Component.literal("§8Carcasse Sorcière"));
            mob.setCustomNameVisible(true);
            
            ItemStack book = getModdedItem("irons_spellbooks:iron_spellbook", Items.BOOK);
            mob.setItemSlot(EquipmentSlot.MAINHAND, book);
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.05f);

            equipFullMageSet(mob,
                "irons_spellbooks:cultist_helmet",
                "irons_spellbooks:cultist_chestplate",
                "irons_spellbooks:cultist_leggings",
                "irons_spellbooks:cultist_boots"
            );

            // Carcasse Sorcière : Magic Arrow (basic), Lightning Bolt (heavy), No Escape, No Summon, No Heal, Shield (shield)
            mob.goalSelector.addGoal(1, new DungeonMobSpellcastGoal(mob,
                DungeonMobSpellcastGoal.MageRole.ATTACKER,
                "irons_spellbooks:magic_arrow", 2, 50,
                "irons_spellbooks:lightning_bolt", 1, 160,
                null, 0, 0,
                null, 0, 0,
                null, 0, 0,
                "irons_spellbooks:shield", 1, 240
            ));
        } else if (entityId.equals("slu:knight") || entityId.equals("slu:elite_knight") || entityId.equals("slu:dungeon_knight") || entityId.equals("slu:castle_guard") || entityId.equals("slu:noble_knight")) {
            mob.setCustomName(Component.literal("§6Chevalier Mage"));
            mob.setCustomNameVisible(true);
            
            ItemStack book = getModdedItem("irons_spellbooks:blaze_spellbook", Items.BOOK);
            mob.setItemSlot(EquipmentSlot.MAINHAND, book);
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.05f);

            ItemStack helm = getModdedItem("irons_spellbooks:pyromancer_helmet", Items.AIR);
            mob.setItemSlot(EquipmentSlot.HEAD, helm);

            // Chevalier Mage : Firebolt (basic), Blaze Storm (heavy), No Escape, No Summon, No Heal, Shield (shield)
            mob.goalSelector.addGoal(1, new DungeonMobSpellcastGoal(mob,
                DungeonMobSpellcastGoal.MageRole.ATTACKER,
                "irons_spellbooks:firebolt", 3, 60,
                "irons_spellbooks:blaze_storm", 2, 240,
                null, 0, 0,
                null, 0, 0,
                null, 0, 0,
                "irons_spellbooks:shield", 2, 240
            ));
        } else if (entityId.equals("slu:dark_knight") || entityId.equals("slu:wither_skeleton_knight") || entityId.equals("slu:ringed_knight") || entityId.equals("slu:mad_knight")) {
            mob.setCustomName(Component.literal("§5Chevalier du Néant"));
            mob.setCustomNameVisible(true);
            
            ItemStack book = getModdedItem("irons_spellbooks:evoker_spellbook", Items.BOOK);
            mob.setItemSlot(EquipmentSlot.MAINHAND, book);
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.08f);

            equipFullMageSet(mob,
                "irons_spellbooks:arcanist_helmet",
                "irons_spellbooks:arcanist_chestplate",
                "irons_spellbooks:arcanist_leggings",
                "irons_spellbooks:arcanist_boots"
            );

            // Chevalier du Néant : Blood Slash (basic), Black Hole (heavy), Abyssal Shroud (escape), Summon Skeleton (summon), No Heal, Shield (shield)
            mob.goalSelector.addGoal(1, new DungeonMobSpellcastGoal(mob,
                DungeonMobSpellcastGoal.MageRole.SUPPORT,
                "irons_spellbooks:blood_slash", 2, 70,
                "irons_spellbooks:black_hole", 1, 400,
                "irons_spellbooks:abyssal_shroud", 1, 240,
                "irons_spellbooks:raise_dead", 2, 600,
                null, 0, 0,
                "irons_spellbooks:shield", 2, 240
            ));
        }
    }

    private static void equipFullMageSet(Mob mob, String helm, String chest, String legs, String boots) {
        if (helm != null) {
            mob.setItemSlot(EquipmentSlot.HEAD, getModdedItem(helm, Items.AIR));
            mob.setDropChance(EquipmentSlot.HEAD, 0.02f);
        }
        if (chest != null) {
            mob.setItemSlot(EquipmentSlot.CHEST, getModdedItem(chest, Items.AIR));
            mob.setDropChance(EquipmentSlot.CHEST, 0.02f);
        }
        if (legs != null) {
            mob.setItemSlot(EquipmentSlot.LEGS, getModdedItem(legs, Items.AIR));
            mob.setDropChance(EquipmentSlot.LEGS, 0.02f);
        }
        if (boots != null) {
            mob.setItemSlot(EquipmentSlot.FEET, getModdedItem(boots, Items.AIR));
            mob.setDropChance(EquipmentSlot.FEET, 0.02f);
        }
    }

    private static ItemStack getModdedItem(String itemId, net.minecraft.world.item.Item fallback) {
        var key = ResourceKey.create(
            Registries.ITEM,
            ResourceLocation.parse(itemId)
        );
        var itemHolder = net.minecraft.core.registries.BuiltInRegistries.ITEM.getHolder(key);
        if (itemHolder.isPresent()) {
            return new ItemStack(itemHolder.get().value());
        }
        return new ItemStack(fallback);
    }
}
