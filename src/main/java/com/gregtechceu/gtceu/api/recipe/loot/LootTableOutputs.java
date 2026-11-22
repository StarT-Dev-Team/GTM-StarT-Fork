package com.gregtechceu.gtceu.api.recipe.loot;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.server.ServerLifecycleHooks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.*;

/**
 * Turns loot table ids saved by {@code GTRecipeBuilder#outputLootTable} into chanced item outputs.
 * <p>
 * Loot tables don't exist yet when recipes are generated, so only the id is saved and the table is read when the
 * server loads the recipe. Each entry's chance is how likely it is to drop in one roll.
 */
public final class LootTableOutputs {

    public static final String DATA_KEY = "loot_table_outputs";
    public static final String DISPLAY_KEY = "loot_table_slots";
    private static final int MAX_DEPTH = 8;

    @Nullable
    private static volatile ResourceManager reloadingResources;

    private LootTableOutputs() {}

    public static final PreparableReloadListener RELOAD_LISTENER = (barrier, resourceManager, preparationsProfiler,
                                                                    reloadProfiler, backgroundExecutor,
                                                                    gameExecutor) -> {
        reloadingResources = resourceManager;
        return barrier.wait(null);
    };

    public static CompoundTag entry(ResourceLocation table, int tierChanceBoost) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", table.toString());
        tag.putInt("tierChanceBoost", tierChanceBoost);
        return tag;
    }

    public static void addTo(CompoundTag data, ResourceLocation table, int tierChanceBoost) {
        ListTag tables = data.getList(DATA_KEY, Tag.TAG_COMPOUND);
        tables.add(entry(table, tierChanceBoost));
        data.put(DATA_KEY, tables);
    }

    public static GTRecipe expand(GTRecipe recipe) {
        if (!recipe.data.contains(DATA_KEY, Tag.TAG_LIST)) return recipe;

        ListTag tables = recipe.data.getList(DATA_KEY, Tag.TAG_COMPOUND);
        CompoundTag data = recipe.data.copy();
        data.remove(DATA_KEY);

        ResourceManager resources = getResourceManager();
        int existingItems = recipe.outputs.getOrDefault(ItemRecipeCapability.CAP, List.of()).size();
        List<Content> resolved = new ArrayList<>();
        ListTag slots = new ListTag();
        for (int i = 0; i < tables.size(); i++) {
            CompoundTag tag = tables.getCompound(i);
            ResourceLocation table = ResourceLocation.tryParse(tag.getString("id"));
            if (table == null) {
                GTCEu.LOGGER.error("Invalid loot table id \"{}\" in recipe {}", tag.getString("id"), recipe.id);
                continue;
            }
            if (resources == null) {
                GTCEu.LOGGER.error("Cannot resolve loot table {} for recipe {}: no server resources available",
                        table, recipe.id);
                continue;
            }
            List<Content> tableOutputs = new ArrayList<>();
            try {
                new Resolver(resources, recipe.id, tag.getInt("tierChanceBoost"), tableOutputs)
                        .table(table, 1, 0, new HashSet<>());
                if (!tableOutputs.isEmpty()) {
                    CompoundTag slot = new CompoundTag();
                    slot.putString("id", table.toString());
                    slot.putInt("start", existingItems + resolved.size());
                    slot.putInt("size", tableOutputs.size());
                    slots.add(slot);
                }
                resolved.addAll(tableOutputs);
            } catch (Exception e) {
                GTCEu.LOGGER.error("Failed to expand loot table {} for recipe {}", table, recipe.id, e);
            }
        }

        Map<RecipeCapability<?>, List<Content>> outputs = new IdentityHashMap<>();
        recipe.outputs.forEach((cap, list) -> outputs.put(cap, new ArrayList<>(list)));
        if (!resolved.isEmpty()) {
            List<Content> items = outputs.computeIfAbsent(ItemRecipeCapability.CAP, c -> new ArrayList<>());
            items.addAll(resolved);
            data.put(DISPLAY_KEY, slots);
        }

        return new GTRecipe(recipe.recipeType, recipe.id, recipe.inputs, outputs,
                recipe.tickInputs, recipe.tickOutputs,
                recipe.inputChanceLogics, recipe.outputChanceLogics,
                recipe.tickInputChanceLogics, recipe.tickOutputChanceLogics,
                recipe.conditions, recipe.ingredientActions, data, recipe.duration, recipe.recipeCategory);
    }

    public static List<Content> collapseForDisplay(GTRecipe recipe, List<Content> itemOutputs) {
        ListTag slots = recipe.data.getList(DISPLAY_KEY, Tag.TAG_COMPOUND);
        if (slots.isEmpty()) return itemOutputs;

        List<Content> collapsed = new ArrayList<>();
        int next = 0;
        for (int i = 0; i < slots.size(); i++) {
            CompoundTag slot = slots.getCompound(i);
            int start = slot.getInt("start");
            int end = start + slot.getInt("size");
            ResourceLocation table = ResourceLocation.tryParse(slot.getString("id"));
            if (table == null || start < next || end <= start || end > itemOutputs.size()) continue;

            collapsed.addAll(itemOutputs.subList(next, start));
            collapsed.add(new LootTableContent(table, List.copyOf(itemOutputs.subList(start, end))));
            next = end;
        }
        collapsed.addAll(itemOutputs.subList(next, itemOutputs.size()));
        return collapsed;
    }

    public static int displaySize(GTRecipe recipe, int itemOutputCount) {
        ListTag slots = recipe.data.getList(DISPLAY_KEY, Tag.TAG_COMPOUND);
        int size = itemOutputCount;
        for (int i = 0; i < slots.size(); i++) {
            size -= Math.max(0, slots.getCompound(i).getInt("size") - 1);
        }
        return size;
    }

    // cycling slot for xei with all entries
    public static class LootTableContent extends Content {

        public final ResourceLocation table;
        public final List<Content> entries;

        public LootTableContent(ResourceLocation table, List<Content> entries) {
            super(displayIngredient(entries),
                    Math.min(ChanceLogic.getMaxChancedValue(),
                            entries.stream().mapToInt(e -> e.chance * ChanceLogic.getMaxChancedValue() / e.maxChance)
                                    .sum()),
                    ChanceLogic.getMaxChancedValue(), entries.get(0).tierChanceBoost);
            this.table = table;
            this.entries = entries;
        }

        @Nullable
        public Content entryFor(ItemStack stack) {
            if (stack.isEmpty()) return null;
            for (Content entry : entries) {
                if (((Ingredient) entry.content).test(stack)) return entry;
            }
            return null;
        }

        private static Ingredient displayIngredient(List<Content> entries) {
            List<ItemStack> stacks = new ArrayList<>();
            for (Content entry : entries) {
                Ingredient ingredient = (Ingredient) entry.content;
                if (ingredient instanceof IntProviderIngredient provider) ingredient = provider.getInner();
                stacks.addAll(Arrays.asList(ingredient.getItems()));
            }
            return Ingredient.of(stacks.toArray(ItemStack[]::new));
        }
    }

    @Nullable
    private static ResourceManager getResourceManager() {
        ResourceManager resources = reloadingResources;
        if (resources != null) return resources;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getResourceManager();
    }

    private record Leaf(JsonObject entry, double chance) {}

    private record Resolver(ResourceManager resources, ResourceLocation recipeId, int tierChanceBoost,
                            List<Content> out) {

        void table(ResourceLocation table, double probability, int depth, Set<ResourceLocation> visited) {
            if (depth > MAX_DEPTH || !visited.add(table)) {
                GTCEu.LOGGER.error("Loot table {} for recipe {} is recursive or nested too deep", table, recipeId);
                return;
            }
            JsonObject json = read(table);
            if (json == null) return;

            for (JsonElement poolElement : GsonHelper.getAsJsonArray(json, "pools", new JsonArray())) {
                JsonObject pool = poolElement.getAsJsonObject();
                JsonArray entries = GsonHelper.getAsJsonArray(pool, "entries", new JsonArray());
                double rolls = pool.has("rolls") ? expected(pool.get("rolls")) : 1;
                double poolProbability = probability * conditionChance(pool);

                List<Leaf> leaves = new ArrayList<>();
                for (JsonElement entry : entries) {
                    collect(entry.getAsJsonObject(), 1, leaves);
                }
                double totalWeight = 0;
                for (Leaf leaf : leaves) {
                    totalWeight += GsonHelper.getAsInt(leaf.entry(), "weight", 1);
                }
                if (totalWeight <= 0) continue;

                for (Leaf leaf : leaves) {
                    double perRoll = Math.min(1, rolls * GsonHelper.getAsInt(leaf.entry(), "weight", 1) / totalWeight);
                    entry(leaf.entry(), poolProbability * perRoll * leaf.chance(), depth, visited);
                }
            }
            visited.remove(table);
        }

        void collect(JsonObject entry, double chance, List<Leaf> leaves) {
            chance *= conditionChance(entry);
            switch (type(entry)) {
                case "group", "sequence" -> {
                    for (JsonElement child : GsonHelper.getAsJsonArray(entry, "children", new JsonArray())) {
                        collect(child.getAsJsonObject(), chance, leaves);
                    }
                }
                case "alternatives" -> {
                    JsonArray children = GsonHelper.getAsJsonArray(entry, "children", new JsonArray());
                    if (children.isEmpty()) return;
                    JsonObject chosen = children.get(children.size() - 1).getAsJsonObject();
                    for (JsonElement child : children) {
                        if (onlyChanceConditions(child.getAsJsonObject())) {
                            chosen = child.getAsJsonObject();
                            break;
                        }
                    }
                    collect(chosen, chance, leaves);
                }
                default -> leaves.add(new Leaf(entry, chance));
            }
        }

        void entry(JsonObject entry, double probability, int depth, Set<ResourceLocation> visited) {
            if (probability <= 0) return;

            switch (type(entry)) {
                case "item" -> {
                    ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(entry, "name"));
                    Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
                    if (item == Items.AIR) {
                        GTCEu.LOGGER.error("Unknown item {} in loot table for recipe {}", id, recipeId);
                        return;
                    }
                    ItemStack stack = new ItemStack(item);
                    applyNbt(entry, stack);
                    add(null, stack, entry, probability);
                }
                case "tag" -> {
                    var tag = TagKey.create(Registries.ITEM,
                            new ResourceLocation(GsonHelper.getAsString(entry, "name")));
                    add(Ingredient.of(tag), null, entry, probability);
                }
                case "loot_table" -> table(new ResourceLocation(GsonHelper.getAsString(entry, "name")),
                        probability, depth + 1, visited);
                case "empty" -> {}
                default -> GTCEu.LOGGER.warn("Skipping unsupported loot entry type {} for recipe {}",
                        type(entry), recipeId);
            }
        }

        /// @param ingredient the ingredient to output, or null to build one from {@code stack}
        void add(@Nullable Ingredient ingredient, @Nullable ItemStack stack, JsonObject entry, double probability) {
            int min = 1, max = 1;
            for (JsonElement element : GsonHelper.getAsJsonArray(entry, "functions", new JsonArray())) {
                JsonObject function = element.getAsJsonObject();
                if (!type(function, "function").equals("set_count")) continue;
                int base = GsonHelper.getAsBoolean(function, "add", false) ? min : 0;
                JsonElement count = function.get("count");
                if (count.isJsonObject() && isUniform(count.getAsJsonObject())) {
                    JsonObject uniform = count.getAsJsonObject();
                    min = base + (int) Math.floor(expected(uniform.get("min")));
                    max = base + (int) Math.floor(expected(uniform.get("max")));
                } else {
                    min = max = base + (int) Math.max(1, Math.round(expected(count)));
                }
            }
            if (max <= 0) return;

            Ingredient output;
            if (min == max) {
                output = stack != null ? SizedIngredient.create(stack.copyWithCount(min)) :
                        SizedIngredient.create(ingredient, min);
            } else {
                var provider = UniformInt.of(Math.max(0, min), max);
                output = stack != null ? IntProviderIngredient.of(stack, provider) :
                        IntProviderIngredient.of(ingredient, provider);
            }
            int maxChance = ChanceLogic.getMaxChancedValue();
            int chance = probability >= 1 ? maxChance :
                    (int) Math.max(1, Math.min(maxChance - 1, Math.round(probability * maxChance)));
            out.add(new Content(output, chance, maxChance, tierChanceBoost));
        }

        void applyNbt(JsonObject entry, ItemStack stack) {
            for (JsonElement element : GsonHelper.getAsJsonArray(entry, "functions", new JsonArray())) {
                JsonObject function = element.getAsJsonObject();
                if (!type(function, "function").equals("set_nbt")) continue;
                try {
                    stack.getOrCreateTag().merge(TagParser.parseTag(GsonHelper.getAsString(function, "tag")));
                } catch (Exception e) {
                    GTCEu.LOGGER.error("Invalid set_nbt tag in loot table for recipe {}", recipeId, e);
                }
            }
        }

        @Nullable
        JsonObject read(ResourceLocation table) {
            var path = new ResourceLocation(table.getNamespace(), "loot_tables/" + table.getPath() + ".json");
            Optional<Resource> resource = resources.getResource(path);
            if (resource.isEmpty()) {
                GTCEu.LOGGER.error("Loot table {} for recipe {} does not exist", table, recipeId);
                return null;
            }
            try (Reader reader = resource.get().openAsReader()) {
                return GsonHelper.convertToJsonObject(JsonParser.parseReader(reader), table.toString());
            } catch (Exception e) {
                GTCEu.LOGGER.error("Failed to read loot table {} for recipe {}", table, recipeId, e);
                return null;
            }
        }
    }

    private static String type(JsonObject json) {
        return type(json, "type");
    }

    private static String type(JsonObject json, String key) {
        ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(json, key, ""));
        return id == null ? "" : id.getPath();
    }

    private static boolean isChanceCondition(JsonObject condition) {
        String type = type(condition, "condition");
        return type.equals("random_chance") || type.equals("random_chance_with_looting");
    }

    private static boolean onlyChanceConditions(JsonObject json) {
        for (JsonElement condition : GsonHelper.getAsJsonArray(json, "conditions", new JsonArray())) {
            if (!isChanceCondition(condition.getAsJsonObject())) return false;
        }
        return true;
    }

    private static double conditionChance(JsonObject json) {
        double chance = 1;
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "conditions", new JsonArray())) {
            JsonObject condition = element.getAsJsonObject();
            if (isChanceCondition(condition)) chance *= GsonHelper.getAsFloat(condition, "chance", 1);
        }
        return chance;
    }

    private static boolean isUniform(JsonObject provider) {
        String type = type(provider);
        return type.equals("uniform") || (type.isEmpty() && provider.has("min") && provider.has("max"));
    }

    // expected value of a provider
    private static double expected(@Nullable JsonElement provider) {
        if (provider == null) return 1;
        if (provider.isJsonPrimitive()) return provider.getAsDouble();
        if (!provider.isJsonObject()) return 1;
        JsonObject json = provider.getAsJsonObject();
        if (isUniform(json)) return (expected(json.get("min")) + expected(json.get("max"))) / 2;
        return switch (type(json)) {
            case "constant" -> GsonHelper.getAsDouble(json, "value", 1);
            case "binomial" -> expected(json.get("n")) * expected(json.get("p"));
            default -> 1;
        };
    }
}
