package tong.statmod.integration.ironspells;

import java.util.Map;
import tong.statmod.magic.LearnedSpellState;

public final class LearnedSpellBindingPolicy {
    public static final int BUTTON_BASE = 1000;

    private LearnedSpellBindingPolicy() {
    }

    public static int authorizedLevel(
            Map<String, Integer> learned, String spellId, int minLevel, int maxLevel) {
        if (learned == null || spellId == null || minLevel <= 0 || maxLevel < minLevel) {
            return 0;
        }
        Integer learnedLevel = learned.get(spellId);
        if (learnedLevel == null || learnedLevel <= 0) {
            return 0;
        }
        return Math.max(minLevel, Math.min(maxLevel, learnedLevel));
    }

    public static int optionFromButton(int buttonId) {
        int option = buttonId - BUTTON_BASE;
        return option >= 0 && option < LearnedSpellState.MAX_ENTRIES ? option : -1;
    }

    public static int buttonForOption(int option) {
        return option >= 0 && option < LearnedSpellState.MAX_ENTRIES
                ? BUTTON_BASE + option
                : -1;
    }
}
