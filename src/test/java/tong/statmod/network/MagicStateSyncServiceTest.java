package tong.statmod.network;

import org.junit.jupiter.api.Test;
import tong.statmod.magic.MagicBranch;
import tong.statmod.magic.MagicRace;
import tong.statmod.storage.PlayerStatData;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MagicStateSyncServiceTest {
    @Test
    void buildsPayloadFromPlayerMagicState() {
        PlayerStatData data = new PlayerStatData();
        data.addMagicNode("common/foundation/arcane_focus");
        data.learnSpell("irons_spellbooks:firebolt");
        data.setArcanePoints(4);
        data.setSchoolPoints(MagicBranch.FIRE, 2);
        data.setMagicRace(MagicRace.DWARF);
        data.setChosenStartBranch(MagicBranch.FIRE);

        SyncMagicPayload payload = MagicStateSyncService.payload(data);

        assertArrayEquals(new String[]{"common/foundation/arcane_focus"}, payload.magicNodes());
        assertArrayEquals(new String[]{"irons_spellbooks:firebolt"}, payload.learnedSpells());
        assertEquals(4, payload.arcanePoints());
        assertEquals(2, payload.schoolPoints()[MagicBranch.FIRE.ordinal()]);
        assertEquals(MagicRace.DWARF.ordinal(), payload.raceOrdinal());
        assertEquals(MagicBranch.FIRE.ordinal(), payload.startBranchOrdinal());
    }

    @Test
    void syncSendsPayloadAndRefreshesMirrorOnce() {
        PlayerStatData data = new PlayerStatData();
        data.setArcanePoints(7);
        AtomicReference<SyncMagicPayload> sent = new AtomicReference<>();
        AtomicInteger mirrorRuns = new AtomicInteger();

        MagicStateSyncService.sync(data, sent::set, mirrorRuns::incrementAndGet);

        assertEquals(7, sent.get().arcanePoints());
        assertEquals(1, mirrorRuns.get());
    }

    @Test
    void ignoresNullState() {
        AtomicReference<SyncMagicPayload> sent = new AtomicReference<>();
        AtomicInteger mirrorRuns = new AtomicInteger();

        MagicStateSyncService.sync(null, sent::set, mirrorRuns::incrementAndGet);

        assertNull(sent.get());
        assertEquals(0, mirrorRuns.get());
    }
}
