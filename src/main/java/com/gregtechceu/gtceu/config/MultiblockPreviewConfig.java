package com.gregtechceu.gtceu.config;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MultiblockPreviewConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DEFAULT_COMMENT = "Order and visibility of multiblocks in the pattern preview widget. Newly detected multiblocks are automatically appended to 'order'. To hide a multiblock, add it to 'hidden'.";

    @Setter
    private static Path configPath;

    public static Path getConfigPath() {
        if (configPath == null) {
            try {
                configPath = FMLPaths.CONFIGDIR.get().resolve("gtceu").resolve("multiblock_preview.json");
            } catch (Throwable t) {
                configPath = Paths.get("config", "gtceu", "multiblock_preview.json");
            }
        }
        return configPath;
    }

    public static List<MultiblockMachineDefinition> getOrderedMultiblocks() {
        return getOrderedMultiblocks(getConfigPath());
    }

    public static List<MultiblockMachineDefinition> getOrderedMultiblocks(Path path) {
        List<MultiblockMachineDefinition> allMultis = GTRegistries.MACHINES.values().stream()
                .filter(MultiblockMachineDefinition.class::isInstance)
                .map(MultiblockMachineDefinition.class::cast)
                .filter(MultiblockMachineDefinition::isRenderXEIPreview)
                .toList();

        if (allMultis.isEmpty()) return Collections.emptyList();

        if (!Files.exists(path)) {
            dumpDefaultConfig(path, allMultis);
            return allMultis;
        }

        return loadAndResolve(path, allMultis);
    }

    private static void dumpDefaultConfig(Path path, List<MultiblockMachineDefinition> allMultis) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("__comment", DEFAULT_COMMENT);

            List<String> orderList = new ArrayList<>();
            for (MultiblockMachineDefinition def : allMultis) {
                orderList.add(def.getId().toString());
            }

            data.put("order", orderList);
            data.put("hidden", new ArrayList<String>());

            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            GTCEu.LOGGER.error("Failed to write multiblock preview config to {}", path, e);
        }
    }

    private static List<MultiblockMachineDefinition> loadAndResolve(Path path,
                                                                    List<MultiblockMachineDefinition> allMultis) {
        List<String> configuredOrder = new ArrayList<>();
        List<String> configuredHidden = new ArrayList<>();
        String comment = DEFAULT_COMMENT;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            if (json.has("__comment") && json.get("__comment").isJsonPrimitive()) {
                comment = json.get("__comment").getAsString();
            }

            Type listType = new TypeToken<List<String>>() {}.getType();

            if (json.has("order") && json.get("order").isJsonArray()) {
                List<String> loadedOrder = GSON.fromJson(json.get("order"), listType);
                if (loadedOrder != null) configuredOrder.addAll(loadedOrder);
            }

            if (json.has("hidden") && json.get("hidden").isJsonArray()) {
                List<String> loadedHidden = GSON.fromJson(json.get("hidden"), listType);
                if (loadedHidden != null) configuredHidden.addAll(loadedHidden);
            }
        } catch (Exception e) {
            GTCEu.LOGGER.error("Failed to read multiblock preview config from {}, using default order", path, e);
            return allMultis;
        }

        Set<String> hiddenIds = new LinkedHashSet<>(configuredHidden);
        List<String> activeOrderIds = new ArrayList<>();

        for (String entry : configuredOrder) {
            if (entry == null || entry.isBlank()) continue;
            activeOrderIds.add(entry.trim());
        }

        List<MultiblockMachineDefinition> visibleCandidates = new ArrayList<>();
        for (MultiblockMachineDefinition def : allMultis) {
            if (!isMultiblockMatched(def, hiddenIds)) {
                visibleCandidates.add(def);
            }
        }

        List<MultiblockMachineDefinition> result = new ArrayList<>();
        Set<MultiblockMachineDefinition> matchedInOrder = new HashSet<>();

        for (String id : activeOrderIds) {
            if (id == null || id.isBlank()) continue;
            for (MultiblockMachineDefinition def : visibleCandidates) {
                if (!matchedInOrder.contains(def) && matchesId(def, id)) {
                    result.add(def);
                    matchedInOrder.add(def);
                    break;
                }
            }
        }

        List<MultiblockMachineDefinition> newMultis = new ArrayList<>();
        for (MultiblockMachineDefinition def : visibleCandidates) {
            if (!matchedInOrder.contains(def)) {
                result.add(def);
                matchedInOrder.add(def);
                newMultis.add(def);
            }
        }

        if (!newMultis.isEmpty()) {
            for (MultiblockMachineDefinition newDef : newMultis) {
                configuredOrder.add(newDef.getId().toString());
            }
            saveConfig(path, comment, configuredOrder, configuredHidden);
        }

        return result;
    }

    private static void saveConfig(Path path, String comment, List<String> order, List<String> hidden) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("__comment", comment);
            data.put("order", order);
            data.put("hidden", hidden);

            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            GTCEu.LOGGER.error("Failed to update multiblock preview config at {}", path, e);
        }
    }

    public static boolean isMultiblockMatched(MultiblockMachineDefinition def, Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return false;
        for (String id : ids) {
            if (matchesId(def, id)) return true;
        }
        return false;
    }

    public static boolean matchesId(MultiblockMachineDefinition def, String id) {
        if (id == null || id.isBlank()) return false;
        id = id.trim();
        ResourceLocation defId = def.getId();
        String fullId = defId.toString();

        if (id.contains(":")) {
            return fullId.equalsIgnoreCase(id);
        } else {
            return defId.getPath().equalsIgnoreCase(id) || fullId.equalsIgnoreCase("gtceu:" + id);
        }
    }
}
