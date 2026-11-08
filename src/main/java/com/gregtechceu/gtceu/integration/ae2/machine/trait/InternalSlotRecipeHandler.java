package com.gregtechceu.gtceu.integration.ae2.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerGroupDistinctness;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine.InternalSlot;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class InternalSlotRecipeHandler {

    @Getter
    private final List<RecipeHandlerList> slotHandlers;

    public InternalSlotRecipeHandler(MEPatternBufferPartMachine buffer, InternalSlot[] slots) {
        this.slotHandlers = new ArrayList<>(slots.length);
        for (int i = 0; i < slots.length; i++) {
            var rhl = new SlotRHL(buffer, slots[i], i);
            slotHandlers.add(rhl);
        }
    }

    @Getter
    protected static class SlotRHL extends RecipeHandlerList {

        private final SlotItemRecipeHandler itemRecipeHandler;
        private final SlotFluidRecipeHandler fluidRecipeHandler;

        private final MEPatternBufferPartMachine buffer;
        private final InternalSlot slot;

        public SlotRHL(MEPatternBufferPartMachine buffer, InternalSlot slot, int idx) {
            super(IO.IN);

            this.buffer = buffer;
            this.slot = slot;

            itemRecipeHandler = new SlotItemRecipeHandler(buffer, slot, idx);
            fluidRecipeHandler = new SlotFluidRecipeHandler(buffer, slot, idx);

            slot.setOnContentsChanged(() -> {
                itemRecipeHandler.notifyChanged();
                fluidRecipeHandler.notifyChanged();
            });

            addHandlers(buffer.getCircuitInventory(), buffer.getShareInventory(), buffer.getShareTank(),
                    itemRecipeHandler, fluidRecipeHandler);

            this.setGroup(RecipeHandlerGroupDistinctness.BUS_DISTINCT);
        }

        @Override
        public boolean cannotSupplyItems(List<Object> itemIngredients, Map<Object, Object> memo) {
            for (Object ingredient : itemIngredients) {
                if (!(ingredient instanceof SizedIngredient si) || si.getInner() instanceof IntProviderIngredient) {
                    continue;
                }

                if (si.getAmount() <= 0 || si.isEmpty()) continue;
                if (this.slot.hasItemMatching(si) || sharedItemsMatch(si, memo)) continue;
                if (si.getInner().getItems().length == 0) continue;

                return true;
            }

            return false;
        }

        @Override
        public boolean cannotSupplyFluids(List<Object> fluidIngredients, Map<Object, Object> memo) {
            for (Object ingredient : fluidIngredients) {
                if (!(ingredient instanceof FluidIngredient fi) || fi.isEmpty() || fi.getAmount() <= 0) continue;
                if (this.slot.hasFluidMatching(fi) || sharedFluidsMatch(fi, memo)) continue;
                if (fi.getStacks().length == 0) continue;

                return true;
            }

            return false;
        }

        @SuppressWarnings("unchecked")
        private boolean sharedItemsMatch(Ingredient ingredient, Map<Object, Object> memo) {
            var results = (Map<Object, Boolean>) memo.computeIfAbsent(Pair.of(this.buffer, ItemRecipeCapability.CAP),
                    key -> new IdentityHashMap<>());

            return results.computeIfAbsent(ingredient,
                    key -> storageMatches(this.buffer.getShareInventory(), ingredient) ||
                            storageMatches(this.buffer.getCircuitInventory(), ingredient));
        }

        @SuppressWarnings("unchecked")
        private boolean sharedFluidsMatch(FluidIngredient ingredient, Map<Object, Object> memo) {
            var results = (Map<Object, Boolean>) memo.computeIfAbsent(Pair.of(this.buffer, FluidRecipeCapability.CAP),
                    key -> new IdentityHashMap<>());

            return results.computeIfAbsent(ingredient, key -> tankMatches(this.buffer.getShareTank(), ingredient));
        }

        private static boolean tankMatches(NotifiableFluidTank tank, FluidIngredient ingredient) {
            for (var storage : tank.getStorages()) {
                if (ingredient.test(storage.getFluid())) return true;
            }

            return false;
        }

        private static boolean storageMatches(NotifiableItemStackHandler handler, Ingredient ingredient) {
            var storage = handler.storage;

            for (int i = 0; i < storage.getSlots(); i++) {
                var stack = storage.getStackInSlot(i);

                if (!stack.isEmpty() && ingredient.test(stack)) return true;
            }

            return false;
        }

        @Override
        public boolean isDistinct() {
            return true;
        }

        @Override
        public void setDistinct(boolean ignored, boolean notify) {}
    }

    @Getter
    private static class SlotItemRecipeHandler extends NotifiableRecipeHandlerTrait<Ingredient> {

        private final InternalSlot slot;
        private final int priority;

        private final int size = 81;
        private final RecipeCapability<Ingredient> capability = ItemRecipeCapability.CAP;
        private final IO handlerIO = IO.IN;
        private final boolean isDistinct = true;

        private SlotItemRecipeHandler(MEPatternBufferPartMachine buffer, InternalSlot slot, int index) {
            super(buffer);
            this.slot = slot;
            this.priority = IFilteredHandler.HIGH + index + 1;
        }

        private void notifyChanged() {
            notifyListeners();
        }

        @Override
        public List<Ingredient> handleRecipeInner(IO io, GTRecipe recipe, List<Ingredient> left, boolean simulate) {
            if (io != IO.IN || slot.isItemEmpty()) return left;
            return slot.handleItemInternal(left, simulate);
        }

        @Override
        public @NotNull List<Object> getContents() {
            return new ArrayList<>(slot.getItems());
        }

        @Override
        public double getTotalContentAmount() {
            return slot.getItems().stream().mapToLong(ItemStack::getCount).sum();
        }
    }

    @Getter
    private static class SlotFluidRecipeHandler extends NotifiableRecipeHandlerTrait<FluidIngredient> {

        private final InternalSlot slot;
        private final int priority;

        private final int size = 81;
        private final RecipeCapability<FluidIngredient> capability = FluidRecipeCapability.CAP;
        private final IO handlerIO = IO.IN;
        private final boolean isDistinct = true;

        private SlotFluidRecipeHandler(MEPatternBufferPartMachine buffer, InternalSlot slot, int index) {
            super(buffer);
            this.slot = slot;
            this.priority = IFilteredHandler.HIGH + index + 1;
        }

        private void notifyChanged() {
            notifyListeners();
        }

        @Override
        public List<FluidIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<FluidIngredient> left,
                                                       boolean simulate) {
            if (io != IO.IN || slot.isFluidEmpty()) return left;

            return slot.handleFluidInternal(left, simulate);
        }

        @Override
        public @NotNull List<Object> getContents() {
            return new ArrayList<>(slot.getFluids());
        }

        @Override
        public double getTotalContentAmount() {
            return slot.getFluids().stream().mapToLong(FluidStack::getAmount).sum();
        }
    }
}
