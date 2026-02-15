package com.gregtechceu.gtceu.data.recipe.builder;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.codec.GTCodecUtils;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.ItemMaterialData;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.tag.TagUtil;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.*;
import com.gregtechceu.gtceu.api.recipe.ingredient.nbtpredicate.NBTPredicate;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

public record LayeredRecipeInfo(List<Layer> layers, Map<RecipeCapability<?>, Int2IntMap> input,
                                Map<RecipeCapability<?>, Int2IntMap> tickInput) {

    public static final Codec<LayeredRecipeInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Layer.CODEC.listOf().fieldOf("layers").forGetter(LayeredRecipeInfo::layers),
            Codec.unboundedMap(RecipeCapability.DIRECT_CODEC, GTCodecUtils.INT2INT_MAP_CODEC).fieldOf("input")
                    .forGetter(LayeredRecipeInfo::input),
            Codec.unboundedMap(RecipeCapability.DIRECT_CODEC, GTCodecUtils.INT2INT_MAP_CODEC).fieldOf("tickInput")
                    .forGetter(LayeredRecipeInfo::tickInput))
            .apply(instance, LayeredRecipeInfo::new));

    public record Layer(int duration, int timeout) {

        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("duration").forGetter(Layer::duration),
                Codec.INT.fieldOf("timeout").forGetter(Layer::timeout)).apply(instance, Layer::new));
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    @Accessors(chain = true, fluent = true)
    public static class Builder {

        private final GTRecipeBuilder recipeBuilder;

        private final List<Layer> layers = new ArrayList<>();

        public Map<RecipeCapability<?>, List<Content>> input = new IdentityHashMap<>();
        public Map<RecipeCapability<?>, List<Content>> tickInput = new IdentityHashMap<>();

        private List<ItemStack> tempItemStacks = new ArrayList<>();
        private List<MaterialStack> tempItemMaterialStacks = new ArrayList<>();
        private List<MaterialStack> tempFluidStacks = new ArrayList<>();

        @Setter
        public int chance = ChanceLogic.getMaxChancedValue();
        @Setter
        public int maxChance = ChanceLogic.getMaxChancedValue();
        @Setter
        public boolean perTick;
        @Setter
        public int duration = -1;
        @Setter
        public int timeout = -1;

        public Builder(GTRecipeBuilder recipeBuilder) {
            this.recipeBuilder = recipeBuilder;
        }

        public Builder next() {
            if (input.isEmpty() && !tickInput.isEmpty()) return this;

            layers.add(new Layer(
                    new IdentityHashMap<>(input),
                    new IdentityHashMap<>(tickInput),
                    duration,
                    timeout));

            input.clear();
            tickInput.clear();
            perTick = false;
            duration = -1;
            timeout = -1;

            return this;
        }

        public <T> Builder input(RecipeCapability<T> capability, T obj) {
            var t = (perTick ? tickInput : input);
            t.computeIfAbsent(capability, c -> new ArrayList<>()).add(makeContent(capability.of(obj)));
            return this;
        }

        public <T> Builder input(RecipeCapability<T> capability, T... obj) {
            var t = (perTick ? tickInput : input);
            t.computeIfAbsent(capability, c -> new ArrayList<>())
                    .addAll(Arrays.stream(obj).map(capability::of).map(this::makeContent).toList());
            return this;
        }

        public Builder inputEU(long eu) {
            return inputEU(eu, 1);
        }

        public Builder inputEU(long voltage, long amperage) {
            return input(EURecipeCapability.CAP, new EnergyStack(voltage, amperage));
        }

        public Builder EUt(long eu) {
            return EUt(eu, 1);
        }

        public Builder EUt(long voltage, long amperage) {
            if (voltage <= 0) {
                GTCEu.LOGGER.error("EUt can't be explicitly set to 0 or to a negative value, id: {}", recipeBuilder.id);
            }
            if (amperage < 1) {
                GTCEu.LOGGER.error("Amperage must be a positive integer, id: {}", recipeBuilder.id);
            }
            if (voltage < 0) {
                GTCEu.LOGGER.error("EUt can't must be a positive integer, id: {}", recipeBuilder.id);
            }
            var lastPerTick = perTick;
            perTick = true;
            tickInput.remove(EURecipeCapability.CAP);
            inputEU(voltage, amperage);
            perTick = lastPerTick;
            return this;
        }

        public Builder inputItems(Object input) {
            if (input instanceof Item item) {
                return inputItems(item);
            } else if (input instanceof Supplier<?> supplier && supplier.get() instanceof ItemLike item) {
                return inputItems(item.asItem());
            } else if (input instanceof ItemStack stack) {
                return inputItems(stack);
            } else if (input instanceof Ingredient ingredient) {
                return inputItems(ingredient);
            } else if (input instanceof MaterialEntry entry) {
                return inputItems(entry);
            } else if (input instanceof TagKey<?> tag) {
                return inputItems((TagKey<Item>) tag);
            } else if (input instanceof MachineDefinition machine) {
                return inputItems(machine);
            } else {
                GTCEu.LOGGER.error("""
                        Input item is not one of:
                        Item, Supplier<Item>, ItemStack, Ingredient, MaterialEntry, TagKey<Item>, MachineDefinition
                        id: {}""", recipeBuilder.id);
                return this;
            }
        }

        public Builder inputItems(Object input, int count) {
            if (input instanceof Item item) {
                return inputItems(item, count);
            } else if (input instanceof Supplier<?> supplier && supplier.get() instanceof ItemLike item) {
                return inputItems(item.asItem(), count);
            } else if (input instanceof ItemStack stack) {
                return inputItems(stack.copyWithCount(count));
            } else if (input instanceof Ingredient ingredient) {
                return inputItems(ingredient, count);
            } else if (input instanceof MaterialEntry entry) {
                return inputItems(entry, count);
            } else if (input instanceof TagKey<?> tag) {
                return inputItems((TagKey<Item>) tag, count);
            } else if (input instanceof MachineDefinition machine) {
                return inputItems(machine, count);
            } else {
                GTCEu.LOGGER.error("""
                        Input item is not one of:
                        Item, Supplier<Item>, ItemStack, Ingredient, MaterialEntry, TagKey<Item>, MachineDefinition
                        id: {}""", recipeBuilder.id);
                return this;
            }
        }

        public Builder inputItems(Ingredient inputs) {
            if (missingIngredientError(0, ItemRecipeCapability.CAP, inputs::isEmpty)) {
                return this;
            }
            return input(ItemRecipeCapability.CAP, inputs);
        }

        public Builder inputItems(Ingredient... inputs) {
            List<Ingredient> ingredients = new ArrayList<>();
            for (int i = 0; i < inputs.length; i++) {
                var ingredient = inputs[i];
                if (missingIngredientError(i, ItemRecipeCapability.CAP, ingredient::isEmpty)) {
                    return this;
                } else {
                    ingredients.add(ingredient);
                }
            }
            return input(ItemRecipeCapability.CAP, ingredients.toArray(Ingredient[]::new));
        }

        public Builder inputItems(Ingredient inputs, int count) {
            if (missingIngredientError(0, ItemRecipeCapability.CAP, inputs::isEmpty)) {
                return this;
            }
            return input(ItemRecipeCapability.CAP, SizedIngredient.create(inputs, count));
        }

        public Builder inputItems(ItemStack input) {
            if (missingIngredientError(0, ItemRecipeCapability.CAP, input::isEmpty)) {
                return this;
            }
            gatherMaterialInfoFromStack(input);
            return input(ItemRecipeCapability.CAP, SizedIngredient.create(input));
        }

        public Builder inputItems(ItemStack... inputs) {
            List<Ingredient> ingredients = new ArrayList<>();
            for (int i = 0; i < inputs.length; i++) {
                ItemStack itemStack = inputs[i];
                if (missingIngredientError(i, ItemRecipeCapability.CAP, itemStack::isEmpty)) {
                    return this;
                } else {
                    gatherMaterialInfoFromStack(itemStack);
                    ingredients.add(SizedIngredient.create(itemStack));
                }
            }
            return input(ItemRecipeCapability.CAP, ingredients.toArray(Ingredient[]::new));
        }

        public Builder inputItems(TagKey<Item> tag, int amount) {
            return inputItems(SizedIngredient.create(tag, amount));
        }

        public Builder inputItems(TagKey<Item> tag) {
            return inputItems(tag, 1);
        }

        public Builder inputItems(Item input, int amount) {
            return inputItems(new ItemStack(input, amount));
        }

        public Builder inputItems(Item input) {
            return inputItems(input, 1);
        }

        public Builder inputItems(Supplier<? extends Item> input) {
            return inputItems(input.get());
        }

        public Builder inputItems(Supplier<? extends Item> input, int amount) {
            return inputItems(input.get(), amount);
        }

        public Builder inputItems(TagPrefix orePrefix, Material material) {
            return inputItems(orePrefix, material, 1);
        }

        public Builder inputItems(MaterialEntry input) {
            return inputItems(input, 1);
        }

        public Builder inputItems(MaterialEntry input, int count) {
            return inputItems(input.tagPrefix(), input.material(), count);
        }

        public Builder inputItems(TagPrefix tagPrefix, @NotNull Material material, int count) {
            if (tagPrefix.isEmpty() || material.isNull()) {
                GTCEu.LOGGER.error(
                        "Tried to set input item stack that doesn't exist, id: {}, TagPrefix: {}, Material: {}, Count: {}",
                        recipeBuilder.id, tagPrefix, material, count);
                return this;
            } else {
                tempItemMaterialStacks.add(new MaterialStack(material, tagPrefix.getMaterialAmount(material) * count));
                tagPrefix.secondaryMaterials().forEach(mat -> tempItemMaterialStacks.add(mat.multiply(count)));
            }
            TagKey<Item> tag = ChemicalHelper.getTag(tagPrefix, material);
            if (tag != null) {
                return inputItems(tag, count);
            } else {
                var item = ChemicalHelper.get(tagPrefix, material, count);
                if (item.isEmpty()) {
                    GTCEu.LOGGER.error(
                            "Tried to set input item stack that doesn't exist, id: {}, TagPrefix: {}, Material: {}, Count: {}",
                            recipeBuilder.id, tagPrefix, material, count);
                }
                return input(ItemRecipeCapability.CAP, SizedIngredient.create(item));
            }
        }

        public Builder inputItems(MachineDefinition machine) {
            return inputItems(machine, 1);
        }

        public Builder inputItems(MachineDefinition machine, int count) {
            return inputItems(machine.asStack(count));
        }

        public Builder inputItemRanged(IntProviderIngredient provider) {
            return inputItems(provider);
        }

        public Builder inputItemsRanged(ItemStack input, IntProvider intProvider) {
            return inputItemRanged(IntProviderIngredient.of(input, intProvider));
        }

        public Builder inputItemsRanged(Item input, IntProvider intProvider) {
            return inputItemsRanged(new ItemStack(input), intProvider);
        }

        public Builder inputItemsRanged(Supplier<? extends ItemLike> input, IntProvider intProvider) {
            return inputItemsRanged(new ItemStack(input.get().asItem()), intProvider);
        }

        public Builder inputItemsRanged(TagPrefix orePrefix, Material material, IntProvider intProvider) {
            var item = ChemicalHelper.get(orePrefix, material, 1);
            if (item.isEmpty()) {
                GTCEu.LOGGER.error(
                        "Tried to set input ranged item stack that doesn't exist, TagPrefix: {}, Material: {}",
                        orePrefix, material);
            }
            return inputItemsRanged(item, intProvider);
        }

        public Builder inputItemsRanged(MachineDefinition machine, IntProvider intProvider) {
            return inputItemsRanged(machine.asStack(), intProvider);
        }

        public Builder inputItemNbtPredicate(ItemStack stack, NBTPredicate predicate) {
            if (missingIngredientError(0, ItemRecipeCapability.CAP, stack::isEmpty)) {
                return this;
            }
            gatherMaterialInfoFromStack(stack);
            return inputItems(NBTPredicateIngredient.of(stack, predicate));
        }

        public Builder notConsumable(ItemStack itemStack) {
            int lastChance = this.chance;
            this.chance = 0;
            inputItems(itemStack);
            this.chance = lastChance;
            return this;
        }

        public Builder notConsumable(Ingredient ingredient) {
            int lastChance = this.chance;
            this.chance = 0;
            inputItems(ingredient);
            this.chance = lastChance;
            return this;
        }

        public Builder notConsumable(Item item) {
            int lastChance = this.chance;
            this.chance = 0;
            inputItems(item);
            this.chance = lastChance;
            return this;
        }

        public Builder notConsumable(Supplier<? extends Item> item) {
            int lastChance = this.chance;
            this.chance = 0;
            inputItems(item);
            this.chance = lastChance;
            return this;
        }

        public Builder notConsumable(TagPrefix orePrefix, Material material) {
            int lastChance = this.chance;
            this.chance = 0;
            inputItems(orePrefix, material);
            this.chance = lastChance;
            return this;
        }

        public Builder notConsumable(TagPrefix orePrefix, Material material, int count) {
            int lastChance = this.chance;
            this.chance = 0;
            inputItems(orePrefix, material, count);
            this.chance = lastChance;
            return this;
        }

        public Builder notConsumableFluid(FluidStack fluid) {
            return notConsumableFluid(FluidIngredient.of(
                    TagUtil.createFluidTag(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath()),
                    fluid.getAmount()));
        }

        public Builder notConsumableFluid(FluidIngredient ingredient) {
            int lastChance = this.chance;
            this.chance = 0;
            inputFluids(ingredient);
            this.chance = lastChance;
            return this;
        }

        public Builder circuitMeta(int configuration) {
            if (configuration < 0 || configuration > IntCircuitBehaviour.CIRCUIT_MAX) {
                GTCEu.LOGGER.error("Circuit configuration must be in the bounds 0 - 32");
            }
            return notConsumable(IntCircuitIngredient.of(configuration));
        }

        public Builder inputFluids(@NotNull Material material, int amount) {
            return inputFluids(material.getFluid(amount));
        }

        public Builder inputFluids(FluidStack input) {
            if (missingIngredientError(0, FluidRecipeCapability.CAP, input::isEmpty)) {
                return this;
            }
            var matStack = ChemicalHelper.getMaterial(input.getFluid());
            if (!matStack.isNull() && chance != 0 && chance == maxChance) {
                tempFluidStacks.add(new MaterialStack(matStack, input.getAmount() * GTValues.M / GTValues.L));
            }
            return input(FluidRecipeCapability.CAP, FluidIngredient.of(
                    TagUtil.createFluidTag(BuiltInRegistries.FLUID.getKey(input.getFluid()).getPath()),
                    input.getAmount(), input.getTag()));
        }

        public Builder inputFluids(FluidStack... inputs) {
            List<FluidIngredient> ingredients = new ArrayList<>();
            for (int i = 0; i < inputs.length; i++) {
                FluidStack fluid = inputs[i];
                if (missingIngredientError(i, FluidRecipeCapability.CAP, fluid::isEmpty)) {
                    return this;
                } else {
                    var matStack = ChemicalHelper.getMaterial(fluid.getFluid());
                    if (!matStack.isNull()) {
                        if (chance == maxChance && chance != 0) {
                            tempFluidStacks
                                    .add(new MaterialStack(matStack, fluid.getAmount() * GTValues.M / GTValues.L));
                        }
                    }

                    TagKey<Fluid> tag = TagUtil
                            .createFluidTag(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath());
                    ingredients.add(FluidIngredient.of(tag, fluid.getAmount(), fluid.getTag()));
                }
            }
            return input(FluidRecipeCapability.CAP, ingredients.toArray(FluidIngredient[]::new));
        }

        public Builder inputFluidsRanged(IntProviderFluidIngredient provider) {
            return inputFluids(provider);
        }

        protected Builder inputFluidsRanged(FluidIngredient input, IntProvider intProvider) {
            return inputFluidsRanged(IntProviderFluidIngredient.of(input, intProvider));
        }

        public Builder inputFluidsRanged(FluidStack input, IntProvider intProvider) {
            return inputFluidsRanged(FluidIngredient.of(input), intProvider);
        }

        public Builder inputFluids(FluidIngredient... inputs) {
            return input(FluidRecipeCapability.CAP, inputs);
        }

        protected boolean missingIngredientError(int index, RecipeCapability<?> cap, BooleanSupplier empty) {
            if (empty.getAsBoolean()) {
                String io = perTick ? "Tick input" : "Input";
                GTCEu.LOGGER.error("{} {} {} of recipe {} is empty", io, cap.name, index, recipeBuilder.id);
                return true;
            }
            return false;
        }

        private void gatherMaterialInfoFromStack(ItemStack input) {
            var matInfo = ItemMaterialData.getMaterialInfo(input.getItem());
            var unresolvedMatInfo = ItemMaterialData.UNRESOLVED_ITEM_MATERIAL_INFO.get(input);
            if (chance == maxChance && chance != 0) {
                if (unresolvedMatInfo != null) {
                    tempItemStacks.add(input);
                }
                if (matInfo != null) {
                    for (var matStack : matInfo.getMaterials()) {
                        tempItemMaterialStacks.add(matStack.multiply(input.getCount()));
                    }
                } else if (unresolvedMatInfo == null) {
                    tempItemStacks.add(input);
                }

            }
        }

        protected Content makeContent(Object o) {
            return new Content(o, ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0);
        }

        void apply() {
            next();

            var info = new LayeredRecipeInfo(
                    layers.stream().map(layer -> new LayeredRecipeInfo.Layer(layer.duration(), layer.timeout()))
                            .toList(),
                    new IdentityHashMap<>(),
                    new IdentityHashMap<>());

            var layerIndex = 0;
            for (var layer : layers) {
                applyLayer(layerIndex, layer.input(), recipeBuilder.input, info.input());
                applyLayer(layerIndex, layer.tickInput(), recipeBuilder.tickInput, info.tickInput());
                layerIndex++;
            }

            recipeBuilder.data.put("layered_info",
                    LayeredRecipeInfo.CODEC.encodeStart(NbtOps.INSTANCE, info).result().orElseThrow());
        }

        private void warnTooManyIngredients(RecipeCapability<?> capability, boolean isInput, int layer, int totalSize) {
            // TODO: decide if we wan't to use it or no
            var recipeCapabilityMax = isInput ? recipeBuilder.recipeType.maxInputs :
                    recipeBuilder.recipeType.maxOutputs;
            if (!recipeCapabilityMax.containsKey(capability)) return;
            int max = recipeCapabilityMax.getInt(capability);
            if (totalSize > max) {
                String io = isInput ? "inputs" : "outputs";
                GTCEu.LOGGER.warn(
                        "Layer {} of recipe {} is trying to add more {} than its recipe type can support, Max {} {}: {}",
                        layer, recipeBuilder.id, io, capability.name, io, max);
            }
        }

        private void applyLayer(int layerIndex, Map<RecipeCapability<?>, List<Content>> layer,
                                Map<RecipeCapability<?>, List<Content>> cMap,
                                Map<RecipeCapability<?>, Int2IntMap> cLayerMap) {
            for (var entry : layer.entrySet()) {
                var capability = entry.getKey();
                var contents = entry.getValue();

                var indexStart = cMap.containsKey(capability) ? cMap.get(capability).size() : 0;
                cMap.computeIfAbsent(capability, c -> new ArrayList<>()).addAll(contents);
                var indexEnd = indexStart + contents.size();
                var layerMap = cLayerMap.computeIfAbsent(capability, c -> new Int2IntArrayMap());
                IntStream.range(indexStart, indexEnd).forEach((index) -> layerMap.put(index, layerIndex));
            }
        }

        public record Layer(
                            Map<RecipeCapability<?>, List<Content>> input,
                            Map<RecipeCapability<?>, List<Content>> tickInput,
                            int duration,
                            int timeout) {}
    }
}
