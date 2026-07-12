package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import tong.statmod.STATMod;
import tong.statmod.dungeon.DungeonSpawnGuard;

import java.util.EnumSet;
import java.util.List;

/**
 * IA de mage du donjon — refonte « vrai caster » (2026-07-11), calibrée sur le
 * {@code WizardAttackGoal} d'Iron's Spellbooks (bytecode 3.16.1 décompilé) :
 *
 * <ul>
 *   <li><b>seeTime accumulé</b> (+1/tick vu, −1/tick perdu) : la pression est maintenue pendant
 *       une brève rupture de ligne de vue ; l'attaque n'est abandonnée qu'à −50 (comme Iron's) ;</li>
 *   <li><b>riposte au coup reçu</b> : touché au tick précédent → le délai d'attaque est réduit de
 *       60 % vers zéro → contre-attaque quasi immédiate ;</li>
 *   <li><b>cadence liée à la distance</b> : délai = dist/portée × (max−min) + min — mitraille au
 *       corps-à-corps, espace ses sorts à longue portée ;</li>
 *   <li><b>mobile en incantation</b> : strafe à vitesse réduite pendant le windup (75 % chez
 *       Iron's) au lieu de se figer ;</li>
 *   <li><b>kite</b> : backpedal face au joueur sous portée/√6 (ratio exact d'Iron's), strafe avec
 *       flip probabiliste (10 %/tick après 25 ticks), saut par-dessus l'obstacle en strafant
 *       (sauf portes/clôtures — le donjon en est plein), repath tous les 5 ticks ;</li>
 *   <li><b>fuite pathfinding rare</b> (feedback playtest : pas de demi-tour permanent) :
 *       uniquement sous 4 blocs, avec cooldown long, à 1.4× vitesse ;</li>
 *   <li><b>visée instantanée au tick du tir</b> : les projectiles d'Iron's partent le long de
 *       {@code getLookAngle()} — sans rotation immédiate ils partent dans le vide, et le sort
 *       d'évasion « charge » dashait VERS le joueur ;</li>
 *   <li><b>télégraphe</b> : son + particules pendant l'incantation ;</li>
 *   <li><b>cohésion d'escouade</b> (feedback playtest) : un mage isolé (>14 blocs d'un coéquipier
 *       {@code AUTHORIZED_TAG}) se replie vers le groupe.</li>
 * </ul>
 */
public class DungeonMobSpellcastGoal extends Goal {

    public enum MageRole {
        ATTACKER,
        HEALER,
        SUPPORT
    }

    /** Catégorie du sort en préparation : durée d'incantation et règles de visée dédiées. */
    private enum CastKind { BASIC, HEAVY, SUMMON, HEAL, SHIELD }

    private static class SpellInfo {
        final AbstractSpell spell;
        final int level;
        final int cooldown;
        int cooldownTimer;

        SpellInfo(String spellId, int level, int cooldown) {
            this.spell = spellId != null ? SpellRegistry.getSpell(ResourceLocation.parse(spellId)) : null;
            this.level = level;
            this.cooldown = cooldown;
        }

        void tick() {
            if (cooldownTimer > 0) cooldownTimer--;
        }

        boolean ready() {
            return spell != null && cooldownTimer == 0;
        }
    }

    // Portée de cast (Iron's : spellcastingRange). Le backpedal se déclenche sous range/√6.
    private static final float CASTING_RANGE = 20.0f;
    private static final float CASTING_RANGE_SQR = CASTING_RANGE * CASTING_RANGE;
    private static final double ENGAGE_RANGE = 32.0;
    // Cadence globale entre deux casts, interpolée par la distance (Iron's : min..max)
    private static final int ATTACK_INTERVAL_MIN = 25;
    private static final int ATTACK_INTERVAL_MAX = 65;

    private final Mob mob;
    private final MageRole role;

    // Grimoire virtuel
    private final SpellInfo basicAttack;
    private final SpellInfo heavyAttack;
    private final SpellInfo escapeSpell;
    private final SpellInfo summonSpell;
    private final SpellInfo healSpell;
    private final SpellInfo shieldSpell;

