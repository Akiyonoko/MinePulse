package club.someoneice.minepulse.client;

import club.someoneice.minepulse.core.ProgressState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 准星下方的挖掘进度条：一条细条 + "剩余 / 总数"，松手约 1.5 秒后淡出。
 */
public final class HudProgress {
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 5;
    private static final long HOLD_MILLIS = 1500L;
    private static final long FADE_MILLIS = 400L;

    private HudProgress() {
    }

    public static void render(GuiGraphics graphics) {
        if (!ClientConfig.SHOW_PROGRESS || !ProgressState.visible()) {
            return;
        }

        long idle = ProgressState.idleMillis();
        int alpha;
        if (idle <= HOLD_MILLIS) {
            alpha = 255;
        } else if (idle <= HOLD_MILLIS + FADE_MILLIS) {
            alpha = (int) (255.0F * (1.0F - (idle - HOLD_MILLIS) / (float) FADE_MILLIS));
        } else {
            return;
        }
        if (alpha <= 0) {
            return;
        }

        int total = ProgressState.total();
        int remaining = ProgressState.remaining();
        // 绿条画的是"还剩多少"：每挖一块短一截，挖完归零
        float percent = total <= 0 ? 0.0F : remaining / (float) total;

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight / 2 + 26;

        Minecraft minecraft = Minecraft.getInstance();
        Component label = Component.literal(remaining + " / " + total);

        // 外框
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, argb(alpha * 3 / 5, 0x000000));
        // 槽
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, argb(alpha * 3 / 5, 0x101010));
        // 剩余量
        int filled = Math.round(BAR_WIDTH * Math.max(0.0F, Math.min(1.0F, percent)));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + BAR_HEIGHT, argb(alpha, 0x6EE85A));
        }
        // 数字
        graphics.drawCenteredString(minecraft.font, label, screenWidth / 2, y - 11, argb(alpha, 0xFFFFFF));
    }

    private static int argb(int alpha, int rgb) {
        return ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }
}
