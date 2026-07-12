package tong.statmod.integration.ironspells.bridge;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.STATMod;
import tong.statmod.integration.tensura.PlayerDataTensuraHook;
import tong.statmod.integration.tensura.TensuraSkillIds;
import tong.statmod.integration.tensura.TensuraSpellProfile;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Wrapper {@link AbstractSpell} qui délègue son exécution à une compétence Tensura.
 *
 * <p><b>Reverse bridge</b> — l'objectif est de permettre au joueur d'inscrire et de lancer
 * une compétence Tensura depuis la flow Iron's Spellbooks (grimoire) plutôt que la HUD Tensura.
 * Coût et cooldown sont gérés côté Iron's (mana, cooldown ticks) ; la magicule Tensura n'est
 * <b>pas</b> drainée (décision fondateur : "Iron's mana only").
 *
 * <p><b>Conformité Tensura</b> — le wrapper interroge {@link TensuraSpellMetadata} pour aligner
 * son {@code CastType} et son {@code castTime} sur les vraies valeurs natives Tensura
 * ({@code Magic.getDefaultCastTime()}). Pour les sorts à charge (cast time &gt; 1) le wrapper
 * passe en {@link CastType#LONG} et réplique le cycle press → hold (per-tick) → release de
 * Tensura, ce qui déclenche le rendu natif du cercle magique via {@code Magic.onHeld} qui
 * appelle {@code applyCastingVisual} interne (bytecode confirmé). Pour les sorts instant, on
 * compresse press+release en un seul frame.
 */
public final class TensuraDelegatingSpell extends AbstractSpell {
    private static final Logger LOGGER = LoggerFactory.getLogger(STATMod.class);

    private final String tensuraSkillId;
    private final TensuraSpellProfile profile;
    private final ResourceLocation spellResource;
    private final DefaultConfig defaultConfig;

    // CastType / castTime sont lazy car le registre Tensura n'est garanti peuplé qu'après le boot.
    private volatile CastType cachedCastType;
    private volatile int cachedCastTimeTicks = -1;

    public TensuraDelegatingSpell(TensuraSpellProfile profile) {
        this.profile = profile;
        this.tensuraSkillId = profile.skillId();
        this.spellResource = ResourceLocation.fromNamespaceAndPath(
                TensuraWrapperIds.WRAPPER_NAMESPACE,
                TensuraWrapperIds.pathFor(this.tensuraSkillId));
        this.defaultConfig = buildDefaultConfig(profile);
    }

    /** Iron's ResourceLocation identity of this spell. */
    @Override
    public ResourceLocation getSpellResource() {
        return spellResource;
    }

    /**
     * Dynamic cast type — INSTANT si Tensura déclare un cast time ≤ 1 tick, sinon LONG.
     * Calculé une fois au premier appel, puis caché.
     */
    @Override
    public CastType getCastType() {
        ensureMetadataResolved();
        return cachedCastType;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public SchoolType getSchoolType() {
        String path = TensuraSchoolMapping.schoolPathFor(profile.primaryStat());
        try {
            ResourceLocation key = ResourceLocation.fromNamespaceAndPath(
                    TensuraSchoolMapping.IRONS_NS, path);
            SchoolType resolved = SchoolRegistry.getSchool(key);
            if (resolved != null) {
                return resolved;
            }
        } catch (Throwable t) {
            LOGGER.debug("SchoolRegistry resolve failed for {}: {}", path, t.toString());
        }
        try {
            return SchoolRegistry.getSchool(
                    ResourceLocation.fromNamespaceAndPath(TensuraSchoolMapping.IRONS_NS, "evocation"));
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    /**
     * Niveau maximum affiché côté Iron's, mappé sur la mastery Tensura via
     * {@link TensuraSpellLevelModifier}. La progression visible dans le grimoire (Lv 1 → 5)
     * reflète directement la progression mastery côté Tensura (0 → maxMastery). Le scaling
     * réel des dégâts reste piloté par Tensura natif côté serveur dans {@code onRelease} —
     * Iron's level est ici purement informatif et UX.
     */
    public static final int MAX_LEVEL = 5;

    /**
     * Coût mana Iron's calé sur le coût magicule natif Tensura quand celui-ci est résoluable,
     * sinon retombe sur la baseline par discipline. Le ratio magicule→mana est documenté dans
     * {@code STAT-DEC-MAGICULE-MANA-RATIO}.
     */
    @Override
    public int getManaCost(int spellLevel) {
        Integer balancedTreeCost = TensuraTreeManaCostResolver.resolve(
                tensuraSkillId,
                profile == null ? null : profile.discipline());
        if (balancedTreeCost != null) {
            return balancedTreeCost;
        }

        double native_ = TensuraSpellMetadata.forSkill(tensuraSkillId).baselineMagiculeCost();
        if (native_ > 0.0) {
            return Math.max(1, (int) Math.ceil(native_ * MAGICULE_TO_MANA_RATIO));
        }
        return manaCostBase(profile);
    }

    /**
     * Ratio de conversion magicule (Tensura) → mana (Iron's). Démarré à 1.0 (identité) pour
     * tests in-game. À ajuster après mesure des coûts moyens des sorts vanilla Iron's pour
     * éviter que les wrappers Tensura soient systématiquement plus chers ou plus laxes.
     */
    private static final double MAGICULE_TO_MANA_RATIO = 1.0;

    @Override
    public int getSpellCooldown() {
        return cooldownTicksBase(profile);
    }

    @Override
    public int getCastTime(int spellLevel) {
        ensureMetadataResolved();
        return cachedCastTimeTicks;
    }

    @Override
    public MutableComponent getDisplayName(Player player) {
        return Component.translatable(TensuraWrapperIds.displayNameTranslationKey(tensuraSkillId));
    }

    /**
     * Pre-cast côté serveur — ne touche qu'au rendu du cercle magique Tensura via
     * {@code instance.onHeld} (pas de {@code startHoldSkill}, on n'utilise pas le système
     * de {@code heldSkills}).
     */
    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        super.onServerPreCast(level, spellLevel, caster, data);
        if (level == null || level.isClientSide || caster == null) return;
        if (!ModList.get().isLoaded("tensura")) return;
        ensureMetadataResolved();
        if (cachedCastType != CastType.LONG) return;
    }

    /**
     * Per-tick côté serveur pendant la charge — appelle {@code instance.onHeld} pour que
     * {@code Magic.onHeld} applique les modifieurs de hold (ralentissement) et, via
     * {@code applyCastingVisual}, fasse apparaître le cercle magique synchronisé aux clients.
     */
    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        super.onServerCastTick(level, spellLevel, caster, data);
        if (level == null || level.isClientSide || caster == null || data == null) return;
        if (!ModList.get().isLoaded("tensura")) return;
        ensureMetadataResolved();
        if (cachedCastType != CastType.LONG) return;
        withStorage(caster, (storage, instance) -> {
            instance.onHeld(caster, 0, 0);
        });
    }

    /**
     * Déclenchement final — appelle directement {@code ManasSkillInstance.onRelease}
     * avec {@code slot = cachedCastTimeTicks} (qui vaut {@code getMaxCastTime()} du skill
     * Tensura) pour BYPASSER le premier garde {@code slot >= getCastingTime()} dans
     * {@code FireBoltMagic.onRelease}. La magicule Tensura est gonflée à 1M pour bypasser
     * le second garde {@code EnergyHelper.isOutOfEnergy()}, puis restaurée immédiatement
     * après (pas de drain — "Iron's mana only").
     *
     * <p>On n'utilise PAS {@code SkillStorage.startHoldSkill/handleSkillRelease} ni le
     * système {@code heldSkills} de Tensura + Iron's Spellbooks gère le cooldown et le coût
     * mana, il n'y a pas besoin des events {@code SkillEvents.RELEASE_SKILL}.
     */
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        super.onCast(level, spellLevel, caster, source, data);
        if (level == null || level.isClientSide || caster == null) return;
        if (!ModList.get().isLoaded("tensura")) return;
        ensureMetadataResolved();
        withStorage(caster, (storage, instance) -> {
            IExistence existence = PlayerDataTensuraHook.getExistence(caster);
            double savedMagicule = existence != null ? existence.getMagicule() : 0.0;
            if (existence != null) {
                existence.setMagicule(1_000_000.0);
            }
            try {
                // slot = cachedCastTimeTicks bypasses the cast-time guard in FireBoltMagic.onRelease
                instance.onRelease(caster, cachedCastTimeTicks, 0, 0);
            } finally {
                if (existence != null) {
                    existence.setMagicule(savedMagicule);
                    existence.markDirty();
                }
            }
        });
    }

    /**
     * Icône native Tensura à utiliser à la place du PNG auto-dérivé par Iron's. Renvoie
     * {@code null} si Tensura n'est pas chargé ou si la résolution échoue — auquel cas Iron's
     * conserve son fallback (chemin {@code statmod:textures/spells/...}).
     *
     * <p>Lu par {@link tong.statmod.mixin.IronSpellIconResolverMixin} qui détourne le getter
     * {@code AbstractSpell.getSpellIconResource()} (qui est {@code final}).
     */
    public ResourceLocation getTensuraIconOverride() {
        ensureMetadataResolved();
        return TensuraSpellMetadata.forSkill(tensuraSkillId).tensuraIconLocation();
    }

    /** Visible for tests: returns the canonical Tensura skill id this wrapper targets. */
    public String tensuraSkillId() {
        return tensuraSkillId;
    }

    /** Visible for tests: profile used to build mana cost / school. */
    public TensuraSpellProfile profile() {
        return profile;
    }

    private synchronized void ensureMetadataResolved() {
        if (cachedCastTimeTicks >= 0) return;
        TensuraSpellMetadata meta = TensuraSpellMetadata.forSkill(tensuraSkillId);
        cachedCastTimeTicks = meta.defaultCastTimeTicks();
        cachedCastType = meta.isHoldStyle() ? CastType.LONG : CastType.INSTANT;
    }

    /**
     * Résout le {@code SkillStorage} et le {@code ManasSkillInstance} pour la compétence
     * ciblée, puis exécute l'action seulement si la compétence a déjà été accordée. Les
     * échecs sont logués en WARN (visibles dans la console normale).
     */
    private void withStorage(LivingEntity caster, BiConsumer<SkillStorage, ManasSkillInstance> action) {
        try {
            SkillStorage storage = SkillAPI.getSkillsFrom(caster);
            if (storage == null) {
                LOGGER.warn("TensuraDelegatingSpell {}: SkillStorage null for {}", tensuraSkillId, caster.getName().getString());
                return;
            }
            ResourceLocation canonical = ResourceLocation.parse(
                    TensuraSkillIds.canonicalize(tensuraSkillId));
            Optional<ManasSkillInstance> resolved = storage.getSkill(canonical);
            if (resolved.isPresent()) {
                action.accept(storage, resolved.get());
            } else {
                LOGGER.warn("TensuraDelegatingSpell {} unavailable for {}: skill was not granted",
                        tensuraSkillId, caster.getName().getString());
            }
        } catch (Throwable t) {
            LOGGER.warn("TensuraDelegatingSpell {} dispatch failed: {}", tensuraSkillId, t.toString());
        }
    }

    private static DefaultConfig buildDefaultConfig(TensuraSpellProfile profile) {
        ResourceLocation schoolResource = ResourceLocation.fromNamespaceAndPath(
                TensuraSchoolMapping.IRONS_NS,
                TensuraSchoolMapping.schoolPathFor(profile == null ? null : profile.primaryStat()));
        return new DefaultConfig()
                .setMinRarity(io.redspace.ironsspellbooks.api.spells.SpellRarity.COMMON)
                .setMaxLevel(1)
                .setCooldownSeconds(cooldownTicksBase(profile) / 20.0)
                .setSchoolResource(schoolResource)
                .setAllowCrafting(false)
                .build();
    }

    private static int manaCostBase(TensuraSpellProfile profile) {
        if (profile == null) return 50;
        return switch (profile.discipline()) {
            case "support" -> 75;
            case "defense" -> 60;
            case "mobility" -> 40;
            case "utility" -> 30;
            case "control" -> 80;
            case "pressure" -> 90;
            case "empowerment" -> 70;
            case "offense" -> 100;
            default -> 50;
        };
    }

    private static int cooldownTicksBase(TensuraSpellProfile profile) {
        if (profile == null) return 40;
        return switch (profile.discipline()) {
            case "offense", "pressure" -> 60;
            case "mobility", "utility" -> 30;
            default -> 40;
        };
    }
}