    // Incantation en cours (windup avant le cast effectif)
    private SpellInfo pendingSpell;
    private CastKind pendingKind;
    private LivingEntity pendingTarget;
    private int windupTicks;

    // Vision accumulée (Iron's) : +1/tick avec ligne de vue, −1 sans
    private int seeTime;
    // Délai global avant le prochain cast, interpolé par la distance (Iron's)
    private int attackDelay = 20;
    private int strafeTime;
    private boolean strafingClockwise;
    private int fleeCooldown;

    public DungeonMobSpellcastGoal(Mob mob, MageRole role,
                                   String basicId, int basicLvl, int basicCd,
                                   String heavyId, int heavyLvl, int heavyCd,
                                   String escapeId, int escapeLvl, int escapeCd,
                                   String summonId, int summonLvl, int summonCd,
                                   String healId, int healLvl, int healCd,
                                   String shieldId, int shieldLvl, int shieldCd) {
        this.mob = mob;
        this.role = role;

        this.basicAttack = new SpellInfo(basicId, basicLvl, basicCd);
        this.heavyAttack = new SpellInfo(heavyId, heavyLvl, heavyCd);
        this.escapeSpell = new SpellInfo(escapeId, escapeLvl, escapeCd);
        this.summonSpell = new SpellInfo(summonId, summonLvl, summonCd);
        this.healSpell = new SpellInfo(healId, healLvl, healCd);
        this.shieldSpell = new SpellInfo(shieldId, shieldLvl, shieldCd);

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        boolean hasTarget = target != null && target.isAlive()
                && mob.distanceToSqr(target) <= ENGAGE_RANGE * ENGAGE_RANGE;
        if (role == MageRole.HEALER) {
            return hasTarget || hasInjuredAllies();
        }
        return hasTarget;
    }

    @Override
    public void start() {
        // Décalage initial aléatoire : une vague de mages ne caste pas à l'unisson
        stagger(basicAttack, 10);
        stagger(heavyAttack, 30);
        stagger(summonSpell, 40);
        stagger(healSpell, 15);
        stagger(shieldSpell, 15);
        attackDelay = 10 + mob.getRandom().nextInt(15);
        seeTime = 0;
        strafeTime = 0;
        strafingClockwise = mob.getRandom().nextBoolean();
        fleeCooldown = 0;
    }

    private void stagger(SpellInfo s, int base) {
        if (s.spell != null) {
            s.cooldownTimer = base + mob.getRandom().nextInt(Math.max(1, s.cooldown / 3));
        }
    }

