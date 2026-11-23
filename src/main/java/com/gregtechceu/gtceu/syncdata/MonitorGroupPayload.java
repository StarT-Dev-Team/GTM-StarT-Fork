package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;

import com.lowdragmc.lowdraglib2.compat.network.ByteBufCodecs;
import com.lowdragmc.lowdraglib2.compat.network.StreamCodec;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;

public final class MonitorGroupPayload {

    public static final Codec<MonitorGroup> CODEC = CompoundTag.CODEC.xmap(MonitorGroupPayload::read,
            MonitorGroupPayload::write);

    public static final StreamCodec<ByteBuf, MonitorGroup> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private MonitorGroupPayload() {}

    public static CompoundTag write(MonitorGroup group) {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", group.getName());
        ListTag list = new ListTag();
        group.getMonitorPositions().forEach(pos -> list.add(NbtUtils.writeBlockPos(pos)));
        if (group.getTargetRaw() != null) {
            tag.put("targetPos", NbtUtils.writeBlockPos(group.getTargetRaw()));
            if (group.getTargetCoverSide() != null) {
                tag.putString("targetSide", group.getTargetCoverSide().getSerializedName());
            }
        }
        tag.put("positions", list);
        tag.putInt("dataSlot", group.getDataSlot());
        tag.put("items", group.getItemStackHandler().serializeNBT());
        tag.put("placeholderSlots", group.getPlaceholderSlotsHandler().serializeNBT());
        return tag;
    }

    public static MonitorGroup read(CompoundTag tag) {
        var handler = new CustomItemStackHandler();
        var placeholderSlotsHandler = new CustomItemStackHandler();
        handler.deserializeNBT(tag.getCompound("items"));
        placeholderSlotsHandler.deserializeNBT(tag.getCompound("placeholderSlots"));
        var group = new MonitorGroup(tag.getString("name"), handler, placeholderSlotsHandler);
        ListTag list = tag.getList("positions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            group.add(NbtUtils.readBlockPos(list.getCompound(i)));
        }
        if (tag.contains("targetPos", Tag.TAG_COMPOUND)) {
            group.setTarget(NbtUtils.readBlockPos(tag.getCompound("targetPos")));
            if (tag.contains("targetSide", Tag.TAG_STRING)) {
                group.setTargetCoverSide(Direction.byName(tag.getString("targetSide")));
            }
            if (tag.contains("dataSlot", Tag.TAG_INT)) {
                group.setDataSlot(tag.getInt("dataSlot"));
            }
        }
        return group;
    }
}
