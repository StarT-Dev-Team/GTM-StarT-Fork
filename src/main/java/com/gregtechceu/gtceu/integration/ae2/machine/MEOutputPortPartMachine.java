package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.RecipeTransaction;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.MEStatusLabel;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.list.AEListGridWidget;
import com.gregtechceu.gtceu.integration.ae2.machine.trait.PortOutputHandlers;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.network.chat.Component;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;

public class MEOutputPortPartMachine extends MEBusPartMachine {

    @Persisted
    private KeyStorage internalBuffer;

    private PortOutputHandlers.Items itemHandler;
    private PortOutputHandlers.Fluids fluidHandler;

    public MEOutputPortPartMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, IO.OUT, args);

        this.fluidHandler = new PortOutputHandlers.Fluids(this, this::insertIntoBuffer);
    }

    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        this.internalBuffer = new KeyStorage();
        this.itemHandler = new PortOutputHandlers.Items(this, this::insertIntoBuffer);

        this.internalBuffer.setOnContentsChanged(this::onBufferChanged);

        return this.itemHandler;
    }

    private void onBufferChanged() {
        if (this.itemHandler != null) this.itemHandler.onContentsChanged();
        if (this.fluidHandler != null) this.fluidHandler.notifyChanged();
    }

    private long insertIntoBuffer(AEKey key, long amount, boolean simulate) {
        long oldValue = this.internalBuffer.storage.getOrDefault(key, 0L);
        long change = Math.min(Long.MAX_VALUE - oldValue, amount);

        if (change > 0 && !simulate) {
            this.internalBuffer.storage.put(key, oldValue + change);
            this.internalBuffer.onChanged();

            RecipeTransaction.record(() -> {
                var current = this.internalBuffer.storage.getOrDefault(key, 0L) - change;

                if (current > 0) this.internalBuffer.storage.put(key, current);
                else this.internalBuffer.storage.removeLong(key);

                this.internalBuffer.onChanged();
            });
        }

        return Math.max(change, 0);
    }

    @Override
    public void onMachineRemoved() {
        var grid = getMainNode().getGrid();
        if (grid != null && !this.internalBuffer.isEmpty()) {
            for (var entry : this.internalBuffer) {
                grid.getStorageService().getInventory().insert(entry.getKey(), entry.getLongValue(),
                        Actionable.MODULATE, actionSource);
            }
        }
    }

    @Override
    protected boolean shouldSubscribe() {
        return super.shouldSubscribe() && !this.internalBuffer.storage.isEmpty();
    }

    @Override
    protected void autoIO() {
        if (!this.shouldSyncME()) return;
        if (this.updateMEStatus()) {
            var grid = getMainNode().getGrid();

            if (grid != null && !this.internalBuffer.isEmpty()) {
                this.internalBuffer.insertInventory(grid.getStorageService().getInventory(), this.actionSource);
            }

            this.updateInventorySubscription();
        }
    }

    @Override
    public UIElement createUIWidget() {
        var group = new UIElement();
        group.layout(layout -> layout.width(170).paddingHorizontal(5).gapAll(1));
        var waiting = new Label();
        waiting.setText(Component.translatable("gtceu.gui.waiting_list"));
        waiting.textStyle(style -> style.textColor(0xFF404040).textShadow(false));
        group.addChildren(new MEStatusLabel(() -> this.isOnline), waiting,
                new AEListGridWidget.Generic(3, this.internalBuffer));
        return group;
    }
}
