package club.someoneice.minepulse;

import club.someoneice.minepulse.client.MinePulseClient;
import club.someoneice.minepulse.core.OreMark;
import club.someoneice.minepulse.core.ProgressState;
import club.someoneice.minepulse.core.ServerConfig;
import club.someoneice.minepulse.mixin.DropExperienceBlockMixin;
import club.someoneice.minepulse.net.BlockSoundPacket;
import club.someoneice.minepulse.net.PlayerStatePacket;
import club.someoneice.minepulse.net.ProgressPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 一脉相挖：站在一点开挖，每次击破都打掉这条脉里最远的那块，掉落物飞向你。
 */
@Mod(MinePulseMod.MOD_ID)
public final class MinePulseMod {
    public static final String MOD_ID = "minepulse";

    /** 玩家当前意愿：允许挖矿 / 允许砍树 / 此刻是否连锁。 */
    public static final Map<UUID, PlayerState> PLAYER_STATE = new ConcurrentHashMap<>();

    /** 每条脉的进度会话：[0]=初始总数, [1]=剩余, [2]=最后一次挖掘的 gameTime。 */
    private static final Map<UUID, long[]> SESSIONS = new ConcurrentHashMap<>();

    /** 超过这个 tick 数没继续挖，就认为换了一条脉。 */
    private static final int SESSION_TIMEOUT = 100;

    /** 挑目标块时最多对多少块做连通性检查，超过就跳过，免得卡主线程。 */
    private static final int MAX_CONNECTIVITY_CHECK = 400;

    public MinePulseMod(IEventBus modEventBus, ModContainer container) {
        ServerConfig.read();
        modEventBus.register(this);
        // 配置界面只在客户端注册；服务端不加载 ScreenConfig 相关类
        if (FMLEnvironment.dist.isClient()) {
            MinePulseClient.registerModConfigScreen(container);
        }
    }

    public record PlayerState(boolean ore, boolean tree, boolean active) {
    }

    // ---------------------------------------------------------------- 网络