    @Override
    public void stop() {
        clearPending();
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        basicAttack.tick();
        heavyAttack.tick();
        escapeSpell.tick();
        summonSpell.tick();
        healSpell.tick();
        shieldSpell.tick();
        if (fleeCooldown > 0) fleeCooldown--;

        LivingEntity target = mob.getTarget();
        if (target != null && !target.isAlive()) target = null;

        MagicData magicData = MagicData.getPlayerMagicData(mob);
        if (magicData == null) return;

        double distSqr = -1.0D;
        if (target != null) {
            distSqr = mob.distanceToSqr(target);
            // Vision accumulée (Iron's) : tolère les ruptures brèves de ligne de vue
            if (mob.getSensing().hasLineOfSight(target)) {
                seeTime = Math.min(seeTime + 1, 100);
            } else {
                seeTime = Math.max(seeTime - 1, -60);
            }
            // Riposte (Iron's) : touché au tick précédent → délai d'attaque écrasé de 60 %
            if (mob.getLastHurtByMobTimestamp() == mob.tickCount - 1) {
                attackDelay = (int) (Mth.lerp(0.6f, attackDelay, 0.0f)) + 1;
            }
        }
        if (attackDelay > 0) attackDelay--;

        // ── URGENCE : évasion instantanée, interrompt même une incantation ──
        if (target != null && mob.getHealth() < mob.getMaxHealth() * 0.35f && escapeSpell.ready()) {
            clearPending();
            castEscape(target, magicData);
            return;
        }

        // ── Incantation en cours : mobile mais concentré (Iron's caste en bougeant) ──
        if (pendingSpell != null) {
            tickWindup(target, magicData);
            return;
        }

        // ── Placement (structure doMovement d'Iron's) ──
        if (target != null) {
            doMovement(target, distSqr);
        } else if (role == MageRole.HEALER) {
            Mob ally = findMostInjuredAlly(16.0D);
            if (ally != null && mob.distanceToSqr(ally) > 8 * 8) {
                mob.getNavigation().moveTo(ally, 1.0D);
            }
        }

        // ── Arbre de décision ──

        // 1. Soin d'urgence personnel (pas soumis au rythme d'attaque)
        if (mob.getHealth() < mob.getMaxHealth() * 0.40f && healSpell.ready()) {
            begin(healSpell, CastKind.HEAL, mob);
            return;
        }

        // 2. Soin de groupe (Healer) / protection d'allié (Support)
        if (role == MageRole.HEALER || role == MageRole.SUPPORT) {
            Mob ally = findMostInjuredAlly(12.0D);
            if (ally != null) {
                if (role == MageRole.HEALER && healSpell.ready()) {
                    begin(healSpell, CastKind.HEAL, ally);
                    return;
                }
                if (role == MageRole.SUPPORT && shieldSpell.ready()) {
                    begin(shieldSpell, CastKind.SHIELD, ally);
                    return;
                }
            }
        }

        // 3. Bouclier défensif personnel
        if (shieldSpell.ready()
                && (mob.getHealth() < mob.getMaxHealth() * 0.65f || mob.getLastHurtByMob() != null)) {
            begin(shieldSpell, CastKind.SHIELD, mob);
            return;
        }

        // Offensive : soumise au rythme global + à la vision accumulée (Iron's : seeTime > -50)
        if (target == null || attackDelay > 0 || seeTime < -50) return;

        // 4. Invocation de sbires
        if (summonSpell.ready() && seeTime > 0) {
            begin(summonSpell, CastKind.SUMMON, target);
            return;
        }

        // 5. Attaque lourde (zone, tempêtes)
        if (heavyAttack.ready() && seeTime > 0 && distSqr <= CASTING_RANGE_SQR) {
            begin(heavyAttack, CastKind.HEAVY, target);
            return;
        }

        // 6. Attaque de base (projectile rapide)
        if (basicAttack.ready() && seeTime > 0 && distSqr <= CASTING_RANGE_SQR * 1.44D) {
            begin(basicAttack, CastKind.BASIC, target);
        }
    }

    // ────────────────────── Incantation ──────────────────────

