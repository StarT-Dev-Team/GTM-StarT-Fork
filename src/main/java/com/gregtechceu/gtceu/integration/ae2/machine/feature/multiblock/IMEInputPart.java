package com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock;

import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlotList;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IMEInputPart extends IMEIOPart, IDataStickInteractable {

    record DataStickFormat(String key, @Nullable Class<? extends AEKey> keyType) {}

    IConfigurableSlotList getSlotList();

    List<DataStickFormat> getDataStickFormats();

    String getDataStickNameKey();

    @Override
    default InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        if (!self().isRemote()) {
            CompoundTag tag = new CompoundTag();

            for (var format : getDataStickFormats()) {
                tag.put(format.key(), writeConfigToTag(format.keyType()));
            }

            dataStick.setTag(tag);
            dataStick.setHoverName(Component.translatable(getDataStickNameKey()));

            player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_copy_settings"));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    default InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        CompoundTag tag = dataStick.getTag();

        if (tag == null) return InteractionResult.PASS;

        var format = getDataStickFormats().stream().filter(candidate -> tag.contains(candidate.key())).findFirst();

        if (format.isEmpty()) return InteractionResult.PASS;

        if (!self().isRemote()) {
            readConfigFromTag(tag.getCompound(format.get().key()));

            updateSubscription();

            player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_paste_settings"));
        }

        return InteractionResult.sidedSuccess(self().isRemote());
    }

    default CompoundTag writeConfigToTag(@Nullable Class<? extends AEKey> keyType) {
        CompoundTag tag = new CompoundTag();
        CompoundTag configStacks = new CompoundTag();
        var slots = getSlotList();

        tag.put("ConfigStacks", configStacks);

        for (int i = 0; i < slots.getConfigurableSlots(); i++) {
            GenericStack config = slots.getConfigurableSlot(i).getConfig();

            if (config == null || (keyType != null && !keyType.isInstance(config.what()))) continue;

            configStacks.put(Integer.toString(i), GenericStack.writeTag(config));
        }

        if (keyType != AEFluidKey.class && this instanceof IDistinctPart part) {
            tag.putBoolean("DistinctBuses", part.isDistinct());
        }

        tag.putInt("TicksPerCycle", getTicksPerCycle());

        return tag;
    }

    default void readConfigFromTag(CompoundTag tag) {
        if (tag.contains("ConfigStacks")) {
            CompoundTag configStacks = tag.getCompound("ConfigStacks");
            var slots = getSlotList();

            for (int i = 0; i < slots.getConfigurableSlots(); i++) {
                String key = Integer.toString(i);

                slots.getConfigurableSlot(i).setConfig(configStacks.contains(key) ?
                        GenericStack.readTag(configStacks.getCompound(key)) : null);
            }
        }

        if (tag.contains("DistinctBuses") && this instanceof IDistinctPart part) {
            part.setDistinct(tag.getBoolean("DistinctBuses"));
        }

        if (tag.contains("TicksPerCycle")) {
            setTicksPerCycle(tag.getInt("TicksPerCycle"));
        }
    }
}
