package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;

import java.util.List;

public class GTEmiRecipeHandler implements StandardRecipeHandler<ModularUIContainerMenu> {

    private static boolean isInputSlot(Slot slot) {
        return slot instanceof ItemHandlerSlot handlerSlot &&
                handlerSlot.getItemHandler() instanceof NotifiableItemStackHandler handler &&
                handler.handlerIO == IO.IN;
    }

    @Override
    public List<Slot> getInputSources(ModularUIContainerMenu handler) {
        return handler.slots.stream()
                .filter(slot -> isInputSlot(slot) || slot.container instanceof Inventory)
                .toList();
    }

    @Override
    public List<Slot> getCraftingSlots(ModularUIContainerMenu handler) {
        return handler.slots.stream()
                .filter(GTEmiRecipeHandler::isInputSlot)
                .toList();
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof GTEmiRecipe;
    }
}
