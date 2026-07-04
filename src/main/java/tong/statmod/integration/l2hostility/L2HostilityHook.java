package tong.statmod.integration.l2hostility;

import net.minecraft.world.entity.LivingEntity;
import tong.statmod.STATMod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Mission M6 — Appels à l'API L2 Hostility <b>par réflexion</b>.
 *
 * <p>Pourquoi la réflexion et pas des imports directs : les classes de base de L2
 * ({@code l2core} : {@code AttVal}, {@code GeneralCapabilityHolder}) sont empaquetées dans un
 * jar-in-jar (jarjar) de {@code l2library}/{@code l2hostility}, absent du classpath de
 * compilation. Les référencer en dur casse la compilation, et les extraire dans {@code libs}
 * risquerait un doublon de classes au runtime. La réflexion contourne les deux problèmes et
 * dégrade proprement (tout est encapsulé dans un try/catch).
 *
 * <p>Chaîne équivalente : {@code LHMiscs.MOB.type().getOrCreate(mob).setLevel(mob, level)}.
 * Résolue une seule fois puis mise en cache.
 */
final class L2HostilityHook {

    private static boolean resolved = false;
    private static boolean usable = false;
    private static Object mobHolder;      // GeneralCapabilityHolder<LivingEntity, MobTraitCap>
    private static Method getOrCreate;    // holder.getOrCreate(IAttachmentHolder) → MobTraitCap
    private static Method setLevel;       // MobTraitCap.setLevel(LivingEntity, int)

    private L2HostilityHook() {}

    static void apply(LivingEntity mob, int level) {
        try {
            if (!resolved) resolve(mob);
            if (!usable) return;
            Object cap = getOrCreate.invoke(mobHolder, mob);
            if (cap == null) return;
            if (setLevel == null) setLevel = cap.getClass().getMethod("setLevel", LivingEntity.class, int.class);
            setLevel.invoke(cap, mob, level);
        } catch (Throwable e) {
            // Ne jamais laisser une incompatibilité L2 casser le spawn du donjon.
            STATMod.LOGGER.warn("[TrialDungeon] L2 Hostility apply failed for {}: {}",
                    mob.getType(), e.toString());
            usable = false;
        }
    }

    private static void resolve(LivingEntity sample) throws Exception {
        resolved = true;
        Class<?> lhMiscs = Class.forName("dev.xkmc.l2hostility.init.registrate.LHMiscs");
        Field mobField = lhMiscs.getField("MOB");
        Object mobCapVal = mobField.get(null);
        mobHolder = findMethod(mobCapVal.getClass(), "type", 0).invoke(mobCapVal);
        getOrCreate = findMethod(mobHolder.getClass(), "getOrCreate", 1);
        // setLevel est résolu paresseusement sur la classe concrète du cap (voir apply).
        usable = mobHolder != null && getOrCreate != null;
        STATMod.LOGGER.info("[TrialDungeon] L2 Hostility integration active (mob difficulty scaling)");
    }

    /** Trouve une méthode publique par nom + nombre d'arguments (contourne l'erasure des génériques). */
    private static Method findMethod(Class<?> c, String name, int argc) {
        for (Method m : c.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == argc) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }
}
