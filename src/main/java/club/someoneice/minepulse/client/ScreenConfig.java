package club.someoneice.minepulse.client;

import club.someoneice.minepulse.net.PlayerStatePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 游戏内设置界面：连锁挖矿 / 连锁砍树 / 连锁触发方式 / 进度条。
 * 入口在 模组列表 → Mine Pulse → Config，改动即时生效并写入 config/minepulse_client.json。
 */
public class ScreenConfig extends Screen {
    private static final int MAX_WIDTH = 220;
    private static final int ROW = 24;

    private final Screen parent;

    private Button oreButton;
    private Button treeButton;
    private Button triggerButton;
    private Button barButton;

    public ScreenConfig(Screen parent) {
        super(Component.translatable("screen.minepulse.config_title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int width = Math.min(MAX_WIDTH, this.width - 60);
        int x = (this.width - width) / 2;
        int y = this.height / 2 - 58;

        this.oreButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.minepulse.opt.ore", onOff(ClientConfig.ENABLE_MINE_ORE)),
                        button -> {
                            ClientConfig.ENABLE_MINE_ORE = !ClientConfig.ENABLE_MINE_ORE;
                            this.apply();
                        })
                .bounds(x, y, width, 20)
                .build());

        this.treeButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.minepulse.opt.tree", onOff(ClientConfig.ENABLE_MINE_TREE)),
                        button -> {
                            ClientConfig.ENABLE_MINE_TREE = !ClientConfig.ENABLE_MINE_TREE;
                            this.apply();
                        })
                .bounds(x, y + ROW, width, 20)
                .build());

        this.triggerButton = this.addRenderableWidget(Button.builder(
                        triggerLabel(),
                        button -> {
                            ClientConfig.REVERSAL = !ClientConfig.REVERSAL;
                            this.apply();
                        })
                .bounds(x, y + ROW * 2, width, 20)
                .build());

        this.barButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.minepulse.opt.bar", onOff(ClientConfig.SHOW_PROGRESS)),
                        button -> {
                            ClientConfig.SHOW_PROGRESS = !ClientConfig.SHOW_PROGRESS;
                            this.apply();
                        })
                .bounds(x, y + ROW * 3, width, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.minepulse.done"),
                        button -> this.onClose())
                .bounds(x, y + ROW * 4 + 8, width, 20)
                .build());
    }

    private void apply() {
        ClientConfig.save();
        this.oreButton.setMessage(
                Component.translatable("screen.minepulse.opt.ore", onOff(ClientConfig.ENABLE_MINE_ORE)));
        this.treeButton.setMessage(
                Component.translatable("screen.minepulse.opt.tree", onOff(ClientConfig.ENABLE_MINE_TREE)));
        this.triggerButton.setMessage(triggerLabel());
        this.barButton.setMessage(
                Component.translatable("screen.minepulse.opt.bar", onOff(ClientConfig.SHOW_PROGRESS)));
        this.syncToServer();
    }

    /** 开关变化立刻同步给服务端，不用等下一次开挖。 */
    private void syncToServer() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        boolean active = ClientConfig.REVERSAL == this.minecraft.player.isShiftKeyDown();
        PacketDistributor.sendToServer(new PlayerStatePacket(
                ClientConfig.ENABLE_MINE_ORE, ClientConfig.ENABLE_MINE_TREE, active));
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "screen.minepulse.on" : "screen.minepulse.off");
    }

    private static Component triggerLabel() {
        return Component.translatable("screen.minepulse.opt.trigger",
                Component.translatable(ClientConfig.REVERSAL
                        ? "screen.minepulse.trigger.shift"
                        : "screen.minepulse.trigger.direct"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, this.height / 2 - 84, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("screen.minepulse.config_hint"),
                centerX, this.height / 2 + 46 + 22, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }
}
