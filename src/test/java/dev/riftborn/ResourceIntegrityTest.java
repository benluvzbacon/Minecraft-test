package dev.riftborn;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Offline file-level checks. Registry codecs and gameplay are tested in the GameTest server. */
class ResourceIntegrityTest {
    private static final Path RES = Path.of("src/main/resources");
    private static final Path ASSETS = RES.resolve("assets/riftborn");
    private static final Path DATA = RES.resolve("data/riftborn");
    private static final Set<String> BLOCKS = Set.of("rift_stone", "rift_anchor", "guardian_altar", "void_bloom");
    private static final Set<String> MOBS = Set.of("rift_stalker", "void_brute", "rift_wisp", "rift_guardian");
    private static final Set<String> ITEMS = Set.of("rift_shard", "rift_dust", "void_fragment", "rift_core", "rift_heart", "rift_compass", "riftblade", "rift_helmet", "rift_chestplate", "rift_leggings", "rift_boots");

    private static List<Path> files(Path path, String suffix) throws IOException {
        try (var stream = Files.walk(path)) { return stream.filter(p -> p.toString().endsWith(suffix)).sorted().toList(); }
    }
    private static JsonObject json(Path path) throws IOException { return JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
    private static void exists(Path path) { assertTrue(Files.isRegularFile(path), () -> "Missing resource: " + path); }

    @Test void everyJsonParsesAndPathsAreLowercase() throws IOException {
        for (Path path : files(RES, ".json")) {
            assertNotNull(json(path));
            assertEquals(path.toString().toLowerCase(java.util.Locale.ROOT), path.toString(), "Invalid identifier path");
        }
        for (String legacy : List.of("recipes", "loot_tables", "advancements", "structures")) {
            assertFalse(Files.exists(DATA.resolve(legacy)), "1.21.1 uses singular datapack registry directories");
        }
    }

    @Test void everyTextureIsAReadablePowerOfTwoPng() throws IOException {
        for (Path path : files(ASSETS, ".png")) {
            var image = ImageIO.read(path.toFile());
            assertNotNull(image, path.toString());
            assertEquals(0, image.getWidth() & (image.getWidth() - 1), path.toString());
            assertEquals(0, image.getHeight() & (image.getHeight() - 1), path.toString());
        }
    }

    @Test void everyRegistryEntryHasItsRequiredAssets() throws IOException {
        JsonObject language = json(ASSETS.resolve("lang/en_us.json"));
        for (String block : BLOCKS) {
            exists(ASSETS.resolve("blockstates/" + block + ".json"));
            exists(ASSETS.resolve("models/block/" + block + ".json"));
            exists(ASSETS.resolve("models/item/" + block + ".json"));
            exists(DATA.resolve("loot_table/blocks/" + block + ".json"));
            assertTrue(language.has("block.riftborn." + block));
        }
        for (String item : ITEMS) {
            exists(ASSETS.resolve("models/item/" + item + ".json"));
            assertTrue(language.has("item.riftborn." + item));
            assertTrue(language.has("item.riftborn." + item + ".tooltip"));
        }
        for (String mob : MOBS) {
            exists(ASSETS.resolve("textures/entity/" + mob + ".png"));
            exists(ASSETS.resolve("models/item/" + mob + "_spawn_egg.json"));
            exists(DATA.resolve("loot_table/entities/" + mob + ".json"));
            assertTrue(language.has("entity.riftborn." + mob));
        }
    }

    @Test void customModelAndTextureReferencesExist() throws IOException {
        for (Path path : files(ASSETS.resolve("models"), ".json")) {
            JsonObject model = json(path);
            if (model.has("parent") && model.get("parent").getAsString().startsWith("riftborn:")) {
                exists(ASSETS.resolve("models/" + model.get("parent").getAsString().substring(9) + ".json"));
            }
            if (model.has("textures")) {
                for (JsonElement texture : model.getAsJsonObject("textures").asMap().values()) {
                    String id = texture.getAsString();
                    if (id.startsWith("riftborn:")) exists(ASSETS.resolve("textures/" + id.substring(9) + ".png"));
                }
            }
        }
        for (Path source : files(Path.of("src/client/java"), ".java")) {
            var matcher = Pattern.compile("Riftborn\\.id\\(\"(textures/[^\"]+)\"\\)").matcher(Files.readString(source));
            while (matcher.find()) exists(ASSETS.resolve(matcher.group(1)));
        }
        for (JsonElement texture : json(ASSETS.resolve("particles/rift_mote.json")).getAsJsonArray("textures")) {
            exists(ASSETS.resolve("textures/particle/" + texture.getAsString().substring(9) + ".png"));
        }
    }

    @Test void translationsUsedByCodeExist() throws IOException {
        JsonObject language = json(ASSETS.resolve("lang/en_us.json"));
        for (Path source : files(Path.of("src/main/java"), ".java")) {
            var matcher = Pattern.compile("\"((?:message|direction|location|boss|entity|item|itemGroup)\\.riftborn(?:\\.[a-z_]+)*)\"").matcher(Files.readString(source));
            while (matcher.find()) assertTrue(language.has(matcher.group(1)), () -> "Missing translation in " + source);
        }
    }

    @Test void commonCodeDoesNotReferenceClientClasses() throws IOException {
        for (Path source : files(Path.of("src/main/java"), ".java")) {
            String content = Files.readString(source);
            assertFalse(content.contains("net.minecraft.client"), source.toString());
            assertFalse(content.contains("dev.riftborn.client"), source.toString());
            assertFalse(content.contains("dev.riftborn.entity.client"), source.toString());
        }
    }

    @Test void structuresHaveValidPalettesLootAndLimitedMobs() throws IOException {
        for (Path path : files(DATA.resolve("structure"), ".nbt")) {
            Map<?, ?> template;
            try (var input = new DataInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
                assertEquals(10, input.readUnsignedByte());
                input.readUTF();
                template = (Map<?, ?>) readTag(input, 10);
                assertEquals(-1, input.read(), "Unexpected trailing NBT data");
            }
            List<?> size = (List<?>) template.get("size");
            List<?> palette = (List<?>) template.get("palette");
            List<?> entities = (List<?>) template.get("entities");
            assertTrue(entities.size() >= 1 && entities.size() <= 3);
            Set<String> names = new HashSet<>();
            for (Object p : palette) names.add((String) ((Map<?, ?>) p).get("Name"));
            assertTrue(names.contains("riftborn:rift_stone"));
            assertTrue(names.contains("riftborn:rift_anchor"));
            assertTrue(names.contains("minecraft:chest"));
            Set<List<?>> occupied = new HashSet<>();
            for (Object value : (List<?>) template.get("blocks")) {
                Map<?, ?> block = (Map<?, ?>) value;
                List<?> pos = (List<?>) block.get("pos");
                assertTrue(occupied.add(pos), "Duplicate position in " + path);
                for (int i = 0; i < 3; i++) {
                    int n = (int) pos.get(i);
                    assertTrue(n >= 0 && n < (int) size.get(i), path.toString());
                }
                assertTrue((int) block.get("state") < palette.size());
                if (block.get("nbt") instanceof Map<?, ?> data && data.get("LootTable") instanceof String table) {
                    exists(DATA.resolve("loot_table/" + table.substring(9) + ".json"));
                }
            }
            for (Object entity : entities) {
                String id = (String) ((Map<?, ?>) ((Map<?, ?>) entity).get("nbt")).get("id");
                assertTrue(id.startsWith("riftborn:") && MOBS.contains(id.substring(9)));
            }
        }
    }

    @Test void jigsawPoolsAndPlacementsAreConsistent() throws IOException {
        for (Path path : files(DATA.resolve("worldgen/structure"), ".json")) {
            JsonObject structure = json(path);
            String pool = structure.get("start_pool").getAsString().substring(9);
            JsonObject templatePool = json(DATA.resolve("worldgen/template_pool/" + pool + ".json"));
            for (JsonElement entry : templatePool.getAsJsonArray("elements")) {
                String template = entry.getAsJsonObject().getAsJsonObject("element").get("location").getAsString().substring(9);
                exists(DATA.resolve("structure/" + template + ".nbt"));
            }
            JsonObject placement = json(DATA.resolve("worldgen/structure_set/" + pool + ".json")).getAsJsonObject("placement");
            assertTrue(placement.get("spacing").getAsInt() > placement.get("separation").getAsInt());
        }
    }

    @Test void heartIsNotCraftableAndBossAlwaysDropsOne() throws IOException {
        for (Path recipe : files(DATA.resolve("recipe"), ".json")) {
            assertNotEquals("riftborn:rift_heart", json(recipe).getAsJsonObject("result").get("id").getAsString());
        }
        JsonObject heart = json(DATA.resolve("loot_table/entities/rift_guardian.json"))
                .getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("riftborn:rift_heart", heart.get("name").getAsString());
        assertEquals(1, heart.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count").get("min").getAsInt());
    }

    @Test void armorHasBothNativeModelLayersAndRecipes() throws IOException {
        for (int layer : new int[]{1, 2}) {
            var texture = ImageIO.read(ASSETS.resolve("textures/models/armor/rift_layer_" + layer + ".png").toFile());
            assertNotNull(texture); assertEquals(64, texture.getWidth()); assertEquals(32, texture.getHeight());
        }
        for (String name : List.of("rift_helmet", "rift_chestplate", "rift_leggings", "rift_boots")) {
            var recipe = json(DATA.resolve("recipe/" + name + ".json"));
            assertEquals("riftborn:" + name, recipe.getAsJsonObject("result").get("id").getAsString());
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
            exists(ASSETS.resolve("textures/item/" + name + ".png"));
        }
    }

    private static Object readTag(DataInputStream in, int type) throws IOException {
        return switch (type) {
            case 1 -> in.readByte();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 8 -> in.readUTF();
            case 9 -> {
                int subtype = in.readUnsignedByte(), length = in.readInt();
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < length; i++) list.add(readTag(in, subtype));
                yield list;
            }
            case 10 -> {
                Map<String, Object> map = new LinkedHashMap<>();
                int subtype;
                while ((subtype = in.readUnsignedByte()) != 0) map.put(in.readUTF(), readTag(in, subtype));
                yield map;
            }
            default -> throw new IOException("Unexpected NBT type " + type);
        };
    }
}
