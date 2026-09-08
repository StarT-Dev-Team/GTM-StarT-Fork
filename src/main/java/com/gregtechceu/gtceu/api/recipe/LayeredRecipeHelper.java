package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.data.recipe.builder.LayeredRecipeInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LayeredRecipeHelper {

    // root recipe keys
    public static final String KEY_LAYERED_STEPS = "layered_steps"; // List<GTRecipe>
    public static final String KEY_LAYERED_XEI = "layered_xei"; // GTRecipe
    public static final String KEY_LAYERED_INFO = "layered_info"; // LayeredRecipeInfo

    // single layer keys
    public static final String KEY_IS_LAYER = "is_layer"; // boolean
    public static final String KEY_LAYER_STEP = "layer_step"; // int
    public static final String KEY_LAYER_TIMEOUT = "layer_timeout"; // int

    public static boolean hasLayeredSteps(GTRecipe recipe) {
        return recipe.data.contains(KEY_LAYERED_STEPS);
    }

    public static int getLayerTimeout(GTRecipe recipe) {
        return recipe.data.getInt(KEY_LAYER_TIMEOUT);
    }

    public static @Nullable List<GTRecipe> getLayeredSteps(GTRecipe recipe) {
        return getLayeredSteps(recipe.data);
    }

    public static @Nullable List<GTRecipe> getLayeredSteps(CompoundTag recipeData) {
        var serialized = recipeData.get(KEY_LAYERED_STEPS);
        if (serialized == null) return null;
        return RECIPE_WITH_ID_CODEC.listOf().parse(NbtOps.INSTANCE, serialized).result().orElse(null);
    }

    public static void setLayeredSteps(GTRecipe recipe, List<GTRecipe> layers) {
        var serialized = RECIPE_WITH_ID_CODEC.listOf().encodeStart(NbtOps.INSTANCE, layers).result().orElseThrow();
        recipe.data.put(KEY_LAYERED_STEPS, serialized);
    }

    public static @Nullable GTRecipe getXeiLayeredRecipe(GTRecipe recipe) {
        return getXeiLayeredRecipe(recipe.data);
    }

    public static @Nullable GTRecipe getXeiLayeredRecipe(CompoundTag recipeData) {
        if (!recipeData.contains(KEY_LAYERED_XEI)) return null;
        var serialized = recipeData.get(KEY_LAYERED_XEI);
        return RECIPE_WITH_ID_CODEC.parse(NbtOps.INSTANCE, serialized).result().orElse(null);
    }

    public static @Nullable List<GTRecipe> calculateRecipeSteps(GTRecipe recipe) {
        var layeredInfo = parseRecipeInfo(recipe);
        if (layeredInfo == null) return null;
        var base = createBaseRecipe(recipe, layeredInfo);
        return IntStream.range(0, layeredInfo.layers().size())
                .mapToObj((index) -> createStepRecipe(recipe, base, layeredInfo, index))
                .toList();
    }

    public static void applyLayeredRecipeModifications(GTRecipeBuilder builder) {
        if (!builder.data.contains(KEY_LAYERED_INFO)) return;

        var layers = calculateRecipeSteps(builder.buildRawRecipe());
        assert layers != null;

        var serializedSteps = RECIPE_WITH_ID_CODEC.listOf().encodeStart(NbtOps.INSTANCE, layers).result().orElseThrow();

        var xei = GTRecipeBuilder.of(builder.id.withPrefix("/"), builder.recipeType)
                .addData(KEY_LAYERED_STEPS, serializedSteps)
                .EUt(layers.get(0).getInputEUt().getTotalEU())
                .inputItems(layers.stream().flatMap(l -> getLayerData(ItemRecipeCapability.CAP, l.inputs))
                        .toArray(Ingredient[]::new))
                .outputItems(layers.stream().flatMap(l -> getLayerData(ItemRecipeCapability.CAP, l.outputs))
                        .toArray(Ingredient[]::new))
                .inputFluids(layers.stream().flatMap(l -> getLayerData(FluidRecipeCapability.CAP, l.inputs))
                        .toArray(FluidIngredient[]::new))
                .outputFluids(layers.stream().flatMap(l -> getLayerData(FluidRecipeCapability.CAP, l.outputs))
                        .toArray(FluidIngredient[]::new))
                .perTick(true)
                .inputItems(layers.stream().flatMap(l -> getLayerData(ItemRecipeCapability.CAP, l.tickInputs))
                        .toArray(Ingredient[]::new))
                .outputItems(layers.stream().flatMap(l -> getLayerData(ItemRecipeCapability.CAP, l.tickOutputs))
                        .toArray(Ingredient[]::new))
                .inputFluids(layers.stream().flatMap(l -> getLayerData(FluidRecipeCapability.CAP, l.tickInputs))
                        .toArray(FluidIngredient[]::new))
                .outputFluids(
                        layers.stream().flatMap(l -> getLayerData(FluidRecipeCapability.CAP, l.tickOutputs))
                                .toArray(FluidIngredient[]::new))
                .addConditions(layers.get(0).conditions)
                .duration(layers.stream().reduce(0, (sum, layer) -> sum + layer.duration, Integer::sum))
                .buildRawRecipe();

        var serializedXei = RECIPE_WITH_ID_CODEC.encodeStart(NbtOps.INSTANCE, xei).result().orElseThrow();

        resetRecipeBuilderContents(builder, layers.get(0), layers.get(layers.size() - 1));
        builder.category(builder.recipeType.getSyntheticCategory());
        builder.data.remove("is_layer");
        builder.data.put(KEY_LAYERED_STEPS, serializedSteps);
        builder.data.put(KEY_LAYERED_XEI, serializedXei);
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

    private static void resetRecipeBuilderContents(GTRecipeBuilder builder, GTRecipe firstStep, GTRecipe lastStep) {
        builder.input.clear();
        firstStep.inputs.forEach((k, v) -> builder.input.put(k, new ArrayList<>(v)));
        builder.output.clear();
        lastStep.outputs.forEach((k, v) -> builder.output.put(k, new ArrayList<>(v)));
        builder.tickInput.clear();
        firstStep.tickInputs.forEach((k, v) -> builder.tickInput.put(k, new ArrayList<>(v)));
        builder.tickOutput.clear();
        firstStep.tickOutputs.forEach((k, v) -> builder.tickOutput.put(k, new ArrayList<>(v)));
        builder.inputChanceLogic.clear();
        builder.inputChanceLogic.putAll(firstStep.inputChanceLogics);
        builder.outputChanceLogic.clear();
        builder.outputChanceLogic.putAll(lastStep.outputChanceLogics);
        builder.tickInputChanceLogic.clear();
        builder.tickInputChanceLogic.putAll(firstStep.tickInputChanceLogics);
        builder.tickOutputChanceLogic.clear();
        builder.tickOutputChanceLogic.putAll(firstStep.tickOutputChanceLogics);
        builder.conditions.clear();
        builder.conditions.addAll(firstStep.conditions);
        builder.data = firstStep.data.copy();
        builder.duration = firstStep.duration;
        builder.recipeCategory = firstStep.recipeCategory;
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

    private static GTRecipe createStepRecipe(GTRecipe fullRecipe, GTRecipe baseRecipe, LayeredRecipeInfo layeredInfo,
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

        copy.data = copy.data.copy();
        copy.data.putInt(KEY_LAYER_STEP, recipeStep);
        copy.data.putInt(KEY_LAYER_TIMEOUT, layer.timeout());
        return copy;
    }

    private static GTRecipe createBaseRecipe(GTRecipe fullRecipe, LayeredRecipeInfo layeredInfo) {
        var copiedData = fullRecipe.data.copy();
        copiedData.remove(KEY_LAYERED_INFO);
        copiedData.putBoolean(KEY_IS_LAYER, true);

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
        var layeredInfoTag = data.get(KEY_LAYERED_INFO);
        return LayeredRecipeInfo.CODEC.parse(NbtOps.INSTANCE, layeredInfoTag).result().orElse(null);
    }

    public static @Nullable LayeredRecipeInfo parseRecipeInfo(GTRecipe recipe) {
        return parseRecipeInfo(recipe.data);
    }

    public static final Codec<GTRecipe> RECIPE_WITH_ID_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(recipe -> recipe.id),
            Codec.INT.fieldOf("parallels").forGetter(recipe -> recipe.parallels),
            GTRecipeSerializer.PARALLELS_BY_TYPE_CODEC.fieldOf("parallelsByType")
                    .forGetter(recipe -> recipe.parallelsByType),
            Codec.INT.fieldOf("ocLevel").forGetter(recipe -> recipe.ocLevel),
            Codec.INT.fieldOf("baseOcLevel").forGetter(recipe -> recipe.baseOcLevel),
            ((MapCodec.MapCodecCodec<GTRecipe>) GTRecipeSerializer.CODEC).codec().forGetter(recipe -> recipe))
            .apply(instance, (id, parallels, parallelsByType, ocLevel, baseOcLevel, recipe) -> {
                recipe.parallels = parallels;
                recipe.parallelsByType = parallelsByType;
                recipe.ocLevel = ocLevel;
                recipe.baseOcLevel = baseOcLevel;
                recipe.setId(id);
                return recipe;
            }));
}
