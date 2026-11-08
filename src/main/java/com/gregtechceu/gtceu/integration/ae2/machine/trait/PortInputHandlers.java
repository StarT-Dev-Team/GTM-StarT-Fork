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
import com.gregtechceu.gtceu.utils.GTMath;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PortInputHandlers {

    public interface PortStockAccess {

        int slots();

        @Nullable
        GenericStack stockAt(int slot);

        long extract(int slot, AEKey key, long amount, boolean simulate);
    }

    public static final class Items extends NotifiableItemStackHandler {

        private final PortStockAccess access;

        public Items(MetaMachine machine, PortStockAccess access) {
            super(machine, 0, IO.IN, IO.NONE);

            this.access = access;
        }

        @Override
        public int getSize() {
            return this.access.slots();
        }

        @Override
        public List<Ingredient> handleRecipeInner(IO io, GTRecipe recipe, List<Ingredient> left, boolean simulate) {
            if (io != IO.IN) return left;

            long[] takenAmounts = null;

            for (var it = left.listIterator(); it.hasNext();) {
                var ingredient = it.next();

                if (ingredient.isEmpty()) {
                    it.remove();

                    continue;
                }

                ItemStack[] items = null;

                int amount;

                if (ingredient instanceof IntProviderIngredient provider) {
                    provider.setItemStacks(null);
                    provider.setSampledCount(-1);

                    if (simulate) {
                        items = new ItemStack[] { provider.getMaxSizeStack() };
                    } else {
                        items = provider.getItems();

                        if (items.length == 0 || items[0].isEmpty()) {
                            it.remove();

                            continue;
                        }
                    }

                    amount = items[0].getCount();
                } else if (ingredient instanceof SizedIngredient sized &&
                        !(sized.getInner() instanceof IntProviderIngredient)) {
                            var innerItems = sized.getInner().getItems();

                            amount = sized.getAmount();

                            if (amount <= 0 || innerItems.length == 0 || innerItems[0].isEmpty()) {
                                it.remove();

                                continue;
                            }
                        } else {
                            items = ingredient.getItems();

                            if (items.length == 0 || items[0].isEmpty()) {
                                it.remove();

                                continue;
                            }

                            amount = items[0].getCount();
                        }

                for (int slot = 0; slot < this.access.slots() && amount > 0; slot++) {
                    var stock = this.access.stockAt(slot);

                    if (stock == null || !(stock.what() instanceof AEItemKey key) || !key.matches(ingredient)) continue;

                    long available = stock.amount() - (takenAmounts == null ? 0 : takenAmounts[slot]);
                    int count = GTMath.saturatedCast(available);

                    if (count <= 0) continue;

                    long extracted = this.access.extract(slot, key, Math.min(count, amount), simulate);

                    if (extracted > 0) {
                        if (takenAmounts == null) takenAmounts = new long[this.access.slots()];

                        takenAmounts[slot] += extracted;
                        amount -= (int) extracted;
                    }
                }

                if (amount <= 0) {
                    it.remove();
                } else if (ingredient instanceof SizedIngredient sized) {
                    sized.setAmount(amount);
                } else if (items != null) {
                    items[0].setCount(amount);
                }
            }

            return left.isEmpty() ? null : left;
        }

        @Override
        public @NotNull List<Object> getContents() {
            List<Object> contents = new ArrayList<>();

            for (int i = 0; i < this.access.slots(); i++) {
                var stock = this.access.stockAt(i);

                if (stock != null && stock.what() instanceof AEItemKey key) {
                    contents.addAll(GTMath.splitStacks(key.getReadOnlyStack(), stock.amount()));
                }
            }

            return contents;
        }

        @Override
        public double getTotalContentAmount() {
            double total = 0;

            for (int i = 0; i < this.access.slots(); i++) {
                var stock = this.access.stockAt(i);

                if (stock != null && stock.what() instanceof AEItemKey) total += stock.amount();
            }

            return total;
        }
    }

    public static final class Fluids extends NotifiableRecipeHandlerTrait<FluidIngredient> {

        private final PortStockAccess access;

        public Fluids(MetaMachine machine, PortStockAccess access) {
            super(machine);
            this.access = access;
        }

        @Override
        public IO getHandlerIO() {
            return IO.IN;
        }

        @Override
        public RecipeCapability<FluidIngredient> getCapability() {
            return FluidRecipeCapability.CAP;
        }

        @Override
        public int getSize() {
            return this.access.slots();
        }

        @Override
        public List<FluidIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<FluidIngredient> left,
                                                       boolean simulate) {
            if (io != IO.IN) return left;

            long[] takenAmounts = null;

            for (var it = left.listIterator(); it.hasNext();) {
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

                int amount = fluids[0].getAmount();

                for (int slot = 0; slot < this.access.slots() && amount > 0; slot++) {
                    var stock = this.access.stockAt(slot);

                    if (stock == null || !(stock.what() instanceof AEFluidKey key)) continue;

                    long available = stock.amount() - (takenAmounts == null ? 0 : takenAmounts[slot]);
                    int count = GTMath.saturatedCast(available);

                    if (count <= 0 || !ingredient.test(key.toStack(1))) continue;

                    long extracted = this.access.extract(slot, key, Math.min(count, amount), simulate);

                    if (extracted > 0) {
                        if (takenAmounts == null) takenAmounts = new long[this.access.slots()];

                        takenAmounts[slot] += extracted;
                        amount -= (int) extracted;
                    }
                }

                if (amount <= 0) {
                    it.remove();
                } else {
                    ingredient.setAmount(amount);
                }
            }

            return left.isEmpty() ? null : left;
        }

        @Override
        public @NotNull List<Object> getContents() {
            List<Object> contents = new ArrayList<>();

            for (int i = 0; i < this.access.slots(); i++) {
                var stock = this.access.stockAt(i);

                if (stock != null && stock.what() instanceof AEFluidKey key) {
                    contents.addAll(GTMath.splitFluidStacks(key.toStack(1), stock.amount()));
                }
            }

            return contents;
        }

        @Override
        public double getTotalContentAmount() {
            double total = 0;

            for (int i = 0; i < this.access.slots(); i++) {
                var stock = this.access.stockAt(i);

                if (stock != null && stock.what() instanceof AEFluidKey) total += stock.amount();
            }

            return total;
        }

        public void notifyChanged() {
            notifyListeners();
        }
    }
}
