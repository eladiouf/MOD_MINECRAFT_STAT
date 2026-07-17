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
import tong.statmod.dungeon.party.goal.ArcherGoal;
import tong.statmod.dungeon.party.goal.AssassinAttackGoal;
import tong.statmod.dungeon.party.goal.DodgeGoal;
import tong.statmod.dungeon.party.goal.HealPartyGoal;
import tong.statmod.dungeon.party.goal.MageRangedGoal;
import tong.statmod.dungeon.party.goal.TankDefendGoal;

public final class AdventurerPartyHelper {

    /** Epic Fight présent → on utilise des bases humanoïdes qu'il patche (combos + esquives d'arme). */
    private static final boolean EPIC_FIGHT = ModList.get().isLoaded("epicfight");
    private static final java.util.Random RNG = new java.util.Random();

    /** True si l'entité est un caster natif Iron's (il lance ses vrais sorts tout seul). */
    private static boolean isIronsCaster(Mob mob) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType())
                .getNamespace().equals("irons_spellbooks");
    }

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
        spawnPartyAt(level, center, floor);
    }

    /** Spawn les 4 membres autour d'un centre donné (utilisé par le donjon et le test /statparty). */
    public static void spawnPartyAt(ServerLevel level, BlockPos center, int floor) {
        int[][] offsets = {{-4, -4}, {4, -4}, {4, 4}, {-4, 4}, {0, 6}};

        for (int i = 0; i < PartyRole.values().length; i++) {
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

            ensureRoleAi(entity);
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
                if (EPIC_FIGHT) yield EntityType.VINDICATOR; // patché Epic Fight : combos + esquive
                if (ModList.get().isLoaded("slu")) {
                    EntityType<?> t = ModdedMobPool.resolve("slu:knight");
                    if (t != null) yield t;
                }
                yield EntityType.ZOMBIE;
            }
            case ASSASSIN -> {
                if (EPIC_FIGHT) yield EntityType.VINDICATOR;
                if (ModList.get().isLoaded("slu")) {
                    EntityType<?> t = ModdedMobPool.resolve("slu:thief");
                    if (t != null) yield t;
                }
                yield EntityType.VINDICATOR;
            }
            case MAGE -> {
                // Vraie entité caster Iron's aléatoire → vrais sorts variés (plus « que du feu »).
                String[] casters = {
                        "irons_spellbooks:pyromancer", "irons_spellbooks:cryomancer",
                        "irons_spellbooks:electromancer", "irons_spellbooks:necromancer",
                        "irons_spellbooks:archevoker"
                };
                EntityType<?> t = ModdedMobPool.resolve(casters[RNG.nextInt(casters.length)]);
                if (t != null) yield t;
                yield EntityType.WITCH;
            }
            case HEALER -> EntityType.WITCH;
            case ARCHER -> {
                // Base pillager (humanoïde, non naturellement archer → seul notre ArcherGoal tire).
                if (ModList.get().isLoaded("slu")) {
                    EntityType<?> t = ModdedMobPool.resolve("slu:crossbow_hollow");
                    if (t != null) yield t;
                }
                yield EntityType.PILLAGER;
            }
        };
    }

    /**
     * (Ré)attache le goal d'IA correspondant au rôle taggé sur l'entité, de façon idempotente.
     * Les goals ajoutés par code ne sont PAS sérialisés en NBT : après un reload / déchargement de
     * chunk, un mob party garde son tag mais perd son IA. Appelé au spawn ET à chaque cycle du
     * {@link tong.statmod.dungeon.party.PartyCoordinator} → l'IA se réattache automatiquement.
     */
    public static void ensureRoleAi(Mob entity) {
        String roleName = entity.getPersistentData().getString(PartyRole.TAG);
        if (roleName.isEmpty()) return;
        PartyRole role;
        try {
            role = PartyRole.valueOf(roleName);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        Class<? extends net.minecraft.world.entity.ai.goal.Goal> goalClass = switch (role) {
            case HEALER -> HealPartyGoal.class;
            case MAGE -> MageRangedGoal.class;
            case ASSASSIN -> AssassinAttackGoal.class;
            case TANK -> TankDefendGoal.class;
            case ARCHER -> ArcherGoal.class;
        };
        boolean rolePresent = entity.goalSelector.getAvailableGoals().stream()
                .anyMatch(w -> goalClass.isInstance(w.getGoal()));
        if (!rolePresent) {
            switch (role) {
                case HEALER -> entity.goalSelector.addGoal(1, new HealPartyGoal(entity));
                case MAGE -> { if (!isIronsCaster(entity)) entity.goalSelector.addGoal(3, new MageRangedGoal(entity)); }
                case ASSASSIN -> entity.goalSelector.addGoal(2, new AssassinAttackGoal(entity));
                case TANK -> entity.goalSelector.addGoal(2, new TankDefendGoal(entity));
                case ARCHER -> entity.goalSelector.addGoal(3, new ArcherGoal(entity));
            }
        }
        // Esquive maison pour les rôles À DISTANCE (mage/archer/soigneur). La mêlée (tank/assassin)
        // esquive via Epic Fight quand il est présent — on ne lui marche pas dessus.
        boolean rangedRole = role == PartyRole.MAGE || role == PartyRole.ARCHER || role == PartyRole.HEALER;
        if (rangedRole || (!EPIC_FIGHT && role == PartyRole.ASSASSIN)) {
            boolean dodgePresent = entity.goalSelector.getAvailableGoals().stream()
                    .anyMatch(w -> w.getGoal() instanceof DodgeGoal);
            if (!dodgePresent) entity.goalSelector.addGoal(0, new DodgeGoal(entity));
        }
    }

    private static void equipForRole(Mob mob, PartyRole role, int floor) {
        switch (role) {
            case TANK -> equipTank(mob, floor);
            case ASSASSIN -> equipAssassin(mob, floor);
            case MAGE -> equipMage(mob, floor);
            case HEALER -> equipHealer(mob, floor);
            case ARCHER -> equipArcher(mob, floor);
        }
    }

    private static final String[] ARROW_TYPES = {"NORMAL", "FIRE", "POISON", "FROST"};

    private static void equipArcher(Mob mob, int floor) {
        // Type de flèche varié (feu/poison/gel), lu par ArcherGoal.
        mob.getPersistentData().putString("statmod_archer_arrow",
                ARROW_TYPES[mob.getRandom().nextInt(ARROW_TYPES.length)]);
        mob.setItemSlot(EquipmentSlot.HEAD, helmetForTier(floor));
        mob.setItemSlot(EquipmentSlot.CHEST, chestForTier(floor));
        mob.setItemSlot(EquipmentSlot.LEGS, legsForTier(floor));
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW)); // visuel : les tirs sont codés
        for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);

        double baseHp = 26.0 + floor * 0.3;
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(baseHp);

        var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.32); // mobile pour kiter

        mob.setHealth((float) baseHp);
    }

    private static ItemStack swordForTier(int floor) {
        String tier = tierPrefix(floor);
        return switch (tier) {
            case "IRON" -> new ItemStack(Items.STONE_SWORD);
            case "DIAMOND" -> new ItemStack(Items.IRON_SWORD);
            default -> new ItemStack(Items.DIAMOND_SWORD);
        };
    }

    /** Arme de mêlée variée (épée ou hache) selon le tier — variété d'équipement. */
    private static ItemStack meleeWeapon(Mob mob, int floor) {
        boolean axe = mob.getRandom().nextBoolean();
        return switch (tierPrefix(floor)) {
            case "IRON" -> new ItemStack(axe ? Items.STONE_AXE : Items.STONE_SWORD);
            case "DIAMOND" -> new ItemStack(axe ? Items.IRON_AXE : Items.IRON_SWORD);
            default -> new ItemStack(axe ? Items.DIAMOND_AXE : Items.DIAMOND_SWORD);
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
        mob.setItemSlot(EquipmentSlot.MAINHAND, meleeWeapon(mob, floor));
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
        equipIronsSet(mob, "cultist", 0x202028); // tenue de l'ombre (fallback cuir noir)
        mob.setItemSlot(EquipmentSlot.MAINHAND, meleeWeapon(mob, floor));
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

    /** Vrai staff / spellbook (Iron's & co., soft-resolus) au lieu du livre vanilla ; fallback baguette. */
    private static ItemStack mageWeapon() {
        String[] ids = {
                "irons_spellbooks:blank_staff", "irons_spellbooks:ancient_staff",
                "irons_spellbooks:netherite_spell_book", "irons_spellbooks:diamond_spell_book",
                "gtbcs_spell_lib:gsl_example_crit_staff", "wind_spellbooks:blank_staff"
        };
        for (String id : ids) {
            net.minecraft.resources.ResourceLocation loc = net.minecraft.resources.ResourceLocation.tryParse(id);
            if (loc != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(loc)) {
                return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc));
            }
        }
        return new ItemStack(Items.BLAZE_ROD); // fallback : ressemble à un bâton
    }

    /** Écoles de mage : chacune a une couleur de robe + un jeu de sorts (voir MageRangedGoal). */
    private static final String[] MAGE_ELEMENTS = {"FIRE", "FROST", "STORM", "NECRO", "ARCANE"};

    private static void equipMage(Mob mob, int floor) {
        double baseHp = 30.0 + floor * 0.4;
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(baseHp);
        mob.setHealth((float) baseHp);

        if (isIronsCaster(mob)) {
            // Caster Iron's natif : il lance ses VRAIS sorts et porte son armure d'origine.
            for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);
            return;
        }

        // Fallback (Iron's absent) : witch en robe teintée, sorts custom via MageRangedGoal.
        String element = MAGE_ELEMENTS[mob.getRandom().nextInt(MAGE_ELEMENTS.length)];
        mob.getPersistentData().putString("statmod_mage_element", element);
        int color = switch (element) {
            case "FIRE" -> 0xB31A1A;
            case "FROST" -> 0x37A6E6;
            case "STORM" -> 0xE0C020;
            case "NECRO" -> 0x1E1E28;
            default -> 0x8A28C8;
        };
        mob.setItemSlot(EquipmentSlot.HEAD, dyedLeather(Items.LEATHER_HELMET, color));
        mob.setItemSlot(EquipmentSlot.CHEST, dyedLeather(Items.LEATHER_CHESTPLATE, color));
        mob.setItemSlot(EquipmentSlot.LEGS, dyedLeather(Items.LEATHER_LEGGINGS, color));
        mob.setItemSlot(EquipmentSlot.FEET, dyedLeather(Items.LEATHER_BOOTS, color));
        mob.setItemSlot(EquipmentSlot.MAINHAND, mageWeapon());
        for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);
        var armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(Math.min(20.0, 6.0 + floor * 0.15));
    }

    private static ItemStack dyedLeather(net.minecraft.world.item.Item item, int color) {
        ItemStack stack = new ItemStack(item);
        if (item instanceof net.minecraft.world.item.DyeableLeatherItem dye) {
            dye.setColor(stack, color);
        }
        return stack;
    }

    /** Premier item existant parmi les ids (mods installés), sinon le fallback. */
    private static ItemStack resolveItem(String[] ids, ItemStack fallback) {
        for (String id : ids) {
            net.minecraft.resources.ResourceLocation loc = net.minecraft.resources.ResourceLocation.tryParse(id);
            if (loc != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(loc)) {
                return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc));
            }
        }
        return fallback;
    }

    /** Équipe un set d'armure Iron's Spellbooks par préfixe (fallback cuir teinté par pièce). */
    private static void equipIronsSet(Mob mob, String prefix, int leatherColor) {
        mob.setItemSlot(EquipmentSlot.HEAD, resolveItem(
                new String[]{"irons_spellbooks:" + prefix + "_helmet"}, dyedLeather(Items.LEATHER_HELMET, leatherColor)));
        mob.setItemSlot(EquipmentSlot.CHEST, resolveItem(
                new String[]{"irons_spellbooks:" + prefix + "_chestplate"}, dyedLeather(Items.LEATHER_CHESTPLATE, leatherColor)));
        mob.setItemSlot(EquipmentSlot.LEGS, resolveItem(
                new String[]{"irons_spellbooks:" + prefix + "_leggings"}, dyedLeather(Items.LEATHER_LEGGINGS, leatherColor)));
        mob.setItemSlot(EquipmentSlot.FEET, resolveItem(
                new String[]{"irons_spellbooks:" + prefix + "_boots"}, dyedLeather(Items.LEATHER_BOOTS, leatherColor)));
    }

    private static void equipHealer(Mob mob, int floor) {
        // Set de prêtre Iron's (fallback cuir clair) + bâton (soin fait en code).
        equipIronsSet(mob, "priest", 0xF0EAD0);
        mob.setItemSlot(EquipmentSlot.MAINHAND, resolveItem(
                new String[]{"irons_spellbooks:graybeard_staff"}, mageWeapon()));
        for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);

        double baseHp = 40.0 + floor * 0.4;
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(baseHp);

        double baseArmor = Math.min(20.0, 8.0 + floor * 0.15);
        var armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(baseArmor);

        // Ne traîne plus derrière : suit le groupe et rejoint vite les blessés.
        var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.30);

        mob.setHealth((float) baseHp);
    }
}
