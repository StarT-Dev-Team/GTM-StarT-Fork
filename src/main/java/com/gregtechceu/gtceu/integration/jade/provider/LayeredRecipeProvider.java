package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.machine.trait.LayeredRecipeLogic;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

public class LayeredRecipeProvider extends CapabilityBlockProvider<LayeredRecipeLogic> {

    public LayeredRecipeProvider() {
        super(GTCEu.id("layered_recipe_info"));
    }

    @Override
    public int getDefaultPriority() {
        return 10; // run after RecipeOutputProvider
    }

    @Override
    protected @Nullable LayeredRecipeLogic getCapability(Level level, BlockPos pos, @Nullable Direction side) {
        return GTCapabilityHelper.getRecipeLogic(level, pos, side) instanceof LayeredRecipeLogic logic ? logic : null;
    }

    @Override
    protected void write(CompoundTag data, LayeredRecipeLogic recipeLogic) {
        var layeredSteps = recipeLogic.getLayeredRecipe();

        if (!layeredSteps.isEmpty()) {
            data.putBoolean("HasRecipe", true);

            data.putInt("Index", recipeLogic.getLayeredRecipeLayerIndex());
            data.putInt("NumSteps", layeredSteps.size());

            var lastRecipe = layeredSteps.get(layeredSteps.size() - 1);
            var tagsPair = RecipeOutputProvider.gatherRecipeItemAndFluidsOutput(lastRecipe);
            if (!tagsPair.getFirst().isEmpty()) {
                data.put("OutputItems", tagsPair.getFirst());
            }
            if (!tagsPair.getSecond().isEmpty()) {
                data.put("OutputFluids", tagsPair.getSecond());
            }
        } else {
            data.putBoolean("HasRecipe", false);
        }
    }

    @Override
    protected void addTooltip(CompoundTag capData, ITooltip tooltip, Player player, BlockAccessor block,
                              BlockEntity blockEntity, IPluginConfig config) {
        if (!capData.getBoolean("HasRecipe")) return;

        tooltip.add(Component.translatable("gtceu.top.layered_recipe.step", capData.getInt("Index") + 1,
                capData.getInt("NumSteps")));

        if (!tooltip.get(GTCEu.id("recipeoutput")).isEmpty()) return;

        List<Ingredient> outputItems = new ArrayList<>();
        if (capData.contains("OutputItems", Tag.TAG_LIST)) {
            ListTag itemTags = capData.getList("OutputItems", Tag.TAG_COMPOUND);
            if (!itemTags.isEmpty()) {
                for (Tag tag : itemTags) {
                    if (tag instanceof CompoundTag tCompoundTag) {
                        if (tCompoundTag.contains("count_provider")) {
                            var ingredient = IntProviderIngredient.SERIALIZER
                                    .parse((JsonObject) NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, tCompoundTag));
                            outputItems.add(ingredient);
                        } else {
                            var stack = GTUtil.loadItemStack(tCompoundTag);
                            if (!stack.isEmpty()) {
                                outputItems.add(SizedIngredient.create(stack));
                            }
                        }
                    }
                }
            }
        }
        List<FluidIngredient> outputFluids = new ArrayList<>();
        if (capData.contains("OutputFluids", Tag.TAG_LIST)) {
            ListTag fluidTags = capData.getList("OutputFluids", Tag.TAG_COMPOUND);
            for (Tag tag : fluidTags) {
                if (tag instanceof CompoundTag tCompoundTag) {
                    if (tCompoundTag.contains("count_provider")) {
                        var ingredient = IntProviderFluidIngredient.fromNBT(tCompoundTag);
                        outputFluids.add(ingredient);
                    } else {
                        var stack = FluidStack.loadFluidStackFromNBT(tCompoundTag);
                        if (!stack.isEmpty()) {
                            outputFluids.add(FluidIngredient.of(stack));
                        }
                    }
                }
            }
        }
        if (!outputItems.isEmpty() || !outputFluids.isEmpty()) {
            tooltip.add(Component.translatable("gtceu.top.layered_recipe.final"));
        }
        RecipeOutputProvider.addItemTooltips(tooltip, outputItems);
        RecipeOutputProvider.addFluidTooltips(tooltip, outputFluids);
    }
}
