package tong.statmod.integration.elementals;

import java.util.EnumSet;

public interface ElementalsRuntimePort {
    void setAllowedBranches(EnumSet<ElementalBranch> branches);

    default float chi() {
        throw new UnsupportedOperationException();
    }

    default float xp() {
        throw new UnsupportedOperationException();
    }

    default int level() {
        throw new UnsupportedOperationException();
    }

    default void setXp(float value) {
        throw new UnsupportedOperationException();
    }

    default void setChi(float value) {
        throw new UnsupportedOperationException();
    }

    default ElementalBranch activeBranch() {
        throw new UnsupportedOperationException();
    }

    default float maxXpForLevel(int level) {
        throw new UnsupportedOperationException();
    }

    default void setLevelAndXp(int level, float xp) {
        throw new UnsupportedOperationException();
    }
}
