package tong.statmod.magic;

public record CastContext(
        String spellId,
        MagicBranch branch,
        double manaFraction,
        boolean hadImpact,
        boolean wasFreeCast,
        int spellLevel
) {}
