package com.gregtechceu.gtceu.integration.ae2.slot;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;

import net.minecraft.nbt.CompoundTag;

import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.ConfigInventory;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class PortAEInventory implements IConfigurableSlotList, ITagSerializable<CompoundTag>, IContentChangeAware {

    public static final int BASE_SIZE = 16;
    public static final int SLOTS_PER_TIER = 4;

    public static int slotsForTier(int tier, int baseTier) {
        return BASE_SIZE + SLOTS_PER_TIER * Math.max(0, tier - baseTier);
    }

    private static final class OverstackingInventory extends ConfigInventory {

        OverstackingInventory(GenericStackInv.Mode mode, int size, Runnable listener) {
            super(null, mode, size, listener, true);
        }
    }

    public final ConfigInventory config;
    public final ConfigInventory stock;

    private final int size;
    @Getter
    private final IConfigurableSlot[] views;
    private final Runnable onChanged;
    private Runnable onContentsChanged = () -> {};

    public PortAEInventory(Runnable onChanged, int size) {
        this.onChanged = onChanged;
        this.size = size;
        this.views = new IConfigurableSlot[size];
        this.config = new OverstackingInventory(GenericStackInv.Mode.CONFIG_STACKS, size, this::changed);
        this.stock = new OverstackingInventory(GenericStackInv.Mode.STORAGE, size, this::changed);

        for (int i = 0; i < size; i++) {
            this.views[i] = new View(i);
        }
    }

    private void changed() {
        this.onChanged.run();
        this.onContentsChanged.run();
    }

    @Override
    public Runnable getOnContentsChanged() {
        return this.onContentsChanged;
    }

    @Override
    public void setOnContentsChanged(Runnable onContentsChanged) {
        this.onContentsChanged = onContentsChanged;
    }

    @Override
    public IConfigurableSlot getConfigurableSlot(int index) {
        return this.views[index];
    }

    @Override
    public int getConfigurableSlots() {
        return this.size;
    }

    public int size() {
        return this.size;
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();

        this.config.writeToChildTag(tag, "config");
        this.stock.writeToChildTag(tag, "stock");

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.config.readFromChildTag(tag, "config");
        this.stock.readFromChildTag(tag, "stock");
    }

    private final class View implements IConfigurableSlot {

        private final int index;

        private View(int index) {
            this.index = index;
        }

        @Override
        public @Nullable GenericStack getConfig() {
            return config.getStack(this.index);
        }

        @Override
        public @Nullable GenericStack getStock() {
            return stock.getStack(this.index);
        }

        @Override
        public void setConfig(@Nullable GenericStack val) {
            config.setStack(this.index, val);
        }

        @Override
        public void setStock(@Nullable GenericStack val) {
            stock.setStack(this.index, val);
        }

        @Override
        public IConfigurableSlot copy() {
            return new Snapshot(getConfig(), getStock());
        }
    }

    public static final class Snapshot implements IConfigurableSlot {

        private GenericStack config;
        private GenericStack stock;

        public Snapshot() {}

        public Snapshot(@Nullable GenericStack config, @Nullable GenericStack stock) {
            this.config = config;
            this.stock = stock;
        }

        @Override
        public @Nullable GenericStack getConfig() {
            return this.config;
        }

        @Override
        public @Nullable GenericStack getStock() {
            return this.stock;
        }

        @Override
        public void setConfig(@Nullable GenericStack val) {
            this.config = val;
        }

        @Override
        public void setStock(@Nullable GenericStack val) {
            this.stock = val;
        }

        @Override
        public IConfigurableSlot copy() {
            return new Snapshot(this.config, this.stock);
        }
    }
}
