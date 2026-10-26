package club.someoneice.minepulse.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 首次挖掘时弹一次的"连锁方式"选择界面。
 * 上：按住 Shift 时连锁；下：直接挖就连锁。宽度跟着屏幕走，不会跑出屏幕外。
 */
public class ScreenAsk extends Screen {
    /** 按钮最大宽度，屏幕更窄时按 width - 60 收缩。 */
    private static final int MAX_BUTTON_WIDTH = 220;

    public ScreenAsk() {
        super(Component.translatable("screen.minepulse.title"));
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(MAX_BUTTON_WIDTH, this.width - 60);
        int x = (this.width - buttonWidth) / 2;
        int y = this.height / 2 - 8;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.minepulse.mode_shift"),
                        button -> this.finish(true))
                .bounds(x, y, buttonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.minepulse.mode_direct"),
                        button -> this.finish(false))
                .bounds(x, y + 24, buttonWidth, 20)
                .build());
    }

    private void finish(boolean shiftMode) {
        if (shiftMode) {
            ClientConfig.useShiftMode();
        } else {
            ClientConfig.useDefaultMode();
        }
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, this.height / 2 - 46, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("screen.minepulse.hint"),
                centerX, this.height / 2 - 32, 0xFFAAAAAA);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        // 关掉界面也算做过选择，别再弹第二次
        ClientConfig.SHOULD_ASK = false;
        ClientConfig.save();
        super.onClose();
    }
}
