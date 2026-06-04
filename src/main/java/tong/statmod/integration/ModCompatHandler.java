package tong.statmod.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.STATMod;
import tong.statmod.progression.ActionXpHelper;
import tong.statmod.stats.StatType;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class ModCompatHandler {

    private static boolean apotheosisLoaded, farmersDelightLoaded, ironsSpellsLoaded;
    private static boolean arsNouveauLoaded, tetraLoaded, tinkersLoaded;
    private static boolean cataclysmLoaded, waystonesLoaded, alexMobsLoaded;
    private static boolean corpseLoaded, aquamiraeLoaded, createLoaded;

    static {
        apotheosisLoaded = ModList.get().isLoaded("apotheosis");
        farmersDelightLoaded = ModList.get().isLoaded("farmersdelight");
        ironsSpellsLoaded = ModList.get().isLoaded("irons_spellbooks");
        arsNouveauLoaded = ModList.get().isLoaded("ars_nouveau");
        tetraLoaded = ModList.get().isLoaded("tetra");
        tinkersLoaded = ModList.get().isLoaded("tconstruct");
        cataclysmLoaded = ModList.get().isLoaded("cataclysm");
        waystonesLoaded = ModList.get().isLoaded("waystones");
        alexMobsLoaded = ModList.get().isLoaded("alexsmobs");
        corpseLoaded = ModList.get().isLoaded("corpse");
        aquamiraeLoaded = ModList.get().isLoaded("aquamirae") || ModList.get().isLoaded("ob_aquamirae");
        createLoaded = ModList.get().isLoaded("create");
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (!apotheosisLoaded) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity killed = event.getEntity();

        if (killed.getMaxHealth() > 300 || killed.getType().toString().contains("apotheosis")) {
            ActionXpHelper.awardXp(player, StatType.BRUTE_FORCE.index, ActionXpHelper.XpTier.RARE);
            ActionXpHelper.awardXp(player, StatType.BLADE_TECHNIQUE.index, ActionXpHelper.XpTier.INTERMEDIATE);
            ActionXpHelper.awardXp(player, StatType.PHYSICAL_ENDURANCE.index, ActionXpHelper.XpTier.COMMON);
        }
    }

    @SubscribeEvent
    public static void onApotheosisDrops(LivingDropsEvent event) {
        if (!apotheosisLoaded) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity killed = event.getEntity();

        if (killed.getMaxHealth() > 300 || killed.getType().toString().contains("apotheosis")) {
            for (ItemEntity drop : event.getDrops()) {
                String name = drop.getItem().getItem().toString();
                if (name.contains("gem") || name.contains("affix")) {
                    ActionXpHelper.awardXp(player, StatType.ERUDITION.index, ActionXpHelper.XpTier.COMMON);
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!farmersDelightLoaded) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String itemName = event.getCrafting().getItem().toString();

        if (itemName.contains("farmersdelight") && (itemName.contains("feast") || itemName.contains("stuffed")
            || itemName.contains("pasta") || itemName.contains("steak") || itemName.contains("roast"))) {
            ActionXpHelper.awardXp(player, StatType.COOKING.index, ActionXpHelper.XpTier.INTERMEDIATE);
        } else if (itemName.contains("farmersdelight") && itemName.contains("knife")) {
            ActionXpHelper.awardXp(player, StatType.FORGING.index, ActionXpHelper.XpTier.COMMON);
        }
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!farmersDelightLoaded) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        String blockName = block.toString();

        if (blockName.contains("cooking_pot") || blockName.contains("skillet") || blockName.contains("stove")) {
            ActionXpHelper.awardXp(player, StatType.COOKING.index, ActionXpHelper.XpTier.COMMON);
        }
    }

    @SubscribeEvent
    public static void onLivingHurtMagic(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!ironsSpellsLoaded && !arsNouveauLoaded) return;

        boolean isMagicDamage = false;
        String sourceName = event.getSource().getMsgId();
        if (sourceName.contains("spell") || sourceName.contains("magic") || sourceName.contains("fire")
            || sourceName.contains("arcane") || sourceName.contains("lightning")
            || sourceName.contains("ice") || sourceName.contains("blood")) {
            isMagicDamage = true;
        }

        if (isMagicDamage) {
            ActionXpHelper.awardXp(player, StatType.ARCANE_POWER.index, ActionXpHelper.XpTier.INTERMEDIATE);
            float damage = event.getAmount();
            if (damage > 10) {
                if (sourceName.contains("fire")) ActionXpHelper.awardXp(player, StatType.FIRE_AFFINITY.index, ActionXpHelper.XpTier.COMMON);
                else if (sourceName.contains("ice") || sourceName.contains("water")) ActionXpHelper.awardXp(player, StatType.WATER_AFFINITY.index, ActionXpHelper.XpTier.COMMON);
                else if (sourceName.contains("lightning")) ActionXpHelper.awardXp(player, StatType.AIR_AFFINITY.index, ActionXpHelper.XpTier.COMMON);
                else ActionXpHelper.awardXp(player, StatType.ARCANE_POWER.index, ActionXpHelper.XpTier.COMMON);
            }
        }
    }

    @SubscribeEvent
    public static void onToolUse(PlayerInteractEvent.RightClickItem event) {
        if (!tetraLoaded && !tinkersLoaded) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String itemName = event.getItemStack().getItem().toString();

        if (tetraLoaded && (itemName.contains("tetra:") && (itemName.contains("hammer") || itemName.contains("workbench")))) {
            ActionXpHelper.awardXp(player, StatType.FORGING.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }
        if (tinkersLoaded && (itemName.contains("tconstruct:") && (itemName.contains("smeltery") || itemName.contains("anvil")))) {
            ActionXpHelper.awardXp(player, StatType.FORGING.index, ActionXpHelper.XpTier.INTERMEDIATE);
        }
    }

    @SubscribeEvent
    public static void onCataclysmBossDeath(LivingDeathEvent event) {
        if (!cataclysmLoaded) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        String mobId = event.getEntity().getType().toString();

        if (mobId.contains("ignis") || mobId.contains("leviathan") || mobId.contains("harbinger")
            || mobId.contains("monstrosity") || mobId.contains("ender_golem") || mobId.contains("ancient_remnant")
            || mobId.contains("void_rune") || mobId.contains("koboleton") || mobId.contains("deepling")) {

            ActionXpHelper.awardXp(player, StatType.BRUTE_FORCE.index, ActionXpHelper.XpTier.RARE);
            ActionXpHelper.awardXp(player, StatType.BLADE_TECHNIQUE.index, ActionXpHelper.XpTier.RARE);
            ActionXpHelper.awardXp(player, StatType.PHYSICAL_ENDURANCE.index, ActionXpHelper.XpTier.RARE);
            ActionXpHelper.awardXp(player, StatType.INTIMIDATION.index, ActionXpHelper.XpTier.INTERMEDIATE);
            ActionXpHelper.awardXp(player, StatType.TRACKING.index, ActionXpHelper.XpTier.INTERMEDIATE);

            event.getEntity().spawnAtLocation(new ItemStack(
                tong.statmod.item.ModItems.STAT_SCROLL.get(), 2));
            event.getEntity().spawnAtLocation(new ItemStack(
                tong.statmod.item.ModItems.MASTERY_CRYSTAL.get(), 2));
        }
    }

    @SubscribeEvent
    public static void onWaystoneActivate(PlayerInteractEvent.RightClickBlock event) {
        if (!waystonesLoaded) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        String blockName = block.toString();

        if (blockName.contains("waystone")) {
            ActionXpHelper.awardXp(player, StatType.TRACKING.index, ActionXpHelper.XpTier.INTERMEDIATE);
            ActionXpHelper.awardXp(player, StatType.KEEN_SENSES.index, ActionXpHelper.XpTier.COMMON);
        }
    }

    @SubscribeEvent
    public static void onRareMobKill(LivingDeathEvent event) {
        if (!alexMobsLoaded) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        String mobId = event.getEntity().getType().toString();

        if (mobId.contains("alexsmobs")) {
            if (mobId.contains("void_worm") || mobId.contains("bone_serpent") || mobId.contains("froststalker")
                || mobId.contains("sunbird") || mobId.contains("enderiophage") || mobId.contains("farseer")
                || mobId.contains("murmur") || mobId.contains("underminer")) {
                ActionXpHelper.awardXp(player, StatType.TRACKING.index, ActionXpHelper.XpTier.RARE);
                ActionXpHelper.awardXp(player, StatType.KEEN_SENSES.index, ActionXpHelper.XpTier.INTERMEDIATE);
            } else {
                ActionXpHelper.awardXp(player, StatType.TRACKING.index, ActionXpHelper.XpTier.COMMON);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!corpseLoaded) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        STATMod.LOGGER.debug("Corpse detected — player {} death, stats preserved", player.getGameProfile().getName());
    }

    @SubscribeEvent
    public static void onBiomeEntry(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!aquamiraeLoaded) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ActionXpHelper.awardXp(player, StatType.KEEN_SENSES.index, ActionXpHelper.XpTier.INTERMEDIATE);
    }

    @SubscribeEvent
    public static void onCreateCraft(BlockEvent.EntityPlaceEvent event) {
        if (!createLoaded) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String blockName = event.getPlacedBlock().getBlock().toString();

        if (blockName.contains("create:") && (blockName.contains("press") || blockName.contains("mixer")
            || blockName.contains("mill") || blockName.contains("deployer"))) {
            ActionXpHelper.awardXp(player, StatType.FORGING.index, ActionXpHelper.XpTier.COMMON);
            ActionXpHelper.awardXp(player, StatType.ERUDITION.index, ActionXpHelper.XpTier.COMMON);
        }
    }

    public static void logLoadedMods() {
        StringBuilder sb = new StringBuilder("Mod compat: ");
        int count = 0;
        if (apotheosisLoaded) { sb.append("Apotheosis "); count++; }
        if (farmersDelightLoaded) { sb.append("Farmer'sDelight "); count++; }
        if (ironsSpellsLoaded) { sb.append("Iron'sSpells "); count++; }
        if (arsNouveauLoaded) { sb.append("ArsNouveau "); count++; }
        if (tetraLoaded) { sb.append("Tetra "); count++; }
        if (tinkersLoaded) { sb.append("Tinkers "); count++; }
        if (cataclysmLoaded) { sb.append("Cataclysm "); count++; }
        if (waystonesLoaded) { sb.append("Waystones "); count++; }
        if (alexMobsLoaded) { sb.append("Alex'sMobs "); count++; }
        if (corpseLoaded) { sb.append("Corpse "); count++; }
        if (aquamiraeLoaded) { sb.append("Aquamirae "); count++; }
        if (createLoaded) { sb.append("Create "); count++; }
        STATMod.LOGGER.info("{}({} compatible mods)", sb.toString().trim(), count);
    }
}