    @SubscribeEvent
    public void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(PlayerStatePacket.TYPE, PlayerStatePacket.CODEC, (packet, context) ->
                context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player != null) {
                        PLAYER_STATE.put(player.getUUID(),
                                new PlayerState(packet.ore(), packet.tree(), packet.active()));
                    }
                }));

        registrar.playToClient(BlockSoundPacket.TYPE, BlockSoundPacket.CODEC, (packet, context) ->
                context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player == null) {
                        return;
                    }
                    Level world = player.level();
                    BlockPos pos = packet.pos();
                    BlockState state = world.getBlockState(pos);
                    world.levelEvent(player, 2001, pos, Block.getId(state));
                    world.playSound(player, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }));

        registrar.playToClient(ProgressPacket.TYPE, ProgressPacket.CODEC, (packet, context) ->
                context.enqueueWork(() -> ProgressState.update(packet.total(), packet.remaining())));
    }

    // ---------------------------------------------------------------- 入口

    public static void oreHook(Player player, ServerLevel world, BlockPos pos, OreMark mark) {
        Set<BlockPos> vein = collect(world, pos, state -> mark.mark(state));
        handleVein(player, world, pos, vein);
    }

    public static void logHook(Player player, ServerLevel world, BlockPos pos) {
        Block mark = world.getBlockState(pos).getBlock();
        Set<BlockPos> vein = collect(world, pos, state -> state.getBlock() == mark);
        handleVein(player, world, pos, vein);
    }

    /**
     * 从 origin 出发扫出连通的整条脉。
     * diagonal 打开时用完整的 3×3×3（26 邻），关掉时矿石退回 6 个正方向。
     */
    private static Set<BlockPos> collect(ServerLevel world, BlockPos origin, Predicate<BlockState> matcher) {
        Set<BlockPos> found = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos start = origin.immutable();
        found.add(start);
        queue.push(start);

        List<BlockPos> offsets = neighbours(origin);
        int budget = 0;
        while (!queue.isEmpty()) {
            if (ServerConfig.MAX_SIZE_CACHE != -1 && budget >= ServerConfig.MAX_SIZE_CACHE) {
                break;
            }
            BlockPos current = queue.pop();
            for (BlockPos offset : offsets) {
                BlockPos next = current.offset(offset);
                if (found.contains(next)) {
                    continue;
                }
                if (!matcher.test(world.getBlockState(next))) {
                    continue;
                }
                budget++;
                found.add(next);
                queue.push(next);
            }
        }
        return found;
    }

    private static List<BlockPos> neighbours(BlockPos origin) {
        List<BlockPos> offsets = new ArrayList<>(26);
        if (ServerConfig.DIAGONAL) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        offsets.add(new BlockPos(dx, dy, dz));
                    }
                }
            }
        } else {
            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                offsets.add(new BlockPos(direction.getStepX(), direction.getStepY(), direction.getStepZ()));
            }
        }
        return offsets;
    }

    private static void handleVein(Player player, ServerLevel world, BlockPos origin, Set<BlockPos> vein) {
        // vein 是"这一下开挖时还连着的所有方块"，含马上要挖掉的那块
        int remaining = Math.max(0, vein.size() - 1);
        long now = world.getGameTime();
        UUID id = player.getUUID();

        long[] session = SESSIONS.get(id);
        boolean fresh = session == null
                || vein.size() > session[1]          // 挖着挖着变大了 → 换了一条脉
                || now - session[2] > SESSION_TIMEOUT; // 中间停太久 → 重新计数
        long total = fresh ? vein.size() : Math.max(session[0], vein.size());
        SESSIONS.put(id, new long[]{total, remaining, now});

        BlockPos target = pickTarget(vein, origin);
        breakBlock(target, world.getBlockState(target), world, player, origin);

        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new ProgressPacket((int) total, remaining));
        }
    }

    /**
     * 挑出这一下要破坏的方块：优先挑"最远"的那块，但绝不挑 origin 自己——
     * origin 是玩家正在挖的方块，挖掉它就等于普通挖矿，连锁到此为止。
     * 其余候选里再优先挑"删掉它脉也不会断"的方块，工字型之类也能一路挖完。
     */
    private static BlockPos pickTarget(Set<BlockPos> vein, BlockPos origin) {
        if (vein.size() <= 1) {
            // 只剩玩家正在挖的这一块了，正常挖掉
            return origin;
        }

        int[] otherYs = vein.stream()
                .filter(pos -> !pos.equals(origin))
                .mapToInt(BlockPos::getY)
                .distinct()
                .toArray();
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int y : otherYs) {
            max = Math.max(max, y);
            min = Math.min(min, y);
        }
        int targetY = max < origin.getY() ? min : max;

        List<BlockPos> candidates = new ArrayList<>(vein.size());
        for (BlockPos pos : vein) {
            if (!pos.equals(origin)) {
                candidates.add(pos);
            }
        }
        // 先排目标 Y 层，同层再按水平距离从远到近
        candidates.sort((a, b) -> {
            boolean aPreferred = a.getY() == targetY;
            boolean bPreferred = b.getY() == targetY;
            if (aPreferred != bPreferred) {
                return aPreferred ? -1 : 1;
            }
            return Integer.compare(distance(b, origin), distance(a, origin));
        });

        boolean checkConnectivity = candidates.size() <= MAX_CONNECTIVITY_CHECK;
        for (BlockPos candidate : candidates) {
            if (!checkConnectivity || keepsConnected(vein, origin, candidate)) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private static int distance(BlockPos pos, BlockPos origin) {
        return Math.abs(pos.getX() - origin.getX()) + Math.abs(pos.getZ() - origin.getZ());
    }

    /** 假想删掉 removed 之后，从 origin 出发还能不能走遍脉里剩下的每一块。 */
    private static boolean keepsConnected(Set<BlockPos> vein, BlockPos origin, BlockPos removed) {
        int expected = vein.size() - 2;
        if (expected <= 0) {
            return true;
        }
        List<BlockPos> offsets = neighbours(origin);
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin);
        queue.push(origin);
        int found = 0;
        while (!queue.isEmpty()) {
            BlockPos current = queue.pop();
            for (BlockPos offset : offsets) {
                BlockPos next = current.offset(offset);
                if (next.equals(removed) || !vein.contains(next) || !seen.add(next)) {
                    continue;
                }
                found++;
                if (found == expected) {
                    return true;
                }
                queue.push(next);
            }
        }
        return false;
    }

    private static void breakBlock(BlockPos pos, BlockState state, ServerLevel world, Player player, BlockPos origin) {
        ItemStack tool = player.getMainHandItem();
        tool.mineBlock(world, state, pos, player);

        for (ItemStack drop : Block.getDrops(state, world, pos, null, player, tool.copy())) {
            Vec3 spawn = spawnPoint(player, origin);
            ItemEntity item = new ItemEntity(world, spawn.x, spawn.y, spawn.z, drop);
            item.setDefaultPickUpDelay();
            world.addFreshEntity(item);
            pushToPlayer(item, player);
        }

        if (state.getBlock() instanceof DropExperienceBlock experienceBlock
                && world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            int amount = ((DropExperienceBlockMixin) experienceBlock).getXpRange().sample(world.random);
            ExperienceOrb.award(world, Vec3.atCenterOf(origin), amount);
        }

        PacketDistributor.sendToAllPlayers(new BlockSoundPacket(pos));
        world.removeBlock(pos, false);
        world.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
    }

    private static Vec3 spawnPoint(Player player, BlockPos origin) {
        Vec3 center = new Vec3(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5);
        BlockPos feet = player.getOnPos();
        double y = feet.getY() < origin.getY() ? (origin.getY() - feet.getY() >= 2 ? -0.7 : 0.0) : 0.7;
        double x = feet.getX() != origin.getX() ? (feet.getX() > origin.getX() ? 0.7 : -0.7) : 0.0;
        double z = feet.getZ() != origin.getZ() ? (feet.getZ() > origin.getZ() ? 0.7 : -0.7) : 0.0;
        return center.add(x, y, z);
    }

    private static void pushToPlayer(ItemEntity item, Player player) {
        Vec3 to = player.position();
        Vec3 from = item.position();
        item.push((to.x - from.x) * 0.1, (to.y - from.y) * 0.1, (to.z - from.z) * 0.1);
    }
}
