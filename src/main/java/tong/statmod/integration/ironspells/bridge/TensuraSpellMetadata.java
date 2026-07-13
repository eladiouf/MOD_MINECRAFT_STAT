package tong.statmod.integration.ironspells.bridge;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tong.statmod.STATMod;
import tong.statmod.integration.tensura.TensuraSkillIds;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Lazy cache des paramètres natifs d'une compétence Tensura, queryés par reflection sur le
 * singleton {@code ManasSkill} de Tensura. Permet au wrapper Iron's de se conformer aux
 * valeurs réelles (cast time, type instant ou hold) au lieu d'inventer des défauts génériques.
 *
 * <p>Le cache est lazy car le registre Tensura n'est garanti peuplé qu'après le boot complet,
 * tandis que nos wrappers sont instanciés tôt par {@link TensuraSpellWrapperRegistry}.
 */
public final class TensuraSpellMetadata {
    private static final Logger LOGGER = LoggerFactory.getLogger(STATMod.class);
    private static final ConcurrentMap<String, TensuraSpellMetadata> CACHE = new ConcurrentHashMap<>();
    private static final TensuraSpellMetadata FALLBACK = new TensuraSpellMetadata(0, false, null, -1.0);

    private final int defaultCastTimeTicks;
    private final boolean knownTensuraSkill;
    private final ResourceLocation tensuraIconLocation;
    private final double baselineMagiculeCost;

    private TensuraSpellMetadata(int defaultCastTimeTicks, boolean knownTensuraSkill,
                                  ResourceLocation tensuraIconLocation,
                                  double baselineMagiculeCost) {
        this.defaultCastTimeTicks = Math.max(0, defaultCastTimeTicks);
        this.knownTensuraSkill = knownTensuraSkill;
        this.tensuraIconLocation = tensuraIconLocation;
        this.baselineMagiculeCost = baselineMagiculeCost;
    }

    public static TensuraSpellMetadata forSkill(String skillId) {
        if (skillId == null || skillId.isBlank()) return FALLBACK;
        TensuraSpellMetadata cached = CACHE.get(skillId);
        if (cached != null) return cached;

        TensuraSpellMetadata queried = query(skillId);
        // Registry lookup can happen during early client/model initialization. A miss at that
        // point is temporary and must remain retryable once Tensura finishes registering skills.
        if (!queried.knownTensuraSkill) return queried;
        TensuraSpellMetadata raced = CACHE.putIfAbsent(skillId, queried);
        return raced != null ? raced : queried;
    }

    /** Force re-query (visible for tests / dev reloads). */
    static void invalidate(String skillId) {
        if (skillId != null) {
            CACHE.remove(skillId);
        }
    }

    public int defaultCastTimeTicks() {
        return defaultCastTimeTicks;
    }

    boolean isResolved() {
        return knownTensuraSkill;
    }

    /** True si la compétence cache un vrai temps de charge (= pas instant). */
    public boolean isHoldStyle() {
        return knownTensuraSkill && defaultCastTimeTicks > 1;
    }

    /**
     * Chemin {@link ResourceLocation} de l'icône native Tensura, ou {@code null} si non
     * résoluable. Format typique : {@code tensura:textures/magic/spiritual/fire_bolt.png}.
     */
    public ResourceLocation tensuraIconLocation() {
        return tensuraIconLocation;
    }

    /**
     * Coût magicule de référence (mode 0) tel que déclaré par {@code Magic.getMagiculeCost}
     * sans contexte joueur, ou {@code -1.0} si non résoluable (sort utilise l'entity caster
     * pour scaler ou Tensura non chargé). Le wrapper convertit cette valeur en mana Iron's
     * via {@link TensuraDelegatingSpell#getManaCost(int)}.
     */
    public double baselineMagiculeCost() {
        return baselineMagiculeCost;
    }

    private static TensuraSpellMetadata query(String skillId) {
        try {
            ResourceLocation rl = ResourceLocation.parse(TensuraSkillIds.canonicalize(skillId));
            Object registry = SkillAPI.getSkillRegistry();
            if (registry == null) return FALLBACK;

            // Registrar.get(ResourceLocation) on architectury returns the skill or null.
            Method getter = findMethod(registry.getClass(), "get", ResourceLocation.class);
            if (getter == null) return FALLBACK;
            Object skill = getter.invoke(registry, rl);
            if (!(skill instanceof ManasSkill manasSkill)) return FALLBACK;

            int castTime = invokeIntGetter(manasSkill, "getDefaultCastTime");
            ResourceLocation icon = invokeIconGetter(manasSkill);
            double magicule = invokeMagiculeCostGetter(manasSkill);
            return new TensuraSpellMetadata(castTime, true, icon, magicule);
        } catch (Throwable t) {
            LOGGER.debug("TensuraSpellMetadata query failed for {}: {}", skillId, t.toString());
            return FALLBACK;
        }
    }

    /**
     * Query {@code Magic.getMagiculeCost(LivingEntity, ManasSkillInstance, int)} avec entity
     * null et instance default. La majorité des Magic ignorent ces args (impl en
     * {@code getstatic CONFIG.magiculeCost / dreturn}). Quelques sous-classes
     * (BarrierMagic, MagicBarrierMagic, ReinforcedBarrierMagic) lisent
     * {@code EnergyHelper.getBaseMaxMagicule(entity)} et crashent NPE — auquel cas on retombe
     * sur {@code -1.0} et le wrapper utilise la valeur par défaut par discipline.
     */
    private static double invokeMagiculeCostGetter(ManasSkill skill) {
        try {
            Method method = findMethod(skill.getClass(), "getMagiculeCost",
                    net.minecraft.world.entity.LivingEntity.class,
                    io.github.manasmods.manascore.skill.api.ManasSkillInstance.class,
                    int.class);
            if (method == null) return -1.0;
            io.github.manasmods.manascore.skill.api.ManasSkillInstance instance =
                    skill.createDefaultInstance();
            Object result = method.invoke(skill, null, instance, 0);
            if (result instanceof Double d) return d;
        } catch (Throwable t) {
            // Entity-dependent spells (barriers) NPE here — fall back to discipline default.
            LOGGER.debug("TensuraSpellMetadata getMagiculeCost failed on {} (entity-dependent or unknown): {}",
                    skill.getClass().getName(), t.toString());
        }
        return -1.0;
    }

    private static ResourceLocation invokeIconGetter(ManasSkill skill) {
        try {
            ResourceLocation icon = skill.getSkillIcon();
            // Tensura's getSkillIcon already returns a textures/<...>.png path. Iron's renderer
            // accepts any ResourceLocation pointing at a real PNG asset.
            return icon;
        } catch (Throwable t) {
            LOGGER.debug("TensuraSpellMetadata getSkillIcon failed on {}: {}",
                    skill.getClass().getName(), t.toString());
            return null;
        }
    }

    private static int invokeIntGetter(Object target, String name) {
        try {
            Method m = target.getClass().getMethod(name);
            Object result = m.invoke(target);
            if (result instanceof Integer i) return i;
        } catch (NoSuchMethodException ignored) {
            // Not a Magic skill (e.g. raw TensuraSkill without cast time). Treat as instant.
        } catch (Throwable t) {
            LOGGER.debug("TensuraSpellMetadata getter {} failed on {}: {}",
                    name, target.getClass().getName(), t.toString());
        }
        return 0;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... paramTypes) {
        try {
            return type.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            for (Method m : type.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == paramTypes.length) {
                    return m;
                }
            }
            return null;
        }
    }
}
