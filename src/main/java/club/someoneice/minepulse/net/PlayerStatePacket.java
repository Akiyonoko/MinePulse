package club.someoneice.minepulse.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：这个玩家此刻愿不愿意连锁挖掘、连锁砍树，以及 Shift 的当前判定状态。
 *
 * @param ore    允许连锁挖矿
 * @param tree   允许连锁砍树
 * @param active 此刻处于"连锁"档位（由 Shift 与反转设置共同决定）
 */
public record PlayerStatePacket(boolean ore, boolean tree, boolean active) implements CustomPacketPayload {
    public static final Type<PlayerStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("minepulse", "playerstate"));

    public static final StreamCodec<FriendlyByteBuf, PlayerStatePacket> CODEC = CustomPacketPayload.codec(
            (packet, buf) -> {
                buf.writeBoolean(packet.ore);
                buf.writeBoolean(packet.tree);
                buf.writeBoolean(packet.active);
            },
            buf -> new PlayerStatePacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
