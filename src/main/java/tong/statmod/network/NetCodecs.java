package tong.statmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

final class NetCodecs {
    private NetCodecs() {}

    static final StreamCodec<ByteBuf, int[]> INT_ARRAY =
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).map(
                    list -> {
                        int[] out = new int[list.size()];
                        for (int i = 0; i < list.size(); i++) out[i] = list.get(i);
                        return out;
                    },
                    arr -> {
                        List<Integer> list = new ArrayList<>(arr.length);
                        for (int v : arr) list.add(v);
                        return list;
                    }
            );

    static final StreamCodec<ByteBuf, String[]> STRING_ARRAY =
            ByteBufCodecs.stringUtf8(32767).apply(ByteBufCodecs.list()).map(
                    list -> list.toArray(new String[0]),
                    arr -> List.of(arr)
            );
}
