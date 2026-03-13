package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.data.recipe.builder.LayeredRecipeInfo;

import com.lowdragmc.lowdraglib.syncdata.payload.ObjectTypedPayload;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LayeredRecipeHelper {

    public static boolean hasLayeredSteps(GTRecipe recipe) {
        return recipe.data.contains("layered_steps");
    }

    public static @Nullable List<Layer> getLayeredSteps(GTRecipe recipe) {
        return getLayeredSteps(recipe.data);
    }

    public static @Nullable List<Layer> getLayeredSteps(CompoundTag recipeData) {
        var serialized = recipeData.get("layered_steps");
        if (serialized == null) return null;
        return Layer.CODEC.listOf().parse(NbtOps.INSTANCE, serialized).result().orElse(null);
    }

    public static void setLayeredSteps(GTRecipe recipe, List<Layer> layers) {
        var serialized = Layer.CODEC.listOf().encodeStart(NbtOps.INSTANCE, layers).result().orElseThrow();
        recipe.data.put("layered_steps", serialized);
    }

    public static @Nullable GTRecipe getXeiLayeredRecipe(GTRecipe recipe) {
        return getXeiLayeredRecipe(recipe.data);
    }

    public static @Nullable GTRecipe getXeiLayeredRecipe(CompoundTag recipeData) {
        if (!recipeData.contains("layered_xei")) return null;
        var serialized = recipeData.get("layered_xei");
        return Layer.RECIPE_WITH_ID_CODEC.parse(NbtOps.INSTANCE, serialized).result().orElse(null);
    }

    public static @Nullable List<Layer> calculateRecipeSteps(GTRecipe recipe) {
        var layeredInfo = parseRecipeInfo(recipe);
        if (layeredInfo == null) return null;
        var base = createBaseRecipe(recipe, layeredInfo);
        return IntStream.range(0, layeredInfo.layers().size())
                .mapToObj((index) -> createStepRecipe(recipe, base, layeredInfo, index))
                .toList();
    }

    public static void applyLayeredRecipeModifications(GTRecipeBuilder builder) {
        if (!builder.data.contains("layered_info")) return;

        var layers = calculateRecipeSteps(builder.buildRawRecipe());
        assert layers != null;

        var serializedSteps = Layer.CODEC.listOf().encodeStart(NbtOps.INSTANCE, layers).result().orElseThrow();

        var xei = GTRecipeBuilder.of(builder.id.withPrefix("/"), builder.recipeType)
                .addData("layered_steps", serializedSteps)
                .EUt(layers.get(0).recipe.getInputEUt().getTotalEU())
                .inputItems(layers.stream().flatMap(l -> getLayerData(ItemRecipeCapability.CAP, l.recipe.inputs))
                        .toArray(Ingredient[]::new))
                .outputItems(layers.stream().flatMap(l -> getLayerData(ItemRecipeCapability.CAP, l.recipe.outputs))
                        .toArray(Ingredient[]::new))
                .inputFluids(layers.stream().flatMap(l -> getLayerData(FluidRecipeCapability.CAP, l.recipe.inputs))
                        .toArray(FluidIngredient[]::new))
                .outputFluids(layers.stream().flatMap(l -> getLayerData(FluidRecipeCapability.CAP, l.recipe.outputs))
                        .toArray(FluidIngredient[]::new))
                .perTick(true)
                .inputItems(layers.stream().flatMap(l -> getLayerData(ItemRecipeCapability.CAP, l.recipe.tickInputs))
                        .toArray(Ingredient[]::new))
                .outputItems(layers.stream().flatMap(l -> getLayerData(ItemRecipeCapability.CAP, l.recipe.tickOutputs))
                        .toArray(Ingredient[]::new))
                .inputFluids(layers.stream().flatMap(l -> getLayerData(FluidRecipeCapability.CAP, l.recipe.tickInputs))
                        .toArray(FluidIngredient[]::new))
                .outputFluids(
                        layers.stream().flatMap(l -> getLayerData(FluidRecipeCapability.CAP, l.recipe.tickOutputs))
                                .toArray(FluidIngredient[]::new))
                .duration(layers.stream().reduce(0, (sum, layer) -> sum + layer.recipe().duration, Integer::sum))
                .buildRawRecipe();
        var serializedXei = Layer.RECIPE_WITH_ID_CODEC.encodeStart(NbtOps.INSTANCE, xei).result().orElseThrow();

        resetRecipeBuilderContents(builder, layers.get(0).recipe);
        builder.category(builder.recipeType.getSyntheticCategory());
        builder.data.remove("is_layer");
        builder.data.put("layered_steps", serializedSteps);
        builder.data.put("layered_xei", serializedXei);
    }

    private static <T> Stream<T> getLayerData(RecipeCapability<T> cap, Map<RecipeCapability<?>, List<Content>> map) {
        return map.getOrDefault(cap, Collections.emptyList()).stream().map(content -> cap.of(content.getContent()));
    }

    public static void buildRepresentativeRecipes(GTRecipeType recipeType) {
        assert recipeType.isLayered();

        var category = recipeType.getCategory();
        for (var recipe : recipeType.getRecipesInCategory(recipeType.getSyntheticCategory())) {
            var original = LayeredRecipeHelper.getXeiLayeredRecipe(recipe);
            if (original == null) continue;
            category.addRecipe(original);
        }
    }

    private static void resetRecipeBuilderContents(GTRecipeBuilder builder, GTRecipe toCopy) {
        toCopy.inputs.forEach((k, v) -> builder.input.put(k, new ArrayList<>(v)));
        builder.output.clear();
        toCopy.outputs.forEach((k, v) -> builder.output.put(k, new ArrayList<>(v)));
        builder.tickInput.clear();
        toCopy.tickInputs.forEach((k, v) -> builder.tickInput.put(k, new ArrayList<>(v)));
        builder.tickOutput.clear();
        toCopy.tickOutputs.forEach((k, v) -> builder.tickOutput.put(k, new ArrayList<>(v)));
        builder.inputChanceLogic.clear();
        builder.inputChanceLogic.putAll(toCopy.inputChanceLogics);
        builder.outputChanceLogic.clear();
        builder.outputChanceLogic.putAll(toCopy.outputChanceLogics);
        builder.tickInputChanceLogic.clear();
        builder.tickInputChanceLogic.putAll(toCopy.tickInputChanceLogics);
        builder.tickOutputChanceLogic.clear();
        builder.tickOutputChanceLogic.putAll(toCopy.tickOutputChanceLogics);
        builder.conditions.clear();
        builder.conditions.addAll(toCopy.conditions);
        builder.data = toCopy.data.copy();
        builder.duration = toCopy.duration;
        builder.recipeCategory = toCopy.recipeCategory;
        builder.perTick = false;
        builder.chance = ChanceLogic.getMaxChancedValue();
        builder.maxChance = ChanceLogic.getMaxChancedValue();
        builder.tierChanceBoost = 0;
        builder.addMaterialInfo(false, false);
        builder.onSave = null;
        builder.researchRecipeEntries().clear();
        builder.setTempItemStacks(new ArrayList<>());
        builder.setTempItemMaterialStacks(new ArrayList<>());
        builder.setTempFluidMaterialStacks(new ArrayList<>());
    }

    private static Map<RecipeCapability<?>, List<Content>> copyLayeredInputs(Map<RecipeCapability<?>, List<Content>> inputMap,
                                                                             Map<RecipeCapability<?>, Int2IntMap> layeredInputMap,
                                                                             int recipeStep) {
        var dest = new IdentityHashMap<RecipeCapability<?>, List<Content>>();
        for (var entry : inputMap.entrySet()) {
            var capability = entry.getKey();
            var contents = entry.getValue();
            var layerInput = layeredInputMap.get(capability);
            if (layerInput == null) {
                if (recipeStep == -1) {
                    dest.put(capability, contents);
                }
                continue;
            }
            dest.put(capability, IntStream.range(0, contents.size()).boxed()
                    .flatMap(index -> layerInput.getOrDefault((int) index, -1) == recipeStep ?
                            Stream.of(contents.get(index)) : Stream.of())
                    .collect(Collectors.toList()));
        }
        return dest;
    }

    private static Layer createStepRecipe(GTRecipe fullRecipe, GTRecipe baseRecipe, LayeredRecipeInfo layeredInfo,
                                          int recipeStep) {
        var copy = baseRecipe.copy();
        copy.setId(copy.id.withSuffix("/step" + (recipeStep + 1)));
        for (var entry : copyLayeredInputs(fullRecipe.inputs, layeredInfo.input(), recipeStep).entrySet()) {
            copy.inputs.merge(entry.getKey(), entry.getValue(),
                    (contents1, contents2) -> Streams.concat(contents1.stream(), contents2.stream()).toList());
        }
        for (var entry : copyLayeredInputs(fullRecipe.tickInputs, layeredInfo.tickInput(), recipeStep).entrySet()) {
            copy.tickInputs.merge(entry.getKey(), entry.getValue(),
                    (contents1, contents2) -> Streams.concat(contents1.stream(), contents2.stream()).toList());
        }
        if (recipeStep == layeredInfo.layers().size() - 1) {
            copy.outputs.putAll(fullRecipe.outputs);
        }
        var layer = layeredInfo.layers().get(recipeStep);
        if (layer.duration() > 0) copy.duration = layer.duration();

        return new Layer(copy, layer.timeout());
    }

    private static GTRecipe createBaseRecipe(GTRecipe fullRecipe, LayeredRecipeInfo layeredInfo) {
        var copiedData = fullRecipe.data.copy();
        copiedData.remove("layered_info");
        copiedData.putBoolean("is_layer", true);

        return new GTRecipe(
                fullRecipe.recipeType, fullRecipe.id,
                copyLayeredInputs(fullRecipe.inputs, layeredInfo.input(), -1),
                new IdentityHashMap<>(),
                copyLayeredInputs(fullRecipe.tickInputs, layeredInfo.tickInput(), -1),
                new IdentityHashMap<>(),
                fullRecipe.inputChanceLogics,
                new IdentityHashMap<>(),
                fullRecipe.tickInputChanceLogics,
                new IdentityHashMap<>(),
                fullRecipe.conditions,
                fullRecipe.ingredientActions,
                copiedData,
                fullRecipe.duration,
                fullRecipe.recipeCategory);
    }

    public static @Nullable LayeredRecipeInfo parseRecipeInfo(CompoundTag data) {
        var layeredInfoTag = data.get("layered_info");
        return LayeredRecipeInfo.CODEC.parse(NbtOps.INSTANCE, layeredInfoTag).result().orElse(null);
    }

    public static @Nullable LayeredRecipeInfo parseRecipeInfo(GTRecipe recipe) {
        return parseRecipeInfo(recipe.data);
    }

    public static class LayerPayload extends ObjectTypedPayload<Layer> {

        @Override
        public @Nullable Tag serializeNBT() {
            return Layer.CODEC.encodeStart(NbtOps.INSTANCE, payload).result().orElseThrow();
        }

        @Override
        public void deserializeNBT(Tag tag) {
            payload = Layer.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow();
        }
    }

    @Accessors(chain = true, fluent = true)
    public static class Layer {

        public static final Codec<GTRecipe> RECIPE_WITH_ID_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(recipe -> recipe.id),
                ((MapCodec.MapCodecCodec<GTRecipe>) GTRecipeSerializer.CODEC).codec().forGetter(recipe -> recipe))
                .apply(instance, (id, recipe) -> {
                    recipe.setId(id);
                    return recipe;
                }));

        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RECIPE_WITH_ID_CODEC.fieldOf("recipe").forGetter(Layer::recipe),
                Codec.INT.fieldOf("timeout").forGetter(Layer::timeout)).apply(instance, Layer::new));

        @Getter
        private final GTRecipe recipe;

        @Getter
        private final int timeout;

        public Layer(GTRecipe recipe, int timeout) {
            this.recipe = recipe;
            this.timeout = timeout;
        }
    }
}
