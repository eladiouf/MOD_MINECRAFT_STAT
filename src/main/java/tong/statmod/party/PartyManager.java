package tong.statmod.party;

import net.minecraft.server.level.ServerPlayer;
import tong.statmod.integration.FtbTeamsIntegration;
import java.util.*;

public class PartyManager {
    private static final Map<UUID, Set<UUID>> parties = new HashMap<>();

    public static void createParty(ServerPlayer leader) {
        parties.computeIfAbsent(leader.getUUID(), k -> new HashSet<>());
    }

    public static void joinParty(ServerPlayer joiner, ServerPlayer leader) {
        Set<UUID> party = parties.get(leader.getUUID());
        if (party == null) { createParty(leader); party = parties.get(leader.getUUID()); }
        party.add(joiner.getUUID());
    }

    public static void leaveParty(ServerPlayer player) {
        parties.values().forEach(p -> p.remove(player.getUUID()));
        parties.remove(player.getUUID());
    }

    public static Set<UUID> getPartyMembers(UUID playerId) {
        UUID leaderId = playerId;
        if (!parties.containsKey(playerId)) {
            leaderId = parties.entrySet().stream()
                .filter(e -> e.getValue().contains(playerId))
                .map(Map.Entry::getKey)
                .findFirst().orElse(playerId);
        }
        Set<UUID> members = parties.get(leaderId);
        if (members == null) return Collections.emptySet();
        Set<UUID> all = new HashSet<>(members);
        all.add(leaderId);
        return Set.copyOf(all);
    }

    public static boolean isInParty(UUID playerId) {
        return parties.values().stream().anyMatch(p -> p.contains(playerId))
            || parties.containsKey(playerId);
    }

    public static boolean areAllied(ServerPlayer p1, ServerPlayer p2) {
        if (FtbTeamsIntegration.isLoaded()) {
            return FtbTeamsIntegration.areAllied(p1, p2);
        }
        Set<UUID> party = getPartyMembers(p1.getUUID());
        return party.contains(p2.getUUID());
    }

    public static void disband(ServerPlayer leader) {
        parties.remove(leader.getUUID());
    }

    public static float getPartyShareBonus(int partySize) {
        return Math.min(0.5f, partySize * 0.1f);
    }

    public static void cleanup(UUID uuid) {
        parties.values().forEach(p -> p.remove(uuid));
        parties.remove(uuid);
    }
}
