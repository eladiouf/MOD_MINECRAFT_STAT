package tong.statmod.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.stats.MobStatEffectApplier;
import tong.statmod.stats.MobStatInitializer;
import tong.statmod.STATMod;
import tong.statmod.challenge.DailyChallenge;
import tong.statmod.fatigue.FatigueManager;
import tong.statmod.fatigue.FatigueProvider;
import tong.statmod.perks.PerkManager;
import tong.statmod.perks.PerkProvider;
import tong.statmod.party.PartyManager;
import tong.statmod.skills.IdentitySkill;
import tong.statmod.skills.NonCombatSkill;
import tong.statmod.skills.StatActiveSkill;
import tong.statmod.stats.StatEffectApplier;
import tong.statmod.weapon.WeaponMasteryManager;
import tong.statmod.weapon.WeaponMasteryProvider;
import tong.statmod.world.thirst.ThirstManager;
import tong.statmod.anticheat.ServerValidator;
import tong.statmod.capability.CapabilityHelper;
import tong.statmod.item.ModItems;
import tong.statmod.world.thirst.ThirstProvider;

@Mod.EventBusSubscriber(modid = STATMod.MODID)
public class CapabilityHandler {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerStats.class);
        event.register(FatigueManager.class);
        event.register(WeaponMasteryManager.class);
        event.register(ThirstManager.class);
        event.register(PerkManager.class);
        event.register(MobStats.class);
        event.register(MobSkillState.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "player_stats"),
                new PlayerStatsProvider());
            event.addCapability(
                ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "fatigue"),
                new FatigueProvider());
            event.addCapability(
                ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "thirst"),
                new ThirstProvider());
            event.addCapability(
                ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "perks"),
                new PerkProvider());
            event.addCapability(
                ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "weapon_mastery"),
                new WeaponMasteryProvider());
        }
        if (event.getObject() instanceof Mob) {
            event.addCapability(
                ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "mob_stats"),
                new MobStatsProvider());
            event.addCapability(
                ResourceLocation.fromNamespaceAndPath(STATMod.MODID, "mob_skill_state"),
                new MobSkillStateProvider());
        }
    }

    @SubscribeEvent
    public static void onMobJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;
        if (mob.getPersistentData().getBoolean("statmod_stats_initialized")) return;
        mob.getPersistentData().putBoolean("statmod_stats_initialized", true);

        // When L2H is present, L2HostilityMobSync handles initialization.
        if (ModList.get().isLoaded("l2hostility")) return;

        MobStatInitializer.applyDefaults(mob);
        MobStatEffectApplier.applyAllBonuses(mob);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            CapabilityHelper.withStats((Player) event.getOriginal(), oldStats -> {
                CapabilityHelper.withStats((Player) event.getEntity(), newStats -> {
                    newStats.copyFrom(oldStats);
                });
            });
            CapabilityHelper.withFatigue((Player) event.getOriginal(), oldFatigue -> {
                CapabilityHelper.withFatigue((Player) event.getEntity(), newFatigue -> {
                    newFatigue.reset();
                });
            });
            CapabilityHelper.withPerks((Player) event.getOriginal(), oldPerks -> {
                CapabilityHelper.withPerks((Player) event.getEntity(), newPerks -> {
                    newPerks.deserializeNBT(oldPerks.serializeNBT());
                });
            });
            CapabilityHelper.withWeaponMastery((Player) event.getOriginal(), oldWeapon -> {
                CapabilityHelper.withWeaponMastery((Player) event.getEntity(), newWeapon -> {
                    newWeapon.deserializeNBT(oldWeapon.serializeNBT());
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            StatEffectApplier.applyAllBonuses(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerFirstJoin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            var data = player.getPersistentData();
            if (!data.getBoolean("statmod_received_book")) {
                data.putBoolean("statmod_received_book", true);
                player.addItem(new net.minecraft.world.item.ItemStack(ModItems.WELCOME_BOOK.get()));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            java.util.UUID uuid = player.getUUID();
            DailyChallenge.cleanup(uuid);
            PartyManager.cleanup(uuid);
            NonCombatSkill.clearCooldowns(uuid);
            IdentitySkill.clearCooldowns(uuid);
            StatActiveSkill.clearCooldowns(uuid);
            ServerValidator.cleanup(uuid);
        }
        entity.getCapability(PlayerStatsProvider.PLAYER_STATS).invalidate();
        entity.getCapability(FatigueProvider.FATIGUE).invalidate();
        entity.getCapability(ThirstProvider.THIRST).invalidate();
        entity.getCapability(PerkProvider.PERKS).invalidate();
        entity.getCapability(WeaponMasteryProvider.WEAPON_MASTERY).invalidate();
    }
}
