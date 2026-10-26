# MinePulse Continued 改造：技术决策

> 来源仓库：https://github.com/AmarokIce/MinePulse （Fabric 1.21.4，作者已停更）
> 本工程：MinePulse Continued，NeoForge 21.1.262 / MC 1.21.1 / Java 21

## 目标

1. 修「斜角线上的矿石/原木识别不到」——原作者在 issue 里自己也提到了。
2. 挖掘时给个进度条，看这条矿脉还剩多少块。
3. 顺手修 issue #7（关掉连锁挖矿/砍树不生效）。

## 决策与理由

### 移植到 NeoForge 1.21.1，而不是继续用 Fabric 分支

用户主玩的实例是 NeoForge 21.1.262（`自改1.21.1-NeoForge_21.1.228`）。
原仓库只有 Fabric 1.21.4 源码，虽然 Modrinth 上有第三方打的 neoforge-1.21.1 包，
但那个包是同一套代码的编译产物、bug 一模一样，所以直接把源码搬过来改。

### 砍掉 `cloth-config` 与作者私库 `club.someoneice.json:amarok-json-for-java`

原版配置读写走作者自己的 maven 私库（`http://maven.snowlyicewolf.club`），
依赖不稳、无法离线构建。改成直接用 MC 自带的 Gson 读写
`config/minepulse_common.json` 与 `config/minepulse_client.json`，
配置文件格式和键名保持原样，老配置能直接沿用。

### 斜角识别：矿石与树统一改 26 邻

原来矿石用 `Direction.values()`（6 个正交邻），树用
`BlockPos.betweenClosed(-1, 0, -1, 1, 1, 1)`（水平 3×3 × 两层 = 18 邻），
两者都会在斜对角处断链。统一改成 26 邻（除自身外的 3×3×3），
由 `ServerConfig.DIAGONAL`（配置键 `diagonal`，默认 `true`）控制，
需要回到旧行为时可关掉。

注意这会让单个矿脉的扩散范围变大：原本隔一条斜线的两处矿会连成一条，
这是修这个 bug 的预期结果，不是副作用。

### 进度条：HUD 细条 + 数字，自动淡出

不做常驻 GUI（会挡视野），只在挖掘连锁时出现：
- 位置：屏幕中心下方 `screenHeight/2 + 26`，宽 100、高 5。
- 内容：绿色进度条 + 文字「剩余 / 总数」。
- 生命周期：服务端每破坏一块就发一次 `minepulse:progress`，
  客户端记 `lastUpdate`；挖完后先保持 1.5 秒，再用 0.4 秒淡出。

数据来源是服务端 BFS 后的 `vein.size()`（总数）与剩余层数，
所以显示的是**真·整条矿脉**的剩余量，不是估算。

### 会话计数缓存在服务端

`PLAYER_STATE` 换成 `Map<UUID, PlayerState>`（记录 ore/tree/active），
键从玩家显示名改成 UUID（原来用 `getDisplayName().getString()`，
改名或多人同会串）。另外维护 `SESSIONS`：玩家从开始挖到挖完的
一次连锁记一个会话，超过 100 tick 没动静就清掉。

### 顺带修的 issue #7

原版 `ENABLE_MINE_ORE` / `ENABLE_MINE_TREE` 读进配置后**从未被使用**，
所以关掉开关也照样连锁。现在两个开关同时作用于：

- 客户端拦截（关掉就不再发"这一下由我接管"）
- 服务端执行（关掉就不会连锁）
- `PlayerStatePacket` 携带两个开关一起同步到服务端

## 已知遗留

- 经验只补一次（`DropExperienceBlock` 的 `xpRange` 只采一次值），
  没有按方块数补齐；原版行为也是这样，这次没动。
- `maxSizeCache` 只用于限制 BFS 上限，语义与原版一致。
- 原版 `ScreenConfig` 在 1.21.1 上改成 `ScreenAsk`，
  贴图与 uv 坐标沿用 `assets/minepulse/ui.png`（512×512）。

## D6 目标块的选择：不挖 origin，且不挖断连通性

- 背景：用户实测"剩四五块时连锁失效，当前这块被挖掉"。原因是最初的 `pickTarget` 只按"最远"挑，
  剩余块都在同一 Y 层时直接返回 origin —— 玩家正在挖的那块被破坏，客户端目标消失，连锁自然中断。
- 决策：只要脉里还有别的块，就绝不破坏 origin；origin 只在 `vein.size() == 1` 时按普通挖矿破坏。
- 决策：候选里优先选"删掉它之后剩余块仍与 origin 连通"的方块（非割点），其次才按距离退化。
  这样工字型、十字型的脉不会因为挖掉连接处而断成两段。
- 代价：每次挖掘最多做 n 次连通性 BFS；设 `MAX_CONNECTIVITY_CHECK = 400`，超过就跳过检查直接取最远块，
  避免一次挖掘卡主线程数十毫秒。
