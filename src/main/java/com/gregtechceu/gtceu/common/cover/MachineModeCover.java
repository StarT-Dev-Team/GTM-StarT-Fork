package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.machine.MachineCoverContainer;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MachineModeCover extends CoverBehavior {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MachineModeCover.class,
            CoverBehavior.MANAGED_FIELD_HOLDER);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Persisted
    @DescSynced
    @Getter
    private int selectedRecipeType = 0;

    public MachineModeCover(@NotNull CoverDefinition definition, @NotNull ICoverable coverHolder,
                            @NotNull Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    @Override
    public boolean canAttach() {
        return super.canAttach() && getControllableMachine() != null;
    }

    @Override
    public void onNeighborChanged(net.minecraft.world.level.block.Block block, net.minecraft.core.BlockPos fromPos,
                                  boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);

        switchToMode();
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Nullable
    private IRecipeLogicMachine getControllableMachine() {
        if (coverHolder instanceof MachineCoverContainer container) {
            var machine = container.getMachine();

            if (machine instanceof IRecipeLogicMachine recipeLogicMachine) {
                return recipeLogicMachine;
            }
        }

        return null;
    }

    public void switchToMode() {
        IRecipeLogicMachine machine = getControllableMachine();

        if (machine == null) return;

        var level = coverHolder.getLevel();
        var sourcePos = coverHolder.getPos().relative(attachedSide);
        int signalStrength = level.getSignal(sourcePos, attachedSide);

        if (signalStrength == 0) return;

        GTRecipeType[] recipeTypes = machine.getRecipeTypes();

        if (recipeTypes.length <= 1) return;

        int modeIndex = signalStrength - 1;

        if (modeIndex >= 0 && modeIndex < recipeTypes.length) {
            machine.setActiveRecipeType(modeIndex);
            machine.getRecipeLogic().markLastRecipeDirty();

            this.selectedRecipeType = modeIndex;
        }
    }
}
