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
import com.gregtechceu.gtceu.integration.kjs.recipe.GTRecipeSchema;
import com.gregtechceu.gtceu.integration.kjs.recipe.components.CapabilityMap;
import com.gregtechceu.gtceu.integration.kjs.recipe.components.ExtendedOutputItem;
import com.gregtechceu.gtceu.integration.kjs.recipe.components.GTRecipeComponents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.fluids.FluidStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeExceptionJS;
import dev.latvian.mods.rhino.util.HideFromJS;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ArrayUtils;
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

    @Accessors(chain = true, fluent = true)
    public static class JSBuilder {

        private final GTRecipeSchema.GTRecipeJS recipeJS;

        private final List<Builder.Layer> layers = new ArrayList<>();

        public Map<RecipeCapability<?>, List<Content>> input = new IdentityHashMap<>();
        public Map<RecipeCapability<?>, List<Content>> tickInput = new IdentityHashMap<>();

        public List<MaterialStack> itemMaterialStacks = new ArrayList<>();
        public List<MaterialStack> fluidMaterialStacks = new ArrayList<>();
        public List<ItemStack> tempItemStacks = new ArrayList<>();

        @Setter
        public boolean perTick;
        @Setter
        public int chance = ChanceLogic.getMaxChancedValue();
        @Setter
        public int maxChance = ChanceLogic.getMaxChancedValue();
        @Setter
        public int tierChanceBoost = 0;
        @Setter
        public int duration = -1;
        @Setter
        public int timeout = -1;

        public JSBuilder(GTRecipeSchema.GTRecipeJS recipeJS) {
            this.recipeJS = recipeJS;
        }

        public JSBuilder next() {
            if (input.isEmpty() && !tickInput.isEmpty()) return this;

            layers.add(new Builder.Layer(new IdentityHashMap<>(input), new IdentityHashMap<>(tickInput), duration,
                    timeout));

            input.clear();
            tickInput.clear();
            perTick = false;
            chance = ChanceLogic.getMaxChancedValue();
            maxChance = ChanceLogic.getMaxChancedValue();
            tierChanceBoost = 0;
            duration = -1;
            timeout = -1;

            return this;
        }

        public <T> JSBuilder input(RecipeCapability<T> capability, Object... obj) {
            var t = (perTick ? tickInput : input);
            var newContents = Arrays.stream(obj).map(this::makeContent).toList();
            t.computeIfAbsent(capability, c -> new ArrayList<>()).addAll(newContents);
            return this;
        }

        public JSBuilder inputEU(EnergyStack eu) {
            return input(EURecipeCapability.CAP, eu);
        }

        public JSBuilder inputEU(long voltage, long amperage) {
            return inputEU(new EnergyStack(voltage, amperage));
        }

        public JSBuilder EUt(EnergyStack.WithIO eu) {
            if (eu.isEmpty()) {
                throw new RecipeExceptionJS(String.format("EUt can't be explicitly set to 0, id: %s", recipeJS.id));
            }
            if (eu.amperage() < 1) {
                throw new RecipeExceptionJS(String.format("Amperage must be a positive integer, id: %s", recipeJS.id));
            }
            if (eu.isOutput()) {
                throw new RecipeExceptionJS(String.format("EUt can't be output, id: %s", recipeJS.id));
            }
            var lastPerTick = perTick;
            perTick = true;
            inputEU(eu.stack());
            perTick = lastPerTick;
            return this;
        }

        public JSBuilder EUt(long voltage, long amperage) {
            return EUt(EnergyStack.WithIO.fromVA(voltage, amperage));
        }

        public JSBuilder itemInputs(InputItem... inputs) {
            return inputItems(inputs);
        }

        public JSBuilder itemInput(MaterialEntry input) {
            return inputItems(input);
        }

        public JSBuilder itemInput(MaterialEntry input, int count) {
            return inputItems(input, count);
        }

        public JSBuilder inputItems(InputItem... inputs) {
            validateItems("input", inputs);

            for (var stack : inputs) {
                // test simple item that have pure singular material stack
                var matStack = ChemicalHelper.getMaterialStack(stack.ingredient.getItems()[0].getItem());
                // test item that has multiple material stacks
                var matInfo = ChemicalHelper.getMaterialInfo(stack.ingredient.getItems()[0].getItem());
                if (chance == maxChance && chance != 0) {
                    if (!matStack.isEmpty()) {
                        itemMaterialStacks.add(matStack.multiply(stack.count));
                    }
                    if (matInfo != null) {
                        for (var ms : matInfo.getMaterials()) {
                            itemMaterialStacks.add(ms.multiply(stack.count));
                        }
                    } else {
                        tempItemStacks.add(stack.ingredient.getItems()[0].copyWithCount(stack.count));
                    }
                }
            }
            return input(ItemRecipeCapability.CAP, (Object[]) inputs);
        }

        public JSBuilder inputItems(ItemStack... inputs) {
            validateItems("input", inputs);

            for (ItemStack itemStack : inputs) {
                if (itemStack.isEmpty()) {
                    throw new RecipeExceptionJS(String.format("Input items is empty, id: %s", recipeJS.id));
                }
                gatherMaterialInfoFromStacks(itemStack);
            }
            return input(ItemRecipeCapability.CAP,
                    Arrays.stream(inputs)
                            .map(stack -> InputItem.of(
                                    stack.hasTag() ? StrictNBTIngredient.of(stack) : Ingredient.of(stack),
                                    stack.getCount()))
                            .toArray());
        }

        public JSBuilder inputItems(TagKey<Item> tag, int amount) {
            return inputItems(InputItem.of(Ingredient.of(tag), amount));
        }

        public JSBuilder inputItems(Item input, int amount) {
            return inputItems(new ItemStack(input, amount));
        }

        public JSBuilder inputItems(Item input) {
            return inputItems(InputItem.of(Ingredient.of(input), 1));
        }

        public JSBuilder inputItems(Supplier<? extends Item> input) {
            return inputItems(input.get());
        }

        public JSBuilder inputItems(Supplier<? extends Item> input, int amount) {
            return inputItems(new ItemStack(input.get(), amount));
        }

        public JSBuilder inputItems(TagPrefix orePrefix, Material material) {
            return inputItems(orePrefix, material, 1);
        }

        public JSBuilder inputItems(MaterialEntry input) {
            return inputItems(input.tagPrefix(), input.material(), 1);
        }

        public JSBuilder inputItems(MaterialEntry input, int count) {
            return inputItems(input.tagPrefix(), input.material(), count);
        }

        public JSBuilder inputItems(TagPrefix orePrefix, Material material, int count) {
            itemMaterialStacks.add(new MaterialStack(material, orePrefix.getMaterialAmount(material) * count));
            return inputItems(ChemicalHelper.getTag(orePrefix, material), count);
        }

        public JSBuilder inputItems(MachineDefinition machine) {
            return inputItems(machine, 1);
        }

        public JSBuilder inputItems(MachineDefinition machine, int count) {
            return inputItems(machine.asStack(count));
        }

        public JSBuilder itemInputsRanged(ExtendedOutputItem ingredient, int min, int max) {
            return inputItemsRanged(ingredient.ingredient.getInner(), min, max);
        }

        public JSBuilder inputItemsRanged(Ingredient ingredient, int min, int max) {
            validateItems("ranged input", ingredient);
            return input(ItemRecipeCapability.CAP, new ExtendedOutputItem(ingredient, 1, UniformInt.of(min, max)));
        }

        public JSBuilder inputItemsRanged(ItemStack stack, int min, int max) {
            validateItems("ranged input", stack);
            return input(ItemRecipeCapability.CAP, new ExtendedOutputItem(stack, UniformInt.of(min, max)));
        }

        public JSBuilder itemInputsRanged(TagPrefix orePrefix, Material material, int min, int max) {
            return inputItemsRanged(ChemicalHelper.get(orePrefix, material), min, max);
        }

        public JSBuilder inputItemNbtPredicate(ItemStack itemStack, NBTPredicate predicate) {
            if (itemStack.isEmpty()) {
                throw new RecipeExceptionJS(String.format("Input items is empty, id: %s", recipeJS.id));
            }
            gatherMaterialInfoFromStacks(itemStack);

            return itemInputs(InputItem.of(new NBTPredicateIngredient(itemStack, predicate), itemStack.getCount()));
        }

        public JSBuilder notConsumable(InputItem itemStack) {
            validateItems("not consumable", itemStack);

            int lastChance = this.chance;
            this.chance = 0;
            inputItems(itemStack);
            this.chance = lastChance;
            return this;
        }

        public JSBuilder notConsumable(TagPrefix orePrefix, Material material) {
            validateItems("not consumable", orePrefix);

            int lastChance = this.chance;
            this.chance = 0;
            inputItems(orePrefix, material);
            this.chance = lastChance;
            return this;
        }

        public JSBuilder notConsumableFluid(GTRecipeComponents.FluidIngredientJS fluid) {
            validateFluids("not consumable", fluid);

            int lastChance = this.chance;
            this.chance = 0;
            inputFluids(fluid);
            this.chance = lastChance;
            return this;
        }

        public JSBuilder chancedInput(InputItem stack, int chance, int tierChanceBoost) {
            validateItems("chanced input", stack);

            if (0 >= chance || chance > ChanceLogic.getMaxChancedValue()) {
                throw new RecipeExceptionJS(
                        String.format("Chance cannot be less or equal to 0 or more than %s, Actual: %s, id: %s",
                                ChanceLogic.getMaxChancedValue(), chance, recipeJS.id));
            }
            int lastChance = this.chance;
            int lastTierChanceBoost = this.tierChanceBoost;
            this.chance = chance;
            this.tierChanceBoost = tierChanceBoost;
            inputItems(stack);
            this.chance = lastChance;
            this.tierChanceBoost = lastTierChanceBoost;
            return this;
        }

        public JSBuilder chancedFluidInput(GTRecipeComponents.FluidIngredientJS stack, int chance,
                                           int tierChanceBoost) {
            validateFluids("chanced input", stack);

            if (0 >= chance || chance > ChanceLogic.getMaxChancedValue()) {
                throw new RecipeExceptionJS(
                        String.format("Chance cannot be less or equal to 0 or more than %s, Actual: %s, id: %s",
                                ChanceLogic.getMaxChancedValue(), chance, recipeJS.id));
            }
            int lastChance = this.chance;
            int lastTierChanceBoost = this.tierChanceBoost;
            this.chance = chance;
            this.tierChanceBoost = tierChanceBoost;
            inputFluids(stack);
            this.chance = lastChance;
            this.tierChanceBoost = lastTierChanceBoost;
            return this;
        }

        public JSBuilder fluidInputs(GTRecipeComponents.FluidIngredientJS... inputs) {
            return inputFluids(inputs);
        }

        public JSBuilder inputFluids(GTRecipeComponents.FluidIngredientJS... inputs) {
            validateFluids("input", inputs);

            for (var fluidIng : inputs) {
                for (var stack : fluidIng.ingredient().getStacks()) {
                    var mat = ChemicalHelper.getMaterial(stack.getFluid());
                    if (!mat.isNull()) {
                        fluidMaterialStacks.add(new MaterialStack(mat,
                                ((long) stack.getAmount() * GTValues.M) / GTValues.L));
                    }
                }
            }
            return input(FluidRecipeCapability.CAP, (Object[]) inputs);
        }

        public JSBuilder inputFluidsRanged(FluidStackJS input, int min, int max) {
            return inputFluidsRanged(input, UniformInt.of(min, max));
        }

        public JSBuilder inputFluidsRanged(FluidStackJS input, IntProvider range) {
            validateFluids("ranged input", input);
            FluidStack stack = new FluidStack(input.getFluid(), (int) input.getAmount(), input.getNbt());
            return input(FluidRecipeCapability.CAP,
                    IntProviderFluidIngredient.of(FluidIngredient.of(stack), range));
        }

        private Content makeContent(Object o) {
            return new Content(o, chance, maxChance, tierChanceBoost);
        }

        @HideFromJS
        public void apply() {
            next();

            var info = new LayeredRecipeInfo(
                    layers.stream().map(layer -> new LayeredRecipeInfo.Layer(layer.duration(), layer.timeout()))
                            .toList(),
                    new IdentityHashMap<>(),
                    new IdentityHashMap<>());

            var layerIndex = 0;
            for (var layer : layers) {
                var allInputs = recipeJS.getValue(GTRecipeSchema.ALL_INPUTS);
                if (allInputs == null)
                    recipeJS.setValue(GTRecipeSchema.ALL_INPUTS, allInputs = new CapabilityMap());

                var allTickInputs = recipeJS.getValue(GTRecipeSchema.ALL_TICK_INPUTS);
                if (allTickInputs == null)
                    recipeJS.setValue(GTRecipeSchema.ALL_TICK_INPUTS, allTickInputs = new CapabilityMap());

                applyLayer(layerIndex, layer.input(), allInputs, info.input());
                applyLayer(layerIndex, layer.tickInput(), allTickInputs, info.tickInput());
                layerIndex++;
            }

            var layeredData = LayeredRecipeInfo.CODEC.encodeStart(NbtOps.INSTANCE, info).result().orElseThrow();
            recipeJS.addData("layered_info", layeredData);
        }

        private void applyLayer(int layerIndex, Map<RecipeCapability<?>, List<Content>> layer,
                                CapabilityMap cMap,
                                Map<RecipeCapability<?>, Int2IntMap> cLayerMap) {
            for (var entry : layer.entrySet()) {
                var capability = entry.getKey();
                var contents = entry.getValue();

                var cMapCap = cMap.get(capability);
                var indexStart = cMapCap != null ? cMapCap.length : 0;
                cMap.put(capability, ArrayUtils.addAll(cMapCap, contents.toArray(Content[]::new)));
                var indexEnd = indexStart + contents.size();
                var layerMap = cLayerMap.computeIfAbsent(capability, c -> new Int2IntArrayMap());
                IntStream.range(indexStart, indexEnd).forEach((index) -> layerMap.put(index, layerIndex));
            }
        }

        private void validateItems(@NotNull String type, InputItem... items) {
            for (var stack : items) {
                if (stack == null || stack.isEmpty()) {
                    throw new RecipeExceptionJS(
                            String.format("Invalid or empty %s item (recipe ID: %s)", type, recipeJS.id));
                }
            }
        }

        private void validateItems(@NotNull String type, ItemStack... stacks) {
            for (var stack : stacks) {
                if (stack == null || stack.isEmpty()) {
                    throw new RecipeExceptionJS(
                            String.format("Invalid or empty %s item (recipe ID: %s)", type, recipeJS.id));
                }
            }
        }

        private void validateItems(@NotNull String type, Ingredient... ingredients) {
            for (var ingredient : ingredients) {
                if (ingredient == null || ingredient.isEmpty()) {
                    throw new RecipeExceptionJS(
                            String.format("Invalid or empty %s item (recipe ID: %s)", type, recipeJS.id));
                }
            }
        }

        private void validateItems(@NotNull String type, OutputItem... items) {
            for (var item : items) {
                if (item == null || item.item == null || item.item.isEmpty()) {
                    throw new RecipeExceptionJS(
                            String.format("Invalid or empty %s item (recipe ID: %s)", type, recipeJS.id));
                }
            }
        }

        private void validateItems(@NotNull String type, TagPrefix... items) {
            for (var item : items) {
                if (item == null || item.isEmpty()) {
                    throw new RecipeExceptionJS(
                            String.format("Invalid or empty %s item (recipe ID: %s)", type, recipeJS.id));
                }
            }
        }

        private void validateFluids(@NotNull String type, GTRecipeComponents.FluidIngredientJS... fluids) {
            for (var fluid : fluids) {
                if (fluid == null || fluid.ingredient() == null || fluid.ingredient().getStacks() == null) {
                    throw new RecipeExceptionJS(
                            String.format("Invalid or empty %s fluid (recipe ID: %s)", type, recipeJS.id));
                }

                for (var stack : fluid.ingredient().getStacks()) {
                    if (stack == null || stack.isEmpty()) {
                        throw new RecipeExceptionJS(
                                String.format("Invalid or empty %s fluid (recipe ID: %s)", type, recipeJS.id));
                    }
                }
            }
        }

        private void validateFluids(@NotNull String type, FluidStackJS... stacks) {
            for (var stack : stacks) {
                if (stack == null || stack.getFluidStack() == null || stack.getFluidStack().isEmpty()) {
                    throw new RecipeExceptionJS(
                            String.format("Invalid or empty %s fluid (recipe ID: %s)", type, recipeJS.id));
                }
            }
        }

        private void gatherMaterialInfoFromStacks(ItemStack itemStack) {
            // test simple item that have pure singular material stack
            var matStack = ChemicalHelper.getMaterialStack(itemStack);
            // test item that has multiple material stacks
            var matInfo = ChemicalHelper.getMaterialInfo(itemStack);
            if (chance == maxChance && chance != 0) {
                if (!matStack.isEmpty()) {
                    itemMaterialStacks.add(matStack.multiply(itemStack.getCount()));
                }
                if (matInfo != null) {
                    for (var ms : matInfo.getMaterials()) {
                        itemMaterialStacks.add(ms.multiply(itemStack.getCount()));
                    }
                } else {
                    tempItemStacks.add(itemStack);
                }
            }
        }
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
            return new Content(o, chance, maxChance, 0);
        }

        public void apply() {
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
