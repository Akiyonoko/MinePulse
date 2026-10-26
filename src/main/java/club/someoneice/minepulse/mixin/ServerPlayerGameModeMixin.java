package club.someoneice.minepulse.mixin;

import club.someoneice.minepulse.MinePulseMod;
import club.someoneice.minepulse.core.OreMark;
import club.someoneice.minepulse.core.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    @Shadow
    public abstract GameType getGameModeForPlayer();

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void minepulse$destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (this.player instanceof FakePlayer) {
            return;
        }
        if (this.player.isCreative() || this.player.isSpectator()) {
            return;
        }

        MinePulseMod.PlayerState state = MinePulseMod.PLAYER_STATE.get(this.player.getUUID());
        if (state == null || !state.active()) {
            return;
        }

        BlockState block = this.level.getBlockState(pos);
        if (this.player.blockActionRestricted(this.level, pos, this.getGameModeForPlayer())) {
            return;
        }
        if (!this.player.getMainHandItem().getItem().canAttackBlock(block, this.level, pos, this.player)) {
            return;
        }

        Optional<OreMark> ore = ServerConfig.ORE_MARKS.stream()
                .filter(mark -> mark.mark(block))
                .findFirst();
        if (ore.isPresent()) {
            if (state.ore()) {
                MinePulseMod.oreHook(this.player, this.level, pos, ore.get());
                cir.setReturnValue(true);
            }
            return;
        }

        if (state.tree() && ServerConfig.TREE_MARKS.stream().anyMatch(mark -> mark.mark(block))) {
            MinePulseMod.logHook(this.player, this.level, pos);
            cir.setReturnValue(true);
        }
    }
}
