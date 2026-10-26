package club.someoneice.minepulse.core;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * 一条"什么算矿/什么算树"的规则，由标签或具体方块组成。
 */
public record OreMark(Set<TagKey<Block>> tags, Set<Block> blocks) {
    public boolean mark(BlockState state) {
        for (TagKey<Block> tag : tags) {
            if (state.is(tag)) {
                return true;
            }
        }
        for (Block block : blocks) {
            if (state.is(block)) {
                return true;
            }
        }
        return false;
    }
}
