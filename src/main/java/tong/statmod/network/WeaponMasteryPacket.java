package tong.statmod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import tong.statmod.client.ClientStatsCache;

import java.util.function.Supplier;

public class WeaponMasteryPacket {
    private final int[] levels;
    private final int[] xp;

    public WeaponMasteryPacket(int[] levels, int[] xp) {
        this.levels = NetworkPayloadRules.requireWeaponArray(levels, "weaponLevels");
        this.xp = NetworkPayloadRules.requireWeaponArray(xp, "weaponXp");
        NetworkPayloadRules.requireMatchingWeaponArrays(this.levels, this.xp, "weapon mastery sync");
    }

    public static void encode(WeaponMasteryPacket packet, FriendlyByteBuf buf) {
        buf.writeVarIntArray(packet.levels);
        buf.writeVarIntArray(packet.xp);
    }

    public static WeaponMasteryPacket decode(FriendlyByteBuf buf) {
        return new WeaponMasteryPacket(
            buf.readVarIntArray(NetworkPayloadRules.MAX_WEAPON_COUNT),
            buf.readVarIntArray(NetworkPayloadRules.MAX_WEAPON_COUNT)
        );
    }

    public static void handle(WeaponMasteryPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientStatsCache.updateWeaponMastery(packet.levels, packet.xp));
        ctx.get().setPacketHandled(true);
    }
}
