package com.sorted.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sorted.SortedMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 槽位分类的人工覆写持久化：
 * 菜单键(类名) → 槽位索引 → 0=自动 1=强制参与 2=忽略。
 * 仅客户端偏好；执行时随 SortRequest 一并交给服务端。
 */
public final class OverrideStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, int[]> OVERRIDES = new HashMap<>();

    private OverrideStore() {}

    public static void load() {
        OVERRIDES.clear();
        Path p = path();
        if (!Files.exists(p)) return;
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                JsonArray arr = e.getValue().getAsJsonArray();
                int[] vals = new int[arr.size()];
                for (int i = 0; i < arr.size(); i++) vals[i] = arr.get(i).getAsInt();
                OVERRIDES.put(e.getKey(), vals);
            }
        } catch (IOException | RuntimeException ex) {
            SortedMod.LOGGER.warn("Failed to load Sorted overrides", ex);
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Map.Entry<String, int[]> e : OVERRIDES.entrySet()) {
                JsonArray arr = new JsonArray();
                for (int v : e.getValue()) arr.add(v);
                root.add(e.getKey(), arr);
            }
            Path p = path();
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (IOException | RuntimeException ex) {
            SortedMod.LOGGER.warn("Failed to save Sorted overrides", ex);
        }
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("sorted").resolve("slot_overrides.json");
    }

    public static int override(String menuKey, int slot) {
        int[] arr = OVERRIDES.get(menuKey);
        if (arr == null || slot < 0 || slot >= arr.length) return 0;
        return arr[slot];
    }

    public static int cycle(String menuKey, int slot, int maxSlots) {
        int[] arr = OVERRIDES.computeIfAbsent(menuKey, k -> new int[maxSlots]);
        if (slot < 0 || slot >= arr.length) return 0;
        arr[slot] = (arr[slot] + 1) % 3;
        save();
        return arr[slot];
    }

    public static void resetMenu(String menuKey) {
        if (OVERRIDES.remove(menuKey) != null) save();
    }

    public static int[] allOf(String menuKey, int slotCount) {
        int[] arr = OVERRIDES.get(menuKey);
        if (arr == null || arr.length != slotCount) return new int[0];
        return arr.clone();
    }
}
