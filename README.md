# MinePulse Continued

A NeoForge 1.21.1 port of **MinePulse** by [AmarokIce](https://github.com/AmarokIce/MinePulse), with bug fixes and a few quality-of-life additions.

站定一点开挖，矿石与树木会从最远的一块开始，一块块连掉落物一起飞向你 —— 直到整条矿脉、整棵大树被掏空为止。

## 关于这个 fork

上游 MinePulse（MPL-2.0）最后发布于 1.21.4，之后不再更新。这个分支把它移植到 **NeoForge 1.21.1**，并修了它在实际游玩中暴露出来的问题：

- **斜角识别（26 邻）**：原版矿脉只认 6 个正交方向，树木只认 18 邻（水平 3×3 + 上方两层），斜对角相连的矿物会断链。现在两种连锁都改用完整 3×3×3 邻域，斜着连的矿、斜上方的原木都能一次挖穿。
- **挖掘进度条**：挖矿或砍树时，准星下方显示一条细进度条和「剩余 / 总数」，绿条随剩余量递减，挖完约 1.5 秒后自动淡出。可在配置里关掉。
- **游戏内配置页**：模组列表 → MinePulse Continued → Config，四个开关（连锁挖矿 / 连锁砍树 / 触发方式 / 进度条），改动即时生效并写盘。
- **修复「关掉连锁挖矿 / 砍树不生效」**（上游 issue #7）：这两个开关以前读了但从没被使用，现在客户端拦截与服务端判断都会遵守。
- 去掉了 cloth-config 与作者私有 maven 依赖，配置改用游戏自带的 Gson 读写 JSON，客户端与服务端都能独立加载。

![矿石连锁](/img/Ore.gif)

## 玩法

- 默认：直接挖就连锁；按住 Shift 则只挖单块。
- 第一次开挖前会弹一次选择框，选「按住 Shift 时连锁」或「直接挖就连锁」。
- 触发方式随时可以在配置页里切换。

## 安装

- Minecraft **1.21.1** + **NeoForge 21.1.x**
- 把 `minepulse-1.1.0.jar` 放进 `mods/` 目录
- **客户端与服务端都需要安装**（真正破坏方块、分发掉落物的逻辑在服务端）

## 配置

| 文件 | 内容 |
| --- | --- |
| `config/minepulse_common.json` | 矿石标签 / 方块列表、树木标签、最大缓存数、`diagonal`（26 邻开关） |
| `config/minepulse_client.json` | 是否弹选择框、连锁挖矿/砍树开关、触发方式、进度条开关 |

## 构建

需要 **JDK 21** 与 **Gradle 8.8+**（仓库自带 wrapper 8.12.1），附带了两份构建脚本：

```bash
bash build.sh        # Linux / macOS / Git Bash
# 或
powershell -File build.ps1
```

也可以直接用 wrapper：

```bash
./gradlew build
```

产物在 `build/libs/`。

## 许可

本项目与被 fork 的上游一样采用 **MPL-2.0**，见 [LICENSE](LICENSE)。

- 原项目：https://github.com/AmarokIce/MinePulse
- 原作者：AmarokIce、yumo2333