    private void begin(SpellInfo spell, CastKind kind, LivingEntity castTarget) {
        pendingSpell = spell;
        pendingKind = kind;
        pendingTarget = castTarget;
        windupTicks = switch (kind) {
            case BASIC -> 8;
            case HEAVY -> 16;
            case SUMMON -> 12;
            case HEAL -> 12;
            case SHIELD -> 5;
        };
        // Télégraphe sonore : le joueur entend qu'un sort arrive
        mob.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 0.8f, 1.0f + mob.getRandom().nextFloat() * 0.4f);
    }

    private void tickWindup(LivingEntity target, MagicData magicData) {
        LivingEntity castTarget = pendingTarget;
        if (castTarget == null || !castTarget.isAlive() || castTarget.isRemoved()) {
            clearPending();
            return;
        }

        // CONTRE-JEU (équilibrage solo) : frapper le mage pendant son incantation l'INTERROMPT
        // (sauf bouclier, réflexe défensif). C'est ce qui rend le « focus le soigneur » efficace
        // en solo — sans ça l'escouade est insoignable à un joueur.
        if (pendingKind != CastKind.SHIELD && mob.getLastHurtByMobTimestamp() == mob.tickCount - 1) {
            mob.playSound(SoundEvents.FIRE_EXTINGUISH, 0.7f, 1.4f);
            if (mob.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE,
                        mob.getX(), mob.getEyeY(), mob.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.02D);
            }
            clearPending();
            attackDelay = Math.max(attackDelay, 15);
            return;
        }

        // Mobile en incantation (Iron's : 75 % de vitesse) — strafe doux, pas de navigation
        mob.getNavigation().stop();
        mob.lookAt(castTarget, 30.0f, 30.0f);
        mob.getMoveControl().strafe(-0.1f, (strafingClockwise ? 1 : -1) * 0.35f);

        // Télégraphe visuel pendant l'incantation
        if (mob.level() instanceof ServerLevel sl && windupTicks % 3 == 0) {
            sl.sendParticles(ParticleTypes.WITCH,
                    mob.getX(), mob.getEyeY() + 0.3D, mob.getZ(),
                    2, 0.25D, 0.25D, 0.25D, 0.0D);
        }

        if (--windupTicks > 0) return;

        // Visée : rotation INSTANTANÉE au moment exact du tir. Les projectiles d'Iron's
        // partent le long de getLookAngle() — la rotation progressive n'aboutit jamais
        // dans le même tick (l'ancienne IA tirait dans le vide).
        if (pendingKind == CastKind.BASIC || pendingKind == CastKind.HEAVY) {
            if (!mob.getSensing().hasLineOfSight(castTarget)) {
                // Un mur s'est interposé pendant l'incantation : annuler SANS brûler le cooldown
                clearPending();
                return;
            }
            mob.lookAt(EntityAnchorArgument.Anchor.EYES, castTarget.getEyePosition());
        }

        SpellInfo s = pendingSpell;
        STATMod.LOGGER.debug("[DungeonMobSpellcastGoal] cast {} par {} sur {}",
                s.spell, mob.getType(), castTarget.getName().getString());
        s.spell.onCast(mob.level(), s.level, mob, CastSource.MOB, magicData);
        s.cooldownTimer = s.cooldown;
        // Cadence liée à la distance (Iron's) : rapide au contact, espacée de loin
        double distSqr = target != null ? mob.distanceToSqr(target) : CASTING_RANGE_SQR;
        resetAttackDelay(distSqr);
        clearPending();
    }

    /** Iron's resetSpellAttackTimer : délai = dist/portée × (max−min) + min. */
    private void resetAttackDelay(double distSqr) {
        float f = (float) (Math.sqrt(distSqr) / CASTING_RANGE);
        attackDelay = Math.max(1, Mth.floor(f * (ATTACK_INTERVAL_MAX - ATTACK_INTERVAL_MIN) + ATTACK_INTERVAL_MIN));
    }

    private void clearPending() {
        pendingSpell = null;
        pendingKind = null;
        pendingTarget = null;
        windupTicks = 0;
    }

    private void castEscape(LivingEntity threat, MagicData magicData) {
        Vec3 away = mob.position().subtract(threat.position());
        if (away.lengthSqr() < 1.0e-4D) away = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 lookPoint = mob.getEyePosition().add(away.normalize().scale(10.0D));
        // Rotation instantanée DOS à la menace : frost_step/charge se lancent le long du
        // regard — avec la rotation progressive, « charge » dashait VERS le joueur.
        mob.lookAt(EntityAnchorArgument.Anchor.EYES, lookPoint);
        escapeSpell.spell.onCast(mob.level(), escapeSpell.level, mob, CastSource.MOB, magicData);
        escapeSpell.cooldownTimer = escapeSpell.cooldown;
        attackDelay = Math.max(attackDelay, 10);
    }

    // ────────────────────── Placement (structure doMovement d'Iron's) ──────────────────────

    private void doMovement(LivingEntity target, double distSqr) {
        // Regard : rotation du corps + tête chaque tick (Iron's utilise mob.lookAt)
        mob.lookAt(target, 30.0f, 30.0f);

        // Fuite pathfinding RARE (feedback playtest : pas de demi-tour permanent) :
        // seulement collé (< 4 blocs), pas sur le point de caster, avec long cooldown.
        if (distSqr < 4.0D * 4.0D && attackDelay > 10 && fleeCooldown <= 0) {
            Vec3 flee = null;
            if (mob instanceof PathfinderMob pm) {
                flee = DefaultRandomPos.getPosAway(pm, 16, 7, target.position());
            }
            if (flee != null) {
                mob.getNavigation().moveTo(flee.x, flee.y, flee.z, 1.4D);
                fleeCooldown = 100;
                return;
            }
        }

        if (distSqr < CASTING_RANGE_SQR && seeTime >= 5) {
            // Zone de contrôle : strafe latéral, backpedal si trop près (ratios d'Iron's)
            mob.getNavigation().stop();

            // Cohésion d'escouade (feedback playtest : « ils devraient rester ensemble »)
            Mob squadmate = findNearestSquadmate(20.0D);
            if (squadmate != null && mob.distanceToSqr(squadmate) > 14.0D * 14.0D) {
                mob.getNavigation().moveTo(squadmate, 0.95D);
                return;
            }

            if (++strafeTime > 25 && mob.getRandom().nextDouble() < 0.1D) {
                strafingClockwise = !strafingClockwise;
                strafeTime = 0;
            }
            // Iron's : recule sous portée/√6 (~8 blocs), avance légèrement sinon
            float forward = (distSqr * 6.0D < CASTING_RANGE_SQR ? -1.0f : 0.5f) * 0.25f;
            float sideways = (strafingClockwise ? 1 : -1) * 0.55f;
            mob.getMoveControl().strafe(forward, sideways);

            // Obstacle en strafant : petit saut (Iron's tryJump), jamais sur porte/clôture
            if (mob.horizontalCollision && mob.getRandom().nextFloat() < 0.1f) {
                tryJump();
            }
        } else if (mob.tickCount % 5 == 0) {
            // Hors zone ou vision perdue : se replacer (repath tous les 5 ticks, comme Iron's)
            mob.getNavigation().moveTo(target, 1.0D);
        }
    }

    /**
     * Saut par-dessus l'obstacle rencontré en strafant (port du tryJump d'Iron's) :
     * saute seulement si le bloc devant est solide, N'est PAS une porte/clôture
     * (infranchissables en sautant — le donjon en est plein) et que l'espace au-dessus est libre.
     */
    private void tryJump() {
        Vec3 dir = new Vec3(mob.xxa, 0.0D, mob.zza);
        if (dir.lengthSqr() < 1.0e-6D) return;
        BlockPos ahead = BlockPos.containing(mob.position().add(dir.normalize()));
        BlockState state = mob.level().getBlockState(ahead);
        if (state.getCollisionShape(mob.level(), ahead).isEmpty()) return;
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.FENCES)) return;
        BlockPos above = ahead.above();
        if (!mob.level().getBlockState(above).getCollisionShape(mob.level(), above).isEmpty()) return;
        mob.getJumpControl().jump();
    }

    /**
     * Coéquipier le plus proche : un autre mob autorisé du donjon (vague, escouade de mages).
     * Sert à la cohésion — un mage isolé se replie vers le groupe au lieu de kiter seul.
     */
    private Mob findNearestSquadmate(double radius) {
        List<Mob> entities = mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(radius));
        Mob nearest = null;
        double bestDistSqr = Double.MAX_VALUE;
        for (Mob entity : entities) {
            if (entity == mob || !entity.isAlive()) continue;
            if (!entity.getPersistentData().getBoolean(DungeonSpawnGuard.AUTHORIZED_TAG)
                    && entity.getPersistentData().getString("statmod_custom_mage_type").isEmpty()) {
                continue;
            }
            double d = mob.distanceToSqr(entity);
            if (d < bestDistSqr) {
                bestDistSqr = d;
                nearest = entity;
            }
        }
        return nearest;
    }

    private boolean hasInjuredAllies() {
        return findMostInjuredAlly(12.0D) != null;
    }

    private Mob findMostInjuredAlly(double radius) {
        List<Mob> entities = mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(radius));
        Mob targetAlly = null;
        float lowestHealthPercent = 1.0f;

        for (Mob entity : entities) {
            if (entity == mob) continue;

            String dim = entity.level().dimension().location().toString();
            if (!"statmod:trial_dungeon".equals(dim)) continue;

            float hpPercent = entity.getHealth() / entity.getMaxHealth();
            // Seuil 65 % (équilibrage solo) : le healer ne « top-off » plus en permanence,
            // il n'intervient que sur un allié réellement entamé.
            if (hpPercent < 0.65f && hpPercent < lowestHealthPercent) {
                lowestHealthPercent = hpPercent;
                targetAlly = entity;
            }
        }
        return targetAlly;
    }
}
