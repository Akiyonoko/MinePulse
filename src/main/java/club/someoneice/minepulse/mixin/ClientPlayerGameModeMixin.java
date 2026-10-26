package club.someoneice.minepulse.mixin;

import club.someoneice.minepulse.client.ClientConfig;
import club.someoneice.minepulse.client.ScreenAsk;
import club.someoneice.minepulse.core.ServerConfig;
import club.someoneice.minepulse.net.PlayerStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端这一侧不自己破坏方块：交给服务端按"最远一块"的规则处理。
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerGameModeMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public abstract GameType getPlayerMode();

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void minepulse$destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Player player = this.minecraft.player;
        Level world = this.minecraft.level;
        if (player == null || world == null) {
            return;
        }

        BlockState block = world.getBlockState(pos);
        boolean blocked = player.isCreative()
                || player.isSpectator()
                || !player.getMainHandItem().getItem().canAttackBlock(block, world, pos, player)
                || player.blockActionRestricted(world, pos, this.getPlayerMode());
        if (blocked) {
            return;
        }

        if (!(ClientConfig.REVERSAL == player.isShiftKeyDown())) {
            return;
        }

        boolean ore = ClientConfig.ENABLE_MINE_ORE
                && ServerConfig.ORE_MARKS.stream().anyMatch(mark -> mark.mark(block));
        boolean tree = ClientConfig.ENABLE_MINE_TREE
                && ServerConfig.TREE_MARKS.stream().anyMatch(mark -> mark.mark(block));

        if (ore || tree) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void minepulse$startDestroyBlock(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        Player player = this.minecraft.player;
        if (player == null) {
            return;
        }

        if (ClientConfig.SHOULD_ASK && this.minecraft.screen == null) {
            ClientConfig.SHOULD_ASK = false;
            ClientConfig.save();
            this.minecraft.setScreen(new ScreenAsk());
            cir.setReturnValue(false);
            return;
        }

        boolean active = ClientConfig.REVERSAL == player.isShiftKeyDown();
        PacketDistributor.sendToServer(new PlayerStatePacket(
                ClientConfig.ENABLE_MINE_ORE, ClientConfig.ENABLE_MINE_TREE, active));
    }
}
