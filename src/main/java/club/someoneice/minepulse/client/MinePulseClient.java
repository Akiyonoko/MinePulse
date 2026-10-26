package club.someoneice.minepulse.client;

import club.someoneice.minepulse.MinePulseMod;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端入口：每帧渲染结束后画进度条。
 * 配置在 ClientConfig 的静态初始化里读，不依赖任何初始化事件。
 */
@EventBusSubscriber(modid = MinePulseMod.MOD_ID, value = Dist.CLIENT)
public final class MinePulseClient {
    private MinePulseClient() {
    }

    /** 在"模组列表 → Mine Pulse → Config"里挂上我们的设置界面。 */
    public static void registerModConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new ScreenConfig(parent));
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        GuiGraphics graphics = event.getGuiGraphics();
        HudProgress.render(graphics);
    }
}
