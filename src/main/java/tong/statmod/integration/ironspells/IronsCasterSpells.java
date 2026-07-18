package tong.statmod.integration.ironspells;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.function.Supplier;

/**
 * Fait lancer de <b>vrais sorts Iron's Spellbooks</b> à un mob caster du groupe (projectiles +
 * sorts variés par école). On appelle directement {@link AbstractSpell#onCast} avec
 * {@link CastSource#MOB} : le sort réel se déclenche (boule de feu, missile magique, éclair…).
 *
 * <p>Classe volontairement isolée : elle référence les classes ISS, donc n'est chargée que quand
 * Iron's Spellbooks est présent (appel gardé par {@code ModList.isLoaded} côté appelant).
 */
public final class IronsCasterSpells {

    private IronsCasterSpells() {}

    // Pools de sorts OFFENSIFS (projectiles / instantanés) par école. Variété à chaque cast.
    private static final List<Supplier<AbstractSpell>> FIRE = List.of(
            () -> SpellRegistry.FIREBALL_SPELL.get(),
            () -> SpellRegistry.FIREBOLT_SPELL.get(),
            () -> SpellRegistry.MAGMA_BOMB_SPELL.get(),
            () -> SpellRegistry.FLAMING_BARRAGE_SPELL.get(),
            () -> SpellRegistry.SCORCH_SPELL.get(),
            () -> SpellRegistry.FIRECRACKER_SPELL.get());

    private static final List<Supplier<AbstractSpell>> FROST = List.of(
            () -> SpellRegistry.ICICLE_SPELL.get(),
            () -> SpellRegistry.ICE_SPIKES_SPELL.get(),
            () -> SpellRegistry.SNOWBALL_SPELL.get(),
            () -> SpellRegistry.FROSTBITE_SPELL.get(),
            () -> SpellRegistry.FROSTWAVE_SPELL.get());

    private static final List<Supplier<AbstractSpell>> STORM = List.of(
            () -> SpellRegistry.LIGHTNING_BOLT_SPELL.get(),
            () -> SpellRegistry.CHAIN_LIGHTNING_SPELL.get(),
            () -> SpellRegistry.BALL_LIGHTNING_SPELL.get(),
            () -> SpellRegistry.LIGHTNING_LANCE_SPELL.get(),
            () -> SpellRegistry.SHOCKWAVE_SPELL.get());

    private static final List<Supplier<AbstractSpell>> NECRO = List.of(
            () -> SpellRegistry.MAGIC_MISSILE_SPELL.get(),
            () -> SpellRegistry.WITHER_SKULL_SPELL.get(),
            () -> SpellRegistry.BLOOD_SLASH_SPELL.get(),
            () -> SpellRegistry.ACID_ORB_SPELL.get(),
            () -> SpellRegistry.ELDRITCH_BLAST_SPELL.get(),
            () -> SpellRegistry.BLOOD_NEEDLES_SPELL.get());

    private static final List<Supplier<AbstractSpell>> ARCANE = List.of(
            () -> SpellRegistry.MAGIC_MISSILE_SPELL.get(),
            () -> SpellRegistry.MAGIC_ARROW_SPELL.get(),
            () -> SpellRegistry.ELDRITCH_BLAST_SPELL.get(),
            () -> SpellRegistry.GUIDING_BOLT_SPELL.get(),
            () -> SpellRegistry.STARFALL_SPELL.get(),
            () -> SpellRegistry.ARROW_VOLLEY_SPELL.get());

    /** Lance un sort ISS aléatoire de l'école {@code element} vers {@code target}. Renvoie false si raté. */
    public static boolean cast(Mob mage, LivingEntity target, String element) {
        if (mage.level().isClientSide) return false;
        List<Supplier<AbstractSpell>> pool = switch (element == null ? "" : element) {
            case "FIRE" -> FIRE;
            case "FROST" -> FROST;
            case "STORM" -> STORM;
            case "NECRO" -> NECRO;
            default -> ARCANE;
        };
        AbstractSpell spell;
        try {
            spell = pool.get(mage.getRandom().nextInt(pool.size())).get();
        } catch (Throwable t) {
            return false;
        }
        if (spell == null) return false;

        faceTarget(mage, target);
        mage.setTarget(target);

        int lvl = 3 + mage.getRandom().nextInt(3); // 3..5
        int max = spell.getMaxLevel();
        if (max > 0 && lvl > max) lvl = max;
        if (lvl < 1) lvl = 1;

        try {
            spell.onCast(mage.level(), lvl, mage, CastSource.MOB, new MagicData(false));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Oriente le mob vers la cible (les sorts-projectiles partent dans la direction du regard). */
    private static void faceTarget(Mob mage, LivingEntity target) {
        double dx = target.getX() - mage.getX();
        double dz = target.getZ() - mage.getZ();
        double dy = (target.getY() + target.getBbHeight() * 0.5) - (mage.getY() + mage.getEyeHeight());
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));
        mage.setYRot(yaw);
        mage.yBodyRot = yaw;
        mage.setYHeadRot(yaw);
        mage.setXRot(pitch);
    }
}
