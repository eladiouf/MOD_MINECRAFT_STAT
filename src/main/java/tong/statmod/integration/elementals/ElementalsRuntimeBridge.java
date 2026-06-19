package tong.statmod.integration.elementals;

import commonnetwork.api.Network;
import dev.saperate.elementals.data.Bender;
import dev.saperate.elementals.data.PlayerData;
import dev.saperate.elementals.elements.Element;
import dev.saperate.elementals.network.packets.common.SyncLevelPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Locale;

public class ElementalsRuntimeBridge implements ElementalsRuntimePort {
    @Override
    public void setAllowedBranches(EnumSet<ElementalBranch> branches) {
        throw new UnsupportedOperationException("Use the server overload");
    }

    public EnumSet<ElementalBranch> branches(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        EnumSet<ElementalBranch> result = EnumSet.noneOf(ElementalBranch.class);
        for (Element element : new ArrayList<>(data.elements)) {
            ElementalBranch branch = byElementName(element.getName());
            if (branch != null) {
                result.add(branch);
            }
        }
        return result;
    }

    public void setAllowedBranches(ServerPlayer player, EnumSet<ElementalBranch> allowed) {
        Bender bender = Bender.getBender(player);
        for (ElementalBranch branch : ElementalBranch.values()) {
            Element element = Element.getElement(branch.elementalsName());
            if (element == null) {
                continue;
            }
            if (allowed.contains(branch) && !bender.hasElement(element)) {
                bender.addElement(element, false);
            }
            if (!allowed.contains(branch) && bender.hasElement(element)) {
                bender.removeElement(element, false);
            }
        }
        bender.bindDefaultAbilities();
        bender.syncElements();
    }

    public float chi(ServerPlayer player) {
        return PlayerData.get(player).chi;
    }

    public float xp(ServerPlayer player) {
        return PlayerData.get(player).xp;
    }

    public int level(ServerPlayer player) {
        return PlayerData.get(player).level;
    }

    public void setXp(ServerPlayer player, float value) {
        PlayerData.get(player).xp = Math.max(0.0f, value);
    }

    public float maxXpForLevel(int level) {
        return Bender.getMaxXp(level);
    }

    public void setLevelAndXp(ServerPlayer player, int level, float xp) {
        PlayerData data = PlayerData.get(player);
        data.level = Math.max(0, level);
        data.xp = Math.max(0.0f, xp);
        Network.getNetworkHandler().sendToClient(new SyncLevelPacket(data.level, data.xp), player);
    }

    public void setChi(ServerPlayer player, float value) {
        PlayerData.get(player).chi = Math.max(0.0f, value);
        Bender.getBender(player).syncChi();
    }

    public ElementalBranch activeBranch(ServerPlayer player) {
        Element active = Bender.getBender(player).getElement();
        return active == null ? null : byElementName(active.getName());
    }

    private static ElementalBranch byElementName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return ElementalBranch.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
