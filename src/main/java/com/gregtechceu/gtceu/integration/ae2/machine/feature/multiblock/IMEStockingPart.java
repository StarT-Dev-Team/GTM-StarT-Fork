package com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.integration.ae2.utils.TopStacks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface IMEStockingPart extends IAutoPullPart, IMEInputPart {

    Predicate<GenericStack> getAutoPullTest();

    long getMinSizeFor(AEKey key);

    int getMinStackSize();

    void setMinStackSize(int newSize);

    @Override
    default void addedToController(IMultiController controller) {
        setAutoPullTest(stack -> !this.testConfiguredInOtherPart(stack));

        if (self().getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::validateConfig));
        }
    }

    @Override
    default void removedFromController(IMultiController controller) {
        setAutoPullTest($ -> false);

        if (isAutoPull()) {
            getSlotList().clearInventory(0);
        }
    }

    default boolean testConfiguredInOtherPart(@Nullable GenericStack config) {
        if (config == null) return false;
        if (!isFormed() || (this instanceof IDistinctPart distinct && distinct.isDistinct())) return false;

        for (IMultiController controller : getControllers()) {
            for (IMultiPart part : controller.getParts()) {
                if (part == this || !(part instanceof IMEStockingPart other)) continue;
                if (part instanceof IDistinctPart distinct && distinct.isDistinct()) continue;

                if (other.getSlotList().hasStackInConfig(config, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    default void validateConfig() {
        var slots = getSlotList();

        for (int i = 0; i < slots.getConfigurableSlots(); i++) {
            var slot = slots.getConfigurableSlot(i);

            if (slot.getConfig() != null && testConfiguredInOtherPart(slot.getConfig())) {
                slot.setConfig(null);
                slot.setStock(null);
            }
        }
    }

    default void refreshList() {
        var grid = getMainNode().getGrid();
        var slots = getSlotList();

        if (grid == null) {
            slots.clearInventory(0);

            return;
        }

        var network = grid.getStorageService().getInventory();
        var topStacks = TopStacks.selectMixed(network.getAvailableStacks(), slots.getConfigurableSlots(),
                this::getMinSizeFor, getAutoPullTest());
        int count = topStacks.size();
        int index;

        for (index = 0; index < slots.getConfigurableSlots(); index++) {
            if (topStacks.isEmpty()) break;

            var entry = topStacks.poll();
            long request = network.extract(entry.what(), entry.amount(), Actionable.SIMULATE, getActionSource());

            if (request == 0) continue;

            var slot = slots.getConfigurableSlot(count - index - 1);

            slot.setConfig(new GenericStack(entry.what(), 1));
            slot.setStock(new GenericStack(entry.what(), request));
        }

        slots.clearInventory(index);
    }

    default void onAutoPullChanged() {
        if (self().isRemote()) return;

        if (!isAutoPull()) {
            getSlotList().clearInventory(0);
        } else if (updateMEStatus()) {
            refreshList();

            updateSubscription();
        }
    }

    default InteractionResult toggleAutoPull(Player player) {
        if (!self().isRemote()) {
            setAutoPull(!isAutoPull());

            player.sendSystemMessage(Component.translatable(isAutoPull() ?
                    "gtceu.machine.me.stocking_auto_pull_enabled" : "gtceu.machine.me.stocking_auto_pull_disabled"));
        }

        return InteractionResult.sidedSuccess(self().isRemote());
    }

    @Override
    default CompoundTag writeConfigToTag(@Nullable Class<? extends AEKey> keyType) {
        if (!isAutoPull()) {
            CompoundTag tag = IMEInputPart.super.writeConfigToTag(keyType);

            tag.putBoolean("AutoPull", false);

            return tag;
        }

        CompoundTag tag = new CompoundTag();

        tag.putBoolean("AutoPull", true);
        tag.putInt("TicksPerCycle", getTicksPerCycle());

        return tag;
    }

    @Override
    default void readConfigFromTag(CompoundTag tag) {
        if (tag.getBoolean("AutoPull")) {
            setAutoPull(true);

            if (tag.contains("TicksPerCycle")) {
                setTicksPerCycle(tag.getInt("TicksPerCycle"));
            }

            return;
        }

        setAutoPull(false);

        IMEInputPart.super.readConfigFromTag(tag);
    }
}
