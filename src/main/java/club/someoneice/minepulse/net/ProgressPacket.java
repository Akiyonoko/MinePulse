package club.someoneice.minepulse.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 -> 客户端：当前这条矿脉/树的挖掘进度。
 *
 * @param total     这条脉刚被发现时一共有多少块
 * @param remaining 已经挖掉本次这一块之后还剩多少块
 */
public record ProgressPacket(int total, int remaining) implements CustomPacketPayload {
    public static final Type<ProgressPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("minepulse", "progress"));

    public static final StreamCodec<FriendlyByteBuf, ProgressPacket> CODEC = CustomPacketPayload.codec(
            (packet, buf) -> {
                buf.writeVarInt(packet.total);
                buf.writeVarInt(packet.remaining);
            },
            buf -> new ProgressPacket(buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
