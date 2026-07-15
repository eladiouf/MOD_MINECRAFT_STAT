package tong.statmod.progression.xp;

import java.util.UUID;

public record XpAction(
        XpActionKind kind,
        double magnitude,
        int quantity,
        int secondary,
        boolean dangerousTarget,
        UUID opponentId) {

    public static XpAction damage(XpActionKind kind, double damage) {
        return new XpAction(kind, damage, 0, 0, false, null);
    }

    public static XpAction combo(int length) {
        return new XpAction(XpActionKind.COMBO, 0, length, 0, false, null);
    }

    public static XpAction landing(double distance) {
        return new XpAction(XpActionKind.CONTROLLED_LANDING, distance, 0, 0, false, null);
    }

    public static XpAction kill(double maxHealth, boolean dangerous) {
        return new XpAction(XpActionKind.HOSTILE_KILL, maxHealth, 0, 0, dangerous, null);
    }

    public static XpAction biome() {
        return new XpAction(XpActionKind.BIOME_DISCOVERY, 0, 0, 0, false, null);
    }

    public static XpAction forging(int maxDamage, int count) {
        return new XpAction(XpActionKind.EQUIPMENT_CRAFTED, maxDamage, count, 0, false, null);
    }

    public static XpAction cooking(int count) {
        return new XpAction(XpActionKind.FOOD_COOKED, 0, count, 0, false, null);
    }

    public static XpAction alchemy(int effectCount, int amplifierSum) {
        return new XpAction(XpActionKind.POTION_BREWED, 0, effectCount, amplifierSum, false, null);
    }

    public static XpAction spellCast(int originalSpellLevel, int originalManaCost) {
        return new XpAction(
                XpActionKind.SPELL_CAST,
                originalManaCost,
                originalSpellLevel,
                0,
                false,
                null);
    }

    public static XpAction bookStudied(int rawXp) {
        return new XpAction(XpActionKind.BOOK_STUDIED, 0, rawXp, 0, false, null);
    }

    public static XpAction spellInscribed(int spellLevel, int rarityValue) {
        return new XpAction(
                XpActionKind.SPELL_INSCRIBED, 0, spellLevel, rarityValue, false, null);
    }

    public static XpAction magicDamageReceived(double damage) {
        return damage(XpActionKind.MAGIC_DAMAGE_RECEIVED, damage);
    }

    public XpAction withOpponent(UUID id) {
        return new XpAction(kind, magnitude, quantity, secondary, dangerousTarget, id);
    }
}
