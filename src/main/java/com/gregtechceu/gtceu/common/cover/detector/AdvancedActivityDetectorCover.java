package com.gregtechceu.gtceu.common.cover.detector;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.RedstoneUtil;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AdvancedActivityDetectorCover extends ActivityDetectorCover implements IUICover {

    @Persisted
    @DescSynced
    @Getter
    @Setter
    private boolean isStrongSignal;

    @Persisted
    @DescSynced
    @Getter
    private int ticksPerCycle;

    public AdvancedActivityDetectorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        this.ticksPerCycle = ConfigHolder.INSTANCE.machines.coverDefaultTicksPerCycle;
    }

    public void setTicksPerCycle(int ticksPerCycle) {
        this.ticksPerCycle = Mth.clamp(ticksPerCycle, ConfigHolder.INSTANCE.machines.coverMinTicksPerCycle,
                ticksPerCycle);
    }

    @Override
    protected void update() {
        if (this.coverHolder.getOffsetTimer() % ticksPerCycle != 0)
            return;

        var workable = GTCapabilityHelper.getWorkable(coverHolder.getLevel(), coverHolder.getPos(), attachedSide);
        if (workable == null)
            return;

        int output;

        // nonstandard logic for handling off state
        if (!workable.isWorkingEnabled() || !workable.isActive() || workable.getMaxProgress() == 0) {
            output = isInverted() ? 15 : 0;
        } else {
            output = RedstoneUtil.computeRedstoneValue(workable.getProgress(), workable.getMaxProgress(),
                    isInverted());
        }

        if (isStrongSignal) {
            setRedstoneSignalOutput(output);
            setRedstoneDirectSignalOutput(output);
        } else {
            setRedstoneSignalOutput(output);
            setRedstoneDirectSignalOutput(0);
        }
    }

    @Override
    public Component getCoverUITitle() {
        return Component.translatable("cover.advanced_activity_detector.label");
    }

    @Override
    public UIElement createUIWidget() {
        var group = new UIElement().layout(layout -> layout.minWidth(176).paddingAll(5).gapAll(3));
        group.addChild(new Label().setText(Component.translatable("cover.advanced_activity_detector.label")));

        var toggles = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3));
        toggles.addChild(new ToggleButton(GuiTextures.INVERT_REDSTONE_BUTTON, this::isInverted, this::setInverted)
                .isMultiLang()
                .setTooltipText("cover.advanced_activity_detector.invert"));
        toggles.addChild(
                new ToggleButton(GuiTextures.BUTTON_REDSTONE_STRENGTH, this::isStrongSignal, this::setStrongSignal)
                        .isMultiLang()
                        .setTooltipText("cover.advanced_detector.signal"));
        group.addChild(toggles);

        var row0 = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3)
                .alignItems(AlignItems.CENTER));
        var label0 = new Label().setText(Component.translatable("cover.advanced_detector.ticks_per_cycle"));
        label0.layout(layout -> layout.width(65));
        label0.style(style -> style.tooltips("cover.advanced_detector.ticks_per_cycle.tooltip"));
        var input0 = new IntInput(this::getTicksPerCycle, this::setTicksPerCycle);
        input0.setMin(ConfigHolder.INSTANCE.machines.coverMinTicksPerCycle);
        input0.layout(layout -> layout.flex(1).height(20));
        row0.addChildren(label0, input0);
        group.addChild(row0);

        return group;
    }
}
