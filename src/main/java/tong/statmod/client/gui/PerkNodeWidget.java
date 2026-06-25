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

    public static int fillColor(PerkNodeVisualState state, boolean hovered, boolean selected) {
        int base = switch (state) {
            case UNLOCKED -> 0xFF243226;
            case AVAILABLE -> 0xFF1B2438;
            case LOCKED_STAT -> 0xFF241A1A;
            case LOCKED_POINTS -> 0xFF24201A;
            case LOCKED_PREREQ -> 0xFF201A24;
            case LOCKED_MIXED -> 0xFF1A1A1A;
        };
        if (selected) {
            return brighten(base, 0x00181818);
        }
        if (hovered) {
            return brighten(base, 0x000E0E0E);
        }
        return base;
    }

    public static int textColor(PerkNodeVisualState state) {
        return switch (state) {
            case UNLOCKED -> 0xFFFFFFFF;
            case AVAILABLE -> 0xFF9DF7BF;
            case LOCKED_STAT -> 0xFFFF9A9A;
            case LOCKED_POINTS -> 0xFFF1D38A;
            case LOCKED_PREREQ -> 0xFFD2B0FF;
            case LOCKED_MIXED -> 0xFF9A9A9A;
        };
    }

    public static int borderColor(PerkTier tier, boolean selected) {
        return selected ? 0xFFF4E7B3 : tierColor(tier);
    }

    private static int brighten(int color, int delta) {
        int rgb = (color & 0x00FFFFFF) + delta;
        rgb = Math.min(rgb, 0x00FFFFFF);
        return (color & 0xFF000000) | rgb;
    }

    private PerkNodeWidget() {}
}
