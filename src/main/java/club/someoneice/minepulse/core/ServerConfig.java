package club.someoneice.minepulse.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 服务端配置：config/minepulse_common.json
 * 由各条规则组成：一条规则可以是单个标签/方块，也可以是"标签 + 方块"的组合。
 */
public final class ServerConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Set<OreMark> ORE_MARKS = new LinkedHashSet<>();
    public static final Set<OreMark> TREE_MARKS = new LinkedHashSet<>();

    /** -1 表示不限制；否则一条脉最多扫这么多块。 */
    public static int MAX_SIZE_CACHE = 1024;

    /** true = 3×3×3 全方向（含斜角）；false = 退回原版手感（矿石只认 6 个正方向）。 */
    public static boolean DIAGONAL = true;

    private static final String FILE_NAME = "minepulse_common.json";

    private static final String DEFAULT_JSON = """
            {
              "ores": [
                "#minecraft:coal_ores",
                ["#minecraft:copper_ores", "#c:storage_blocks/raw_copper"],
                ["#minecraft:iron_ores", "#c:storage_blocks/raw_iron"],
                ["#minecraft:gold_ores", "#c:storage_blocks/raw_gold"],
                "#minecraft:redstone_ores",
                "#minecraft:diamond_ores",
                "#minecraft:emerald_ores",
                "#minecraft:lapis_ores",
                "#minecraft:nether_quartz_ore",
                "#minecraft:ancient_debris",
                "#c:ores/netherite_scrap",
                ["#c:ores/bauxite", "#c:ores/cinnabar", "#c:ores/galena", "#c:ores/iridium",
                 "#c:ores/lead", "#c:ores/peridot", "#c:ores/pyrite", "#c:ores/ruby",
                 "#c:ores/sapphire", "#c:ores/sheldonite", "#c:ores/silver", "#c:ores/sodalite",
                 "#c:ores/sphalerite", "#c:ores/tin", "#c:ores/tungsten"],
                ["#c:desh_ores", "#c:storage_blocks/raw_desh"],
                ["#c:ice_shard_ores", "#c:ostrum_ores", "#c:calorite_ores"],
                ["minecraft:mushroom_stem", "minecraft:red_mushroom_block", "minecraft:brown_mushroom_block"]
              ],
              "trees": [
                "#minecraft:logs"
              ],
              "maxSizeCache": 1024,
              "diagonal": true
            }
            """;

    private ServerConfig() {
    }

    public static void read() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        String json = DEFAULT_JSON;
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULT_JSON, StandardCharsets.UTF_8);
            } else {
                json = Files.readString(file, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.error("[minepulse] 读取 {} 失败，使用内置默认配置", FILE_NAME, e);
        }
        apply(json);
    }

    private static void apply(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            Set<OreMark> ores = new LinkedHashSet<>();
            Set<OreMark> trees = new LinkedHashSet<>();
            readMarks(root.getAsJsonArray("ores"), ores);
            readMarks(root.getAsJsonArray("trees"), trees);

            ORE_MARKS.clear();
            ORE_MARKS.addAll(ores);
            TREE_MARKS.clear();
            TREE_MARKS.addAll(trees);

            if (root.has("maxSizeCache") && !root.get("maxSizeCache").isJsonNull()) {
                MAX_SIZE_CACHE = root.get("maxSizeCache").getAsInt();
            }
            if (root.has("diagonal") && !root.get("diagonal").isJsonNull()) {
                DIAGONAL = root.get("diagonal").getAsBoolean();
            }
        } catch (Exception e) {
            LOGGER.error("[minepulse] 配置文件解析失败，使用内置默认配置", e);
            if (ORE_MARKS.isEmpty()) {
                ORE_MARKS.add(new OreMark(Set.of(tag("#minecraft:coal_ores"), tag("#minecraft:iron_ores"),
                        tag("#minecraft:diamond_ores")), Set.of()));
            }
        }
    }

    private static void readMarks(JsonArray array, Set<OreMark> out) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            out.add(parseMark(element));
        }
    }

    private static OreMark parseMark(JsonElement element) {
        Set<TagKey<Block>> tags = new HashSet<>();
        Set<Block> blocks = new HashSet<>();
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                addEntry(child.getAsString(), tags, blocks);
            }
        } else {
            addEntry(element.getAsString(), tags, blocks);
        }
        return new OreMark(Set.copyOf(tags), Set.copyOf(blocks));
    }

    private static void addEntry(String value, Set<TagKey<Block>> tags, Set<Block> blocks) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (value.startsWith("#")) {
            tags.add(tag(value));
        } else {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(value));
            if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
                blocks.add(block);
            }
        }
    }

    private static TagKey<Block> tag(String value) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.tryParse(value.substring(1)));
    }
}
