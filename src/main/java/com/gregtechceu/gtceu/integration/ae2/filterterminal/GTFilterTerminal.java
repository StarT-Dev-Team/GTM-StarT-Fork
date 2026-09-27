package com.gregtechceu.gtceu.integration.ae2.filterterminal;

import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlotList;

import net.minecraft.server.level.ServerPlayer;

import appeng.api.filterterminal.FilterTerminalTargetMetadata;
import appeng.api.filterterminal.IFilterTerminalConfigView;
import appeng.api.filterterminal.IFilterTerminalTarget;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

final class GTFilterTerminal {

    record Target(Object identity, IGridNode gridNode, FilterTerminalTargetMetadata metadata,
                  IFilterTerminalConfigView configView, Predicate<ServerPlayer> canEdit)
            implements IFilterTerminalTarget {

        @Override
        public Object getIdentity() {
            return identity;
        }

        @Override
        public IGridNode getGridNode() {
            return gridNode;
        }

        @Override
        public FilterTerminalTargetMetadata getMetadata() {
            return metadata;
        }

        @Override
        public IFilterTerminalConfigView getConfigView() {
            return configView;
        }

        @Override
        public boolean canEdit(ServerPlayer player) {
            return canEdit.test(player);
        }
    }

    record ConfigView(IConfigurableSlotList slots, AEKeyType keyType, boolean stocking,
                      BooleanSupplier readOnly, Runnable configChanged)
            implements IFilterTerminalConfigView {

        private static final long MAX_AMOUNT = Integer.MAX_VALUE;

        @Override
        public int size() {
            return slots.getConfigurableSlots();
        }

        @Nullable
        @Override
        public GenericStack getConfig(int slot) {
            return slots.getConfigurableSlot(slot).getConfig();
        }

        @Nullable
        @Override
        public GenericStack getStock(int slot) {
            return slots.getConfigurableSlot(slot).getStock();
        }

        @Override
        public boolean acceptsKeyType(int slot, AEKeyType keyType) {
            return isValidSlot(slot) && this.keyType == keyType;
        }

        @Override
        public boolean canEditConfig(int slot) {
            return isValidSlot(slot) && !readOnly.getAsBoolean();
        }

        @Override
        public boolean canSetConfig(int slot, @Nullable GenericStack stack) {
            if (!canEditConfig(slot)) {
                return false;
            }
            if (stack == null) {
                return true;
            }
            if (stack.what().getType() != keyType || stack.amount() <= 0 || stack.amount() > MAX_AMOUNT) {
                return false;
            }
            return !stocking || !slots.hasStackInConfig(stack, true);
        }

        @Override
        public void setConfig(int slot, @Nullable GenericStack stack) {
            if (!canSetConfig(slot, stack)) {
                return;
            }

            var configurableSlot = slots.getConfigurableSlot(slot);
            if (!Objects.equals(configurableSlot.getConfig(), stack)) {
                configurableSlot.setConfig(stack);
                configChanged.run();
            }
        }

        @Override
        public boolean canEditAmount(int slot) {
            return !stocking && canEditConfig(slot);
        }

        @Override
        public long getMaxAmount(int slot, AEKey key) {
            return MAX_AMOUNT;
        }

        private boolean isValidSlot(int slot) {
            return slot >= 0 && slot < size();
        }

        @Override
        public byte getSlotsPerRow() {
            return 8;
        }
    }
}
