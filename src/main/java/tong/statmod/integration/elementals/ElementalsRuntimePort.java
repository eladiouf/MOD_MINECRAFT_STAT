package tong.statmod.integration.elementals;

import java.util.EnumSet;

public interface ElementalsRuntimePort {
    void setAllowedBranches(EnumSet<ElementalBranch> branches);
}
