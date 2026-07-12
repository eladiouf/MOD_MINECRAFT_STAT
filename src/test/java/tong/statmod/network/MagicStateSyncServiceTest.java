package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
        data.setMagicPoints(4);
        data.setSchoolMasteryProgress(MagicBranch.FIRE, 2);
        data.setSchoolPracticeMasteryProgress(MagicBranch.FIRE, 9);
        data.setMagicRace(MagicRace.DWARF);
        data.setChosenStartBranch(MagicBranch.FIRE);

        SyncMagicPayload payload = MagicStateSyncService.payload(data);

        assertArrayEquals(new String[]{"common/foundation/arcane_focus"}, payload.magicNodes());
        assertArrayEquals(new String[]{"irons_spellbooks:firebolt"}, payload.learnedSpells());
        assertEquals(4, payload.magicPoints());
        assertEquals(2, payload.masteryProgress()[MagicBranch.FIRE.ordinal()]);
        assertEquals(9, payload.practiceMasteryProgress()[MagicBranch.FIRE.ordinal()]);
        assertEquals(MagicRace.DWARF.ordinal(), payload.raceOrdinal());
        assertEquals(MagicBranch.FIRE.ordinal(), payload.startBranchOrdinal());
    }

    @Test
    void syncSendsPayloadAndRefreshesMirrorOnce() {
        PlayerStatData data = new PlayerStatData();
        data.setMagicPoints(7);
        AtomicReference<SyncMagicPayload> sent = new AtomicReference<>();
        AtomicInteger mirrorRuns = new AtomicInteger();

        MagicStateSyncService.sync(data, sent::set, mirrorRuns::incrementAndGet);

        assertEquals(7, sent.get().magicPoints());
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

    @Test
    void payloadCodecPreservesPracticeMastery() {
        SyncMagicPayload expected = new SyncMagicPayload(
                new String[]{"common/foundation/arcane_focus"},
                new String[]{"irons_spellbooks:firebolt"},
                4,
                new int[]{2, 0},
                new int[]{9, 0},
                MagicRace.DWARF.ordinal(),
                MagicBranch.FIRE.ordinal());
        ByteBuf buffer = Unpooled.buffer();

        try {
            SyncMagicPayload.CODEC.encode(buffer, expected);
            SyncMagicPayload restored = SyncMagicPayload.CODEC.decode(buffer);

            assertArrayEquals(expected.masteryProgress(), restored.masteryProgress());
            assertArrayEquals(expected.practiceMasteryProgress(), restored.practiceMasteryProgress());
            assertEquals(expected.raceOrdinal(), restored.raceOrdinal());
            assertEquals(expected.startBranchOrdinal(), restored.startBranchOrdinal());
        } finally {
            buffer.release();
        }
    }
}
