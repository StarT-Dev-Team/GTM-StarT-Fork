package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeTransaction;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.MEStatusLabel;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.list.AEListGridWidget;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The Output Bus that can directly send its contents to ME storage network.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MEOutputBusPartMachine extends MEBusPartMachine implements IMachineLife, IInteractedMachine {

    @Getter
    @Persisted
    private KeyStorage internalBuffer; // Do not use KeyCounter, use our simple implementation

    public MEOutputBusPartMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, IO.OUT, args);
    }

    /////////////////////////////////
    // ***** Machine LifeCycle ****//
    /////////////////////////////////

    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        this.internalBuffer = new KeyStorage();
        return new InaccessibleInfiniteHandler(this);
    }

    @Override
    public void onMachineRemoved() {
        var grid = getMainNode().getGrid();
        if (grid != null && !internalBuffer.isEmpty()) {
            for (var entry : internalBuffer) {
                grid.getStorageService().getInventory().insert(entry.getKey(), entry.getLongValue(),
                        Actionable.MODULATE, actionSource);
            }
        }
    }

    /////////////////////////////////
    // ********** Sync ME *********//
    /////////////////////////////////

    @Override
    protected boolean shouldSubscribe() {
        return super.shouldSubscribe() && !internalBuffer.storage.isEmpty();
    }

    @Override
    public void autoIO() {
        if (!this.shouldSyncME()) return;
        if (this.updateMEStatus()) {
            var grid = getMainNode().getGrid();
            if (grid != null && !internalBuffer.isEmpty()) {
                internalBuffer.insertInventory(grid.getStorageService().getInventory(), actionSource);
            }
            this.updateInventorySubscription();
        }
    }

    ///////////////////////////////
    // ********** GUI ***********//
    ///////////////////////////////

    @Override
    public UIElement createUIWidget() {
        var group = new UIElement();
        group.layout(layout -> layout.width(170).paddingHorizontal(5).gapAll(1));
        var waiting = new Label();
        waiting.setText(Component.translatable("gtceu.gui.waiting_list"));
        waiting.textStyle(style -> style.textColor(0xFF404040).textShadow(false));
        group.addChildren(new MEStatusLabel(() -> this.isOnline), waiting,
                new AEListGridWidget.Item(3, this.internalBuffer));
        return group;
    }

    private class InaccessibleInfiniteHandler extends NotifiableItemStackHandler {

        public InaccessibleInfiniteHandler(MetaMachine holder) {
            super(holder, 1, IO.OUT, IO.NONE, ItemStackHandlerDelegate::new);
            internalBuffer.setOnContentsChanged(this::onContentsChanged);
        }

        private static boolean hasIngredientActions(GTRecipe recipe) {
            return GTCEu.Mods.isKubeJSLoaded() && recipe.ingredientActions != null &&
                    !recipe.ingredientActions.isEmpty();
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

        /// Insert straight into the buffer instead of allocating a full array of size
        /// {@link ItemStackHandlerDelegate#getSlots()}
        @Override
        public @Nullable List<Ingredient> handleRecipeInner(IO io, GTRecipe recipe,
                                                            List<Ingredient> left,
                                                            boolean simulate) {
            if (io != IO.OUT) return left;
            if (hasIngredientActions(recipe)) return super.handleRecipeInner(io, recipe, left, simulate);

            for (var it = left.iterator(); it.hasNext();) {
                var ingredient = it.next();
                if (ingredient.isEmpty()) {
                    it.remove();
                    continue;
                }

                ItemStack output;
                if (ingredient instanceof IntProviderIngredient provider) {
                    provider.setItemStacks(null);
                    provider.setSampledCount(-1);
                    if (simulate) {
                        output = provider.getMaxSizeStack();
                    } else {
                        var items = provider.getItems();
                        output = items.length == 0 ? ItemStack.EMPTY : items[0];
                    }
                } else {
                    var items = ingredient.getItems();
                    output = items.length == 0 ? ItemStack.EMPTY : items[0];
                }
                int amount = ingredient instanceof SizedIngredient si ? si.getAmount() : output.getCount();
                if (output.isEmpty() || amount <= 0) {
                    it.remove();
                    continue;
                }

                // the delegate records the transaction undo itself
                int remaining = storage.insertItem(0, output.copyWithCount(amount), simulate).getCount();
                if (remaining <= 0) it.remove();
                else if (ingredient instanceof SizedIngredient si) si.setAmount(remaining);
                else output.setCount(remaining);
            }
            return left.isEmpty() ? null : left;
        }
    }

    @NoArgsConstructor
    private class ItemStackHandlerDelegate extends CustomItemStackHandler {

        // Necessary for InaccessibleInfiniteHandler
        public ItemStackHandlerDelegate(Integer integer) {
            super();
        }

        @Override
        public int getSlots() {
            return Short.MAX_VALUE;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            // NO-OP
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            var key = AEItemKey.of(stack);
            int count = stack.getCount();
            long oldValue = internalBuffer.storage.getOrDefault(key, 0);
            long changeValue = Math.min(Long.MAX_VALUE - oldValue, count);
            if (changeValue > 0) {
                if (!simulate) {
                    internalBuffer.storage.put(key, oldValue + changeValue);
                    internalBuffer.onChanged();
                    // Take the items back out of the buffer if the recipe fails after this point
                    RecipeTransaction.record(() -> {
                        var current = internalBuffer.storage.getOrDefault(key, 0L) - changeValue;
                        if (current > 0) internalBuffer.storage.put(key, current);
                        else internalBuffer.storage.removeLong(key);
                        internalBuffer.onChanged();
                    });
                }
                return stack.copyWithCount((int) (count - changeValue));
            } else {
                return ItemStack.EMPTY;
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }
}
