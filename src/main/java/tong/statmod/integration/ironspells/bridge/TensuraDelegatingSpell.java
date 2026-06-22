package tong.statmod.integration.ironspells.bridge;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
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
import tong.statmod.integration.tensura.TensuraSkillIds;
import tong.statmod.integration.tensura.TensuraSpellProfile;

import java.util.Optional;

/**
 * Wrapper {@link AbstractSpell} qui délègue son exécution à une compétence Tensura
 * via {@link ManasSkillInstance#onPressed(LivingEntity, int, int)}.
 *
 * <p><b>Reverse bridge</b> — l'objectif est de permettre au joueur d'inscrire et de lancer
 * une compétence Tensura depuis la flow Iron's Spellbooks (grimoire) plutôt que la HUD Tensura.
 * Coût et cooldown sont gérés côté Iron's (mana, cooldown ticks) ; la magicule Tensura n'est
 * <b>pas</b> drainée (décision fondateur : "Iron's mana only").
 *
 * <p>Le wrapper est immuable et stateless ; un instance unique est créé par compétence
 * Tensura au boot par {@link TensuraSpellWrapperRegistry}.
 */
public final class TensuraDelegatingSpell extends AbstractSpell {
    private static final Logger LOGGER = LoggerFactory.getLogger(STATMod.class);

    private final String tensuraSkillId;
    private final TensuraSpellProfile profile;
    private final ResourceLocation spellResource;
    private final DefaultConfig defaultConfig;

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

    /** Wrappers are instant; the cooldown is handled via Iron's getSpellCooldown(). */
    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public SchoolType getSchoolType() {
        String path = TensuraSchoolMapping.schoolPathFor(profile.primaryStat());
        // Defensive lookup: SchoolRegistry exposes static fields (FIRE, ICE, NATURE, ...) and a
        // registry. We resolve by path to stay agnostic to field availability across versions.
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
        // Fallback to evocation if available, else null (Iron's tolerates null in some paths).
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
        return 1;
    }

    @Override
    public int getManaCost(int spellLevel) {
        return manaCostBase(profile);
    }

    @Override
    public int getSpellCooldown() {
        return cooldownTicksBase(profile);
    }

    // getSpellIconResource is final in AbstractSpell — Iron's auto-derives the icon path
    // from getSpellResource() (namespace + textures/spells/<path>.png). We ship a single
    // default placeholder at that auto-derived path.

    @Override
    public MutableComponent getDisplayName(Player player) {
        return Component.translatable(TensuraWrapperIds.displayNameTranslationKey(tensuraSkillId));
    }

    /**
     * Server-side execution. Resolves the Tensura skill instance (learning it silently if
     * absent), then calls {@code onPressed(caster, 0, 0)} to trigger Tensura's native effect.
     */
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        super.onCast(level, spellLevel, caster, source, data);
        if (level == null || level.isClientSide || caster == null) {
            return;
        }
        if (!ModList.get().isLoaded("tensura")) {
            return;
        }
        delegateToTensura(caster);
    }

    /** Visible for tests: returns the canonical Tensura skill id this wrapper targets. */
    public String tensuraSkillId() {
        return tensuraSkillId;
    }

    /** Visible for tests: profile used to build mana cost / school. */
    public TensuraSpellProfile profile() {
        return profile;
    }

    private void delegateToTensura(LivingEntity caster) {
        try {
            SkillStorage storage = SkillAPI.getSkillsFrom(caster);
            if (storage == null) {
                return;
            }
            ResourceLocation canonical = ResourceLocation.parse(
                    TensuraSkillIds.canonicalize(tensuraSkillId));
            Optional<ManasSkillInstance> resolved = storage.getSkill(canonical);
            if (resolved.isEmpty()) {
                storage.learnSkill(canonical);
                resolved = storage.getSkill(canonical);
            }
            resolved.ifPresent(instance -> instance.onPressed(caster, 0, 0));
        } catch (Throwable t) {
            LOGGER.warn("TensuraDelegatingSpell {} cast delegation failed: {}",
                    tensuraSkillId, t.toString());
        }
    }

    private static DefaultConfig buildDefaultConfig(TensuraSpellProfile profile) {
        // Iron's DefaultConfig is itself fluent (no inner Builder). Mana cost is overridden
        // separately via getManaCost(int) so it is not part of the config. The school resource
        // is set here so Iron's validator accepts the spell registration.
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
        // 2 seconds baseline; pressure/offense get a bit more.
        if (profile == null) return 40;
        return switch (profile.discipline()) {
            case "offense", "pressure" -> 60;
            case "mobility", "utility" -> 30;
            default -> 40;
        };
    }
}
