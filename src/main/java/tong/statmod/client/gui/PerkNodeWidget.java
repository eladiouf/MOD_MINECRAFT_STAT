package tong.statmod.client.gui;

import tong.statmod.perks.PerkTier;

public final class PerkNodeWidget {
    public enum ClickResult {
        PERK_MISSING,
        ALREADY_UNLOCKED,
        LEVEL_TOO_LOW,
        NOT_ENOUGH_POINTS,
        UNLOCK_SENT
    }

    public static int tierColor(PerkTier tier) {
        return switch (tier) {
            case CORE -> 0xFF808080;
            case ACTIVE -> 0xFF55FF55;
            case SYNERGY -> 0xFF5555FF;
            case SITUATIONAL -> 0xFFFFFF55;
            case MASTERY -> 0xFFAA00AA;
            case TRANSCENDENCE -> 0xFFFFAA00;
        };
    }

    private PerkNodeWidget() {}
}
