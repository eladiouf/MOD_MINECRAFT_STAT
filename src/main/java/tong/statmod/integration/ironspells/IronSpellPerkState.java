package tong.statmod.integration.ironspells;

import tong.statmod.magic.MagicBranch;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class IronSpellPerkState {
    private static final Map<UUID, Long> LAST_CAST_TIME = new ConcurrentHashMap<>();
    private static final Map<UUID, MagicBranch> LAST_CAST_BRANCH = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> BRANCH_CHAIN = new ConcurrentHashMap<>();

    private IronSpellPerkState() {}

    static boolean isQuickCast(UUID uuid, long now, long windowMs) {
        Long previous = LAST_CAST_TIME.get(uuid);
        return previous != null && now - previous <= windowMs;
    }

    static int recordCast(UUID uuid, MagicBranch branch, long now, long chainWindowMs) {
        Long previousTime = LAST_CAST_TIME.get(uuid);
        MagicBranch previousBranch = LAST_CAST_BRANCH.get(uuid);
        int nextChain = branch != null
                && branch == previousBranch
                && previousTime != null
                && now - previousTime <= chainWindowMs
                ? BRANCH_CHAIN.getOrDefault(uuid, 1) + 1
                : 1;

        LAST_CAST_TIME.put(uuid, now);
        if (branch == null) {
            LAST_CAST_BRANCH.remove(uuid);
        } else {
            LAST_CAST_BRANCH.put(uuid, branch);
        }
        BRANCH_CHAIN.put(uuid, nextChain);
        return nextChain;
    }

    static int currentBranchChain(UUID uuid, MagicBranch branch, long now, long chainWindowMs) {
        Long previousTime = LAST_CAST_TIME.get(uuid);
        MagicBranch previousBranch = LAST_CAST_BRANCH.get(uuid);
        if (branch == null || branch != previousBranch || previousTime == null || now - previousTime > chainWindowMs) {
            return 0;
        }
        return BRANCH_CHAIN.getOrDefault(uuid, 0);
    }

    static void clear(UUID uuid) {
        LAST_CAST_TIME.remove(uuid);
        LAST_CAST_BRANCH.remove(uuid);
        BRANCH_CHAIN.remove(uuid);
    }
}
