package com.gregtechceu.gtceu.integration.ae2.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class PortOutputHandlers {

    public interface Sink {

        long insert(AEKey key, long amount, boolean simulate);
    }

    public static final class Items extends NotifiableItemStackHandler {

        private final Sink sink;

        public Items(MetaMachine machine, Sink sink) {
            super(machine, 0, IO.OUT, IO.NONE);

            this.sink = sink;
        }

        @Override
        public List<Ingredient> handleRecipeInner(IO io, GTRecipe recipe, List<Ingredient> left, boolean simulate) {
            if (io != IO.OUT) return left;

            for (var it = left.listIterator(); it.hasNext();) {
                var ingredient = it.next();

                if (ingredient.isEmpty()) {
                    it.remove();

                    continue;
                }

                ItemStack[] items;

                int amount;

                if (ingredient instanceof IntProviderIngredient provider) {
                    provider.setItemStacks(null);
                    provider.setSampledCount(-1);

                    if (simulate) {
                        items = new ItemStack[] { provider.getMaxSizeStack() };
                    } else {
                        items = provider.getItems();
                    }

                    if (items.length == 0 || items[0].isEmpty()) {
                        it.remove();

                        continue;
                    }

                    amount = items[0].getCount();
                } else {
                    items = ingredient.getItems();

                    if (items.length == 0 || items[0].isEmpty()) {
                        it.remove();

                        continue;
                    }

                    amount = ingredient instanceof SizedIngredient sized ? sized.getAmount() : items[0].getCount();
                }

                long accepted = this.sink.insert(AEItemKey.of(items[0]), amount, simulate);
                long remaining = accepted > 0 ? amount - accepted : 0;

                if (remaining <= 0) {
                    it.remove();
                } else if (ingredient instanceof SizedIngredient sized) {
                    sized.setAmount((int) remaining);
                } else {
                    items[0].setCount((int) remaining);
                }
            }

            return left.isEmpty() ? null : left;
        }

        @Override
        public @NotNull List<Object> getContents() {
            return Collections.emptyList();
        }

        @Override
        public double getTotalContentAmount() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    }

    public static final class Fluids extends NotifiableRecipeHandlerTrait<FluidIngredient> {

        private final Sink sink;

        public Fluids(MetaMachine machine, Sink sink) {
            super(machine);
            this.sink = sink;
        }

        @Override
        public IO getHandlerIO() {
            return IO.OUT;
        }

        @Override
        public RecipeCapability<FluidIngredient> getCapability() {
            return FluidRecipeCapability.CAP;
        }

        @Override
        public List<FluidIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<FluidIngredient> left,
                                                       boolean simulate) {
            if (io != IO.OUT) return left;

            for (var it = left.iterator(); it.hasNext();) {
                var ingredient = it.next();

                if (ingredient.isEmpty()) {
                    it.remove();

                    continue;
                }

                var fluids = ingredient.getStacks();

                if (fluids.length == 0 || fluids[0].isEmpty()) {
                    it.remove();

                    continue;
                }

                var output = fluids[0];
                int amount = output.getAmount();
                long accepted = this.sink.insert(AEFluidKey.of(output.getFluid(), output.getTag()), amount, simulate);
                long remaining = accepted > 0 ? amount - accepted : 0;

                if (remaining <= 0) {
                    it.remove();
                } else {
                    ingredient.setAmount((int) remaining);
                }
            }

            return left.isEmpty() ? null : left;
        }

        @Override
        public @NotNull List<Object> getContents() {
            return Collections.emptyList();
        }

        @Override
        public double getTotalContentAmount() {
            return 0;
        }

        public void notifyChanged() {
            notifyListeners();
        }
    }
}
