package com.gregtechceu.gtceu.api.machine.trait;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;

import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.data.ModelData;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * represents an abstract capability held by machine. Such as item, fluid, energy, etc.
 * All trait should be added while MetaMachine is creating. you cannot modify it on the fly。
 */
public abstract class MachineTrait implements IMachineTrait, IEnhancedManaged {

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Getter
    protected MetaMachine machine;
    @Setter
    protected Predicate<@Nullable Direction> capabilityValidator;
    @Getter
    @Setter
    protected int priority;
    @Getter
    @Setter
    private @Nullable String persistedName;

    public MachineTrait() {
        this.capabilityValidator = side -> true;
    }

    protected List<Class<?>> validMachineClasses() {
        return List.of();
    }

    @ApiStatus.Internal
    public void setMachine(MetaMachine machine) {
        if (this.machine != null) throw new IllegalStateException("Machine trait already attached to a machine.");
        if (!validMachineClasses().isEmpty() &&
                validMachineClasses().stream().noneMatch(cls -> cls.isAssignableFrom(machine.getClass()))) {
            throw new IllegalArgumentException(
                    "Attempted to attach trait to invalid machine class %s".formatted(machine.getClass()));
        }
        this.machine = machine;
    }

    public final boolean hasCapability(@Nullable Direction side) {
        return capabilityValidator.test(side);
    }

    @Override
    public void onChanged() {
        machine.onChanged();
    }

    public void updateModelData(ModelData.Builder builder) {}

    public MachineRenderState getRenderState() {
        return getMachine().getRenderState();
    }

    public void setRenderState(MachineRenderState state) {
        getMachine().setRenderState(state);
    }

    /**
     * Use for data not able to be saved with the SyncData system, like optional mod compatiblity in internal machines.
     *
     * @param tag     the CompoundTag to load data from
     * @param forDrop if the save is done for dropping the machine as an item.
     */
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {}

    public void loadCustomPersistedData(@NotNull CompoundTag tag) {}

    @Override
    public void scheduleRenderUpdate() {
        machine.scheduleRenderUpdate();
    }

    public void onTraitAttached() {}

    public void onMachineLoad() {}

    public void onMachineUnLoad() {}

    public void onMachineDestroyed() {}

    public void onMachineNeighborChanged(Block neighborBlock, BlockPos neighborPos, boolean isMoving) {}
}
