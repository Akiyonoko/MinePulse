package club.someoneice.minepulse.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 客户端配置：config/minepulse_client.json
 */
public final class ClientConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "minepulse_client.json";

    /** 首次挖掘时弹一次"连锁方式"选择界面。 */
    public static boolean SHOULD_ASK = true;
    /** 允许连锁挖矿。 */
    public static boolean ENABLE_MINE_ORE = true;
    /** 允许连锁砍树。 */
    public static boolean ENABLE_MINE_TREE = true;
    /** 反转 Shift 的作用（默认：不按 Shift 就连锁）。 */
    public static boolean REVERSAL = false;
    /** 是否显示挖掘进度条。 */
    public static boolean SHOW_PROGRESS = true;

    private ClientConfig() {
    }

    static {
        load();
    }

    public static void load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                save();
                return;
            }
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            SHOULD_ASK = read(root, "should_ask", SHOULD_ASK);
            ENABLE_MINE_ORE = read(root, "enable_mine_ore", ENABLE_MINE_ORE);
            ENABLE_MINE_TREE = read(root, "enable_mine_tree", ENABLE_MINE_TREE);
            REVERSAL = read(root, "reversal", REVERSAL);
            SHOW_PROGRESS = read(root, "show_progress", SHOW_PROGRESS);
        } catch (Exception e) {
            LOGGER.error("[minepulse] 读取 {} 失败，使用默认设置", FILE_NAME, e);
        }
    }

    public static void save() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("should_ask", SHOULD_ASK);
            root.addProperty("enable_mine_ore", ENABLE_MINE_ORE);
            root.addProperty("enable_mine_tree", ENABLE_MINE_TREE);
            root.addProperty("reversal", REVERSAL);
            root.addProperty("show_progress", SHOW_PROGRESS);
            StringBuilder out = new StringBuilder("{\n");
            out.append("  \"should_ask\": ").append(SHOULD_ASK).append(",\n");
            out.append("  \"enable_mine_ore\": ").append(ENABLE_MINE_ORE).append(",\n");
            out.append("  \"enable_mine_tree\": ").append(ENABLE_MINE_TREE).append(",\n");
            out.append("  \"reversal\": ").append(REVERSAL).append(",\n");
            out.append("  \"show_progress\": ").append(SHOW_PROGRESS).append("\n}\n");
            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("[minepulse] 写入 {} 失败", FILE_NAME, e);
        }
    }

    private static boolean read(JsonObject root, String key, boolean fallback) {
        if (root.has(key) && !root.get(key).isJsonNull()) {
            return root.get(key).getAsBoolean();
        }
        return fallback;
    }

    /** 选"按住 Shift 时连锁"。 */
    public static void useShiftMode() {
        REVERSAL = true;
        SHOULD_ASK = false;
        save();
    }

    /** 选"不按 Shift 时连锁"（默认）。 */
    public static void useDefaultMode() {
        REVERSAL = false;
        SHOULD_ASK = false;
        save();
    }
}
