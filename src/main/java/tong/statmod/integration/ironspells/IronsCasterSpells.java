package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import tong.statmod.dungeon.ai.DungeonAiActor;

/** Runtime bridge for real Iron's Spells casts by managed dungeon mobs. */
public final class IronsCasterSpells {
    private IronsCasterSpells() {}

    /** Backwards-compatible entry point used by existing rival-party mages. */
    public static boolean cast(Mob mage, LivingEntity target, String element) {
        int floor = Math.max(1, mage.getPersistentData().getInt(DungeonAiActor.FLOOR_TAG));
        return cast(mage, target, IronSpellProfile.fromElement(element),
                IronSpellIntent.DIRECT_DAMAGE, floor);
    }

    /** Casts one enabled registry spell matching the requested tactical intent. */
    public static boolean cast(Mob caster, LivingEntity hostileTarget, IronSpellProfile profile,
                               IronSpellIntent intent, int floor) {
        if (caster.level().isClientSide) return false;

        List<AbstractSpell> enabled = enabledSpells(profile, intent);
        if (enabled.isEmpty() && intent != IronSpellIntent.DIRECT_DAMAGE) {
            enabled = enabledSpells(profile, IronSpellIntent.DIRECT_DAMAGE);
            intent = IronSpellIntent.DIRECT_DAMAGE;
        }
        if (enabled.isEmpty()) return false;

        boolean requiresHostileAim = requiresHostileAim(intent);
        if (requiresHostileAim && hostileTarget == null) return false;
        if (hostileTarget != null) {
            if (intent == IronSpellIntent.MOBILITY) {
                faceAway(caster, hostileTarget);
            } else if (requiresHostileAim) {
                faceTarget(caster, hostileTarget);
                caster.setTarget(hostileTarget);
            }
        }

        AbstractSpell spell = enabled.get(caster.getRandom().nextInt(enabled.size()));
        int level = levelFor(spell, floor);
        try {
            spell.onCast(caster.level(), level, caster, CastSource.MOB, new MagicData(false));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<AbstractSpell> enabledSpells(IronSpellProfile profile,
                                                      IronSpellIntent intent) {
        List<AbstractSpell> enabled = new ArrayList<>();
        for (String id : IronSpellCatalog.spellIds(profile, intent)) {
            try {
                AbstractSpell spell = SpellRegistry.getSpell(id);
                if (spell != null && spell != SpellRegistry.none() && spell.isEnabled()) {
                    enabled.add(spell);
                }
            } catch (Throwable ignored) {
                // Soft registry IDs allow optional addon spells without linking addon classes.
            }
        }
        return enabled;
    }

    private static int levelFor(AbstractSpell spell, int floor) {
        int dungeonLevel = 1 + (Math.max(1, Math.min(100, floor)) - 1) / 20;
        int level = Math.max(spell.getMinLevel(), dungeonLevel);
        int max = spell.getMaxLevel();
        return max > 0 ? Math.min(level, max) : Math.max(1, level);
    }

    private static boolean requiresHostileAim(IronSpellIntent intent) {
        return intent == IronSpellIntent.DIRECT_DAMAGE
                || intent == IronSpellIntent.AREA_DAMAGE
                || intent == IronSpellIntent.CONTROL;
    }

    private static void faceTarget(Mob caster, LivingEntity target) {
        face(caster, target.getX() - caster.getX(), target.getZ() - caster.getZ(),
                (target.getY() + target.getBbHeight() * 0.5)
                        - (caster.getY() + caster.getEyeHeight()));
    }

    private static void faceAway(Mob caster, LivingEntity target) {
        face(caster, caster.getX() - target.getX(), caster.getZ() - target.getZ(), 0.0);
    }

    private static void face(Mob caster, double dx, double dz, double dy) {
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
        caster.setYRot(yaw);
        caster.yBodyRot = yaw;
        caster.setYHeadRot(yaw);
        caster.setXRot(pitch);
    }
}
