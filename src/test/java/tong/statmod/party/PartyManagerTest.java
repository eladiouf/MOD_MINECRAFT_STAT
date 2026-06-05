package tong.statmod.party;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PartyManagerTest {

    @Test
    @SuppressWarnings("unchecked")
    void getPartyMembers_returnsImmutableSnapshot() throws Exception {
        Field partiesField = PartyManager.class.getDeclaredField("parties");
        partiesField.setAccessible(true);
        Map<UUID, Set<UUID>> parties = (Map<UUID, Set<UUID>>) partiesField.get(null);
        Map<UUID, Set<UUID>> previous = new HashMap<>(parties);
        parties.clear();

        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        parties.put(leader, new HashSet<>(Set.of(member)));

        try {
            Set<UUID> party = PartyManager.getPartyMembers(leader);
            assertThrows(UnsupportedOperationException.class, party::clear);
        } finally {
            parties.clear();
            parties.putAll(previous);
        }
    }
}
