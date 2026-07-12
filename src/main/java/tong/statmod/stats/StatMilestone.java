package tong.statmod.stats;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import tong.statmod.sound.ModSounds;

/**
 * Milestones visuels et sonores déclenchés aux paliers de niveau :
 * <ul>
 *   <li>Lv 10 — Apprenti (message actionbar doré)</li>
 *   <li>Lv 25 — Adepte (message + buff temporaire 30s)</li>
 *   <li>Lv 50 — Expert (message chat + regen burst)</li>
 *   <li>Lv 75 — Maître (message chat + effets renforcés)</li>
 *   <li>Lv 100 — Légende (message global serveur + effets max)</li>
 * </ul>
 *
 * <p>Appelé par {@link tong.statmod.integration.RaceEffectApplier#finalizeLevelProgression}
 * pour chaque level-up.
 */
public final class StatMilestone {
    private StatMilestone() {}

    /**
     * Vérifie si un level-up atteint un milestone et déclenche les effets.
     *
     * @param player le joueur
     * @param statIndex l'index de la stat
     * @param newLevel le nouveau niveau atteint (base, sans race bonus)
     */
    public static void check(Player player, int statIndex, int newLevel) {
        if (!(player instanceof ServerPlayer sp)) return;

        StatType stat = StatType.byIndex(statIndex);
        if (stat == null) return;

        switch (newLevel) {
            case 10 -> onApprentice(sp, stat);
            case 25 -> onAdept(sp, stat);
            case 50 -> onExpert(sp, stat);
            case 75 -> onMaster(sp, stat);
            case 100 -> onLegend(sp, stat);
            default -> { /* pas un milestone */ }
        }
    }

    private static void onApprentice(ServerPlayer player, StatType stat) {
        player.sendSystemMessage(Component.literal(
                "§6§l✦ APPRENTI §r§e" + stat.displayName + " §7— Niveau 10 atteint !"));
        playMilestoneSound(player, 1);
    }

    private static void onAdept(ServerPlayer player, StatType stat) {
        player.sendSystemMessage(Component.literal(
                "§b§l✦✦ ADEPTE §r§3" + stat.displayName + " §7— Niveau 25 atteint !"));
        playMilestoneSound(player, 2);
        // Buff de célébration — 30 secondes de regen + speed.
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0, false, true));
    }

    private static void onExpert(ServerPlayer player, StatType stat) {
        player.sendSystemMessage(Component.literal(
                "§d§l✦✦✦ EXPERT §r§5" + stat.displayName + " §7— Niveau 50 atteint !"));
        playMilestoneSound(player, 3);
        // Burst de régénération.
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, true));
    }

    private static void onMaster(ServerPlayer player, StatType stat) {
        player.sendSystemMessage(Component.literal(
                "§c§l✦✦✦✦ MAÎTRE §r§4" + stat.displayName + " §7— Niveau 75 atteint !"));
        // Annonce à tous les joueurs du serveur.
        Component broadcast = Component.literal(
                "§6⚜ §c" + player.getGameProfile().getName() + " §7est devenu §c§lMaître §r§7en §f"
                        + stat.displayName + " §7!");
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            other.sendSystemMessage(broadcast);
        }
        playMilestoneSound(player, 4);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1, false, true));
    }

    private static void onLegend(ServerPlayer player, StatType stat) {
        // Annonce globale légendaire.
        Component broadcast = Component.literal(
                "§e§l★ LÉGENDE ★ §r§6" + player.getGameProfile().getName()
                        + " §7a maîtrisé §e§l" + stat.displayName + " §7au niveau §e§l100 §7!");
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            other.sendSystemMessage(broadcast);
        }
        playMilestoneSound(player, 5);
        // Effets max — 60 secondes de buffs puissants.
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0, false, true));
    }

    private static void playMilestoneSound(ServerPlayer player, int tier) {
        switch (tier) {
            case 1 -> { // Apprenti (Lv 10)
                player.playNotifySound(ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 1.0f, 1.1f);
            }
            case 2 -> { // Adepte (Lv 25)
                player.playNotifySound(ModSounds.STAT_UP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                player.playNotifySound(ModSounds.PERK_UNLOCK.get(), SoundSource.PLAYERS, 0.7f, 1.2f);
            }
            case 3 -> { // Expert (Lv 50)
                player.playNotifySound(ModSounds.DUNGEON_FLOOR_COMPLETE.get(), SoundSource.PLAYERS, 1.0f, 1.1f);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.1f);
            }
            case 4 -> { // Maître (Lv 75)
                player.playNotifySound(ModSounds.DUNGEON_BOSS_KILL.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            case 5 -> { // Légende (Lv 100)
                player.playNotifySound(ModSounds.DUNGEON_BOSS_KILL.get(), SoundSource.PLAYERS, 1.2f, 0.8f);
                player.playNotifySound(ModSounds.DUNGEON_PORTAL_ENTER.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2f, 0.9f);
            }
            default -> {
                player.playNotifySound(ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
    }
}
