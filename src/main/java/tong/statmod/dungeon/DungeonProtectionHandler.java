package tong.statmod.dungeon;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import tong.statmod.StatMod;

/**
 * Mission M6 — Le Trial Dungeon est <b>indestructible</b>, par les joueurs COMME par les mobs
 * (durci 2026-07-09 sur feedback playtest).
 *
 * <p>Protections dans la dimension {@code statmod:trial_dungeon} :
 * <ol>
 *   <li>Casse de bloc annulée (sauf joueur en créatif — admin).</li>
 *   <li>Pose de bloc annulée (sauf créatif).</li>
 *   <li>Explosions : la liste des blocs affectés est vidée → creepers, blazes, TNT, boss moddés
 *       ne peuvent plus percer le sol ni les murs (les dégâts aux entités restent).</li>
 *   <li>Griefing de mob interdit ({@code EntityMobGriefingEvent}) : endermen qui volent des
 *       blocs, zombies qui cassent les portes en bois, ravagers, boules de feu qui allument des
 *       incendies, silverfish…</li>
 *   <li>{@code LivingDestroyBlockEvent} annulé : wither qui mange le bâti, zombie qui finit une
 *       porte, attaques de boss moddés passant par ce chemin.</li>
 *   <li>Outils de terrain interdits (strip de bûche, chemin à la pelle, labour) et objets
 *       incendiaires (briquet, boule de feu) bloqués en survie.</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = StatMod.MOD_ID)
public final class DungeonProtectionHandler {

    private DungeonProtectionHandler() {}

    private static boolean inDungeon(Object levelAccessor) {
        return levelAccessor instanceof Level lvl
                && lvl.dimension().equals(DungeonDimensions.TRIAL_DUNGEON);
    }

    /** Un joueur en créatif (admin) garde le droit de casser/poser pour éditer/débugger. */
    private static boolean isBuilder(Player player) {
        return player != null && player.getAbilities().instabuild;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!inDungeon(event.getLevel())) return;
        if (isBuilder(event.getPlayer())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!inDungeon(event.getLevel())) return;
        if (event.getEntity() instanceof Player p && isBuilder(p)) return;
        // Les blocs à gravité (enclume du piège, gravier des thèmes) doivent pouvoir atterrir —
        // sinon ils se cassent en item à la retombée.
        if (event.getEntity() instanceof net.minecraft.world.entity.item.FallingBlockEntity) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!inDungeon(event.getLevel())) return;
        if (isBuilder(event.getEntity())) return;

        // Interactive Lodestone blessing altars.
        // ⚠ PAS les sanctuaires de la chambre-forte (lodestone + cristal d'améthyste au-dessus) :
        // ceux-là téléportent (DungeonUltraVault.onUseLodestone) — on les laisse passer, sinon ce
        // handler consommait le clic et la chambre-forte devenait inaccessible.
        if (event.getLevel().getBlockState(event.getPos()).is(net.minecraft.world.level.block.Blocks.LODESTONE)
                && !event.getLevel().getBlockState(event.getPos().above()).is(net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER)) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
                int floor = DungeonTeleportHandler.floorAtPos(event.getPos().getX(), event.getPos().getZ());
                if (floor > 0) {
                    int lastClaimed = sp.getPersistentData().getInt("statmod:blessed_floor");
                    if (lastClaimed < floor) {
                        sp.getPersistentData().putInt("statmod:blessed_floor", floor);

                        // Potion effects: Speed II, Resistance I for 3 minutes (3600 ticks)
                        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 3600, 1));
                        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 3600, 0));

                        // Play sound & particles
                        net.minecraft.server.level.ServerLevel sl = sp.serverLevel();
                        sl.playSound(null, event.getPos(), net.minecraft.sounds.SoundEvents.TOTEM_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, event.getPos().getX() + 0.5, event.getPos().getY() + 1.2, event.getPos().getZ() + 0.5, 30, 0.2, 0.2, 0.2, 0.1);

                        sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6§l[Donjon] Bénédiction reçue ! Vitesse II & Résistance I actives."));
                    } else {
                        sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[Donjon] Vous avez déjà réclamé la bénédiction de cet étage."));
                    }
                }
            }
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        net.minecraft.world.item.ItemStack stack = event.getItemStack();
        if (!stack.isEmpty() && isForbiddenItem(stack.getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!inDungeon(event.getLevel())) return;
        if (isBuilder(event.getEntity())) return;

        net.minecraft.world.item.ItemStack stack = event.getItemStack();
        if (!stack.isEmpty() && isForbiddenItem(stack.getItem())) {
            event.setCanceled(true);
        }
    }

    private static boolean isForbiddenItem(net.minecraft.world.item.Item item) {
        if (item == null) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
        return id.contains("bucket")
                || item instanceof net.minecraft.world.item.HangingEntityItem
                || item instanceof net.minecraft.world.item.ArmorStandItem
                || item instanceof net.minecraft.world.item.BoatItem
                || item instanceof net.minecraft.world.item.FlintAndSteelItem
                || item instanceof net.minecraft.world.item.FireChargeItem;
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!inDungeon(event.getLevel())) return;
        // Les explosions ne détruisent aucun bloc du donjon (mais blessent encore les entités).
        event.getAffectedBlocks().clear();
    }

    /**
     * Griefing de mob interdit : endermen (vol de blocs), zombies (casse des portes en bois —
     * critique depuis que toutes les portes du donjon sont en bois), ravagers, boules de feu qui
     * posent du feu, silverfish qui s'incrustent, etc. Le donjon appartient au bâtisseur.
     */
    @SubscribeEvent
    public static void onMobGriefing(net.minecraftforge.event.entity.EntityMobGriefingEvent event) {
        if (event.getEntity() == null) return;
        if (!inDungeon(event.getEntity().level())) return;
        event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
    }

    /** Destruction directe de bloc par une entité vivante (wither, portes, boss moddés) : non. */
    @SubscribeEvent
    public static void onLivingDestroyBlock(net.minecraftforge.event.entity.living.LivingDestroyBlockEvent event) {
        if (!inDungeon(event.getEntity().level())) return;
        event.setCanceled(true);
    }

    /** Outils de terrain (strip de bûche, chemin à la pelle, labour à la houe) : non plus. */
    @SubscribeEvent
    public static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!inDungeon(event.getLevel())) return;
        if (event.getPlayer() != null && isBuilder(event.getPlayer())) return;
        event.setCanceled(true);
    }

    /**
     * PNJ marchands (tag {@code sdm_tab:*}) dans le donjon : invulnérables.
     * Un joueur ne peut pas les tuer — ce sont des PNJ de shop, pas des monstres.
     */
    @SubscribeEvent
    public static void onIncomingDamage(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        var entity = event.getEntity();
        if (!inDungeon(entity.level())) return;
        if (entity instanceof net.minecraft.server.level.ServerPlayer target
                && DungeonTeleportHandler.floorAtPos(target.getBlockX(), target.getBlockZ()) == 0) {
            var attacker = resolveAttacker(
                    event.getSource().getEntity(), event.getSource().getDirectEntity());
            if (attacker != null) {
                boolean arenaPvp = tong.statmod.dungeon.city.CityPlan.inArenaCombat(
                        target.getBlockX(), target.getBlockZ())
                        && tong.statmod.dungeon.city.CityPlan.inArenaCombat(
                        attacker.getBlockX(), attacker.getBlockZ());
                if (!arenaPvp) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
        for (String tag : entity.getTags()) {
            if (tag.startsWith("sdm_tab:")) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static net.minecraft.world.entity.player.Player resolveAttacker(net.minecraft.world.entity.Entity source, net.minecraft.world.entity.Entity direct) {
        if (source instanceof net.minecraft.world.entity.player.Player player) {
            return player;
        }
        if (source instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            return player;
        }
        if (direct instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            return player;
        }
        return null;
    }
}
