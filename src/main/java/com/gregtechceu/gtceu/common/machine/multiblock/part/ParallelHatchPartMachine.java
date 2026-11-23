package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import dev.vfyjxf.taffy.style.AlignItems;
import lombok.Getter;

public class ParallelHatchPartMachine extends TieredPartMachine implements IFancyUIMachine, IParallelHatch {

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
    public UIElement createUIWidget() {
        var group = new UIElement();
        group.layout(layout -> layout.alignItems(AlignItems.CENTER).gapAll(4).paddingAll(4));

        var maxLabel = new Label();
        maxLabel.setText(Component.translatable("gtceu.gui.title.parallel_hatch.max_parallel"));
        var maxInput = new IntInput(this::getCurrentParallel, this::setCurrentParallel);
        maxInput.setMin(MIN_PARALLEL);
        maxInput.setMax(maxParallel);

        var minLabel = new Label();
        minLabel.setText(Component.translatable("gtceu.gui.title.parallel_hatch.min_parallel"));
        var minInput = new IntInput(this::getMinimumParallel, this::setMinimumParallel);
        minInput.setMin(MIN_PARALLEL);
        minInput.setMax(maxParallel);

        group.addChildren(maxLabel, maxInput, minLabel, minInput);
        return group;
    }

    @Override
    public boolean canShared() {
        return false;
    }
}
