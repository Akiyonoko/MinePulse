package club.someoneice.minepulse.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 -> 客户端：某个方块被连锁破坏，播一次破坏音效与粒子。
 */
public record BlockSoundPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<BlockSoundPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("minepulse", "sendsound"));

    public static final StreamCodec<FriendlyByteBuf, BlockSoundPacket> CODEC = CustomPacketPayload.codec(
            (packet, buf) -> buf.writeBlockPos(packet.pos),
            buf -> new BlockSoundPacket(buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
