package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.util.Mth;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class ParallelHatchPartMachine extends TieredPartMachine implements IFancyUIMachine, IParallelHatch {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ParallelHatchPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    private static final int MIN_PARALLEL = 1;

    private final int maxParallel;

    @Persisted
    @Getter
    private int currentParallel;

    @Persisted
    @Getter
    private int minimumParallel;

    public ParallelHatchPartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
        maxParallel = (int) Math.pow(4, tier - GTValues.EV);
        currentParallel = maxParallel;
        minimumParallel = MIN_PARALLEL;
    }

    private void updateControllerRL() {
        for (IMultiController controller : getControllers()) {
            if (controller instanceof IRecipeLogicMachine rlm) {
                rlm.getRecipeLogic().markLastRecipeDirty();
                rlm.getRecipeLogic().updateTickSubscription();
            }
        }
    }

    public void setCurrentParallel(int parallelAmount) {
        var prevMin = minimumParallel;
        currentParallel = Mth.clamp(parallelAmount, MIN_PARALLEL, maxParallel);
        minimumParallel = Mth.clamp(prevMin, MIN_PARALLEL, currentParallel);
        updateControllerRL();
    }

    public void setMinimumParallel(int parallelAmount) {
        minimumParallel = Mth.clamp(parallelAmount, MIN_PARALLEL, currentParallel);
        updateControllerRL();
    }

    @Override
    public Widget createUIWidget() {
        var parallelAmountGroup = new WidgetGroup(0, 0, 100, 80);

        parallelAmountGroup.addWidget(new LabelWidget(-14, 4, "gtceu.gui.title.parallel_hatch.max_parallel"));
        parallelAmountGroup
                .addWidget(new IntInputWidget(new Position(0, 18), this::getCurrentParallel, this::setCurrentParallel)
                        .setMin(MIN_PARALLEL)
                        .setMax(maxParallel));

        parallelAmountGroup.addWidget(new LabelWidget(-10, 50, "gtceu.gui.title.parallel_hatch.min_parallel"));
        parallelAmountGroup
                .addWidget(new IntInputWidget(new Position(0, 64), this::getMinimumParallel, this::setMinimumParallel)
                        .setMin(MIN_PARALLEL)
                        .setMax(maxParallel));

        return parallelAmountGroup;
    }

    @Override
    @NotNull
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean canShared() {
        return false;
    }
}
