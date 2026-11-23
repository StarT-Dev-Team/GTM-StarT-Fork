package com.gregtechceu.gtceu.common.cover.detector;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.IEnergyInfoProvider;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.gui.widget.LongInput;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTMath;

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

import java.math.BigInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.gregtechceu.gtceu.utils.RedstoneUtil.computeLatchedRedstoneBetweenValues;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AdvancedEnergyDetectorCover extends EnergyDetectorCover implements IUICover {

    private static final int DEFAULT_MIN_PERCENT = 33;
    private static final int DEFAULT_MAX_PERCENT = 66;

    @Persisted
    @Getter
    @Setter
    public long minValue, maxValue;

    @Persisted
    @Getter
    private boolean usePercent;

    private LongInput minValueInput;
    private LongInput maxValueInput;

    @Persisted
    @DescSynced
    @Getter
    @Setter
    private boolean isStrongSignal;

    @Persisted
    @DescSynced
    @Getter
    private int ticksPerCycle;

    public AdvancedEnergyDetectorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        this.minValue = DEFAULT_MIN_PERCENT;
        this.maxValue = DEFAULT_MAX_PERCENT;
        this.ticksPerCycle = ConfigHolder.INSTANCE.machines.coverDefaultTicksPerCycle;
        this.usePercent = true;
    }

    public void setTicksPerCycle(int ticksPerCycle) {
        this.ticksPerCycle = Mth.clamp(ticksPerCycle, ConfigHolder.INSTANCE.machines.coverMinTicksPerCycle,
                ticksPerCycle);
    }

    @Override
    protected void update() {
        if (coverHolder.getOffsetTimer() % 20 != 0) return;

        IEnergyInfoProvider energyInfoProvider = getEnergyInfoProvider();
        if (energyInfoProvider == null) return;

        IEnergyInfoProvider.EnergyInfo energyInfo = energyInfoProvider.getEnergyInfo();
        boolean isBigInt = energyInfoProvider.supportsBigIntEnergyValues();

        var output = 0;
        if (isBigInt) {
            if (usePercent) {
                if (energyInfo.capacity().compareTo(BigInteger.ZERO) > 0) {
                    float ratio = GTMath.ratio(energyInfo.stored(), energyInfo.capacity());
                    output = computeLatchedRedstoneBetweenValues(ratio * 100, maxValue,
                            minValue, isInverted(), redstoneSignalOutput);
                } else {
                    output = isInverted() ? 15 : 0;
                }
            } else {
                output = computeLatchedRedstoneBetweenValues(energyInfo.stored(),
                        BigInteger.valueOf(this.maxValue), BigInteger.valueOf(this.minValue),
                        isInverted(), redstoneSignalOutput);
            }
        } else {
            if (usePercent) {
                if (energyInfo.capacity().longValue() > 0) {
                    float ratio = energyInfo.stored().floatValue() / energyInfo.capacity().floatValue();
                    output = computeLatchedRedstoneBetweenValues(ratio * 100, maxValue,
                            minValue, isInverted(), redstoneSignalOutput);
                } else {
                    output = isInverted() ? 15 : 0;
                }
            } else {
                output = computeLatchedRedstoneBetweenValues(energyInfo.stored().longValue(),
                        this.maxValue, this.minValue,
                        isInverted(), redstoneSignalOutput);
            }
        }
        if (isStrongSignal) {
            setRedstoneSignalOutput(output);
            setRedstoneDirectSignalOutput(output);
        } else {
            setRedstoneSignalOutput(output);
            setRedstoneDirectSignalOutput(0);
        }
    }

    public void setUsePercent(boolean usePercent) {
        var wasPercent = this.usePercent;
        this.usePercent = usePercent;

        initializeMinMaxInputs(wasPercent);
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public UIElement createUIWidget() {
        var group = new UIElement().layout(layout -> layout.minWidth(176).paddingAll(5).gapAll(3));
        group.addChild(new Label().setText(Component.translatable("cover.advanced_energy_detector.label")));

        var toggles = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3));
        toggles.addChild(new ToggleButton(GuiTextures.INVERT_REDSTONE_BUTTON, this::isInverted, this::setInverted)
                .isMultiLang()
                .setTooltipText("cover.advanced_energy_detector.invert"));
        toggles.addChild(
                new ToggleButton(GuiTextures.BUTTON_REDSTONE_STRENGTH, this::isStrongSignal, this::setStrongSignal)
                        .isMultiLang()
                        .setTooltipText("cover.advanced_detector.signal"));
        toggles.addChild(
                new ToggleButton(GuiTextures.ENERGY_DETECTOR_COVER_MODE_BUTTON, this::isUsePercent, this::setUsePercent)
                        .isMultiLang()
                        .setTooltipText("cover.advanced_energy_detector.use_percent"));
        group.addChild(toggles);

        var row0 = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3)
                .alignItems(AlignItems.CENTER));
        var label0 = new Label().setText(Component.translatable("cover.advanced_energy_detector.min"));
        label0.layout(layout -> layout.width(65));
        this.minValueInput = new LongInput(this::getMinValue, this::setMinValue);
        this.minValueInput.layout(layout -> layout.flex(1).height(20));
        row0.addChildren(label0, this.minValueInput);
        group.addChild(row0);

        var row1 = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3)
                .alignItems(AlignItems.CENTER));
        var label1 = new Label().setText(Component.translatable("cover.advanced_energy_detector.max"));
        label1.layout(layout -> layout.width(65));
        this.maxValueInput = new LongInput(this::getMaxValue, this::setMaxValue);
        this.maxValueInput.layout(layout -> layout.flex(1).height(20));
        row1.addChildren(label1, this.maxValueInput);
        group.addChild(row1);

        var row2 = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3)
                .alignItems(AlignItems.CENTER));
        var label2 = new Label().setText(Component.translatable("cover.advanced_detector.ticks_per_cycle"));
        label2.layout(layout -> layout.width(65));
        label2.style(style -> style.tooltips("cover.advanced_detector.ticks_per_cycle.tooltip"));
        var input2 = new IntInput(this::getTicksPerCycle, this::setTicksPerCycle);
        input2.setMin(ConfigHolder.INSTANCE.machines.coverMinTicksPerCycle);
        input2.layout(layout -> layout.flex(1).height(20));
        row2.addChildren(label2, input2);
        group.addChild(row2);

        initializeMinMaxInputs(usePercent);

        return group;
    }

    private void initializeMinMaxInputs(boolean wasPercent) {
        if (GTCEu.isClientThread() || minValueInput == null || maxValueInput == null)
            return;

        long energyCapacity;
        try {
            energyCapacity = getEnergyInfoProvider().getEnergyInfo().capacity().longValueExact();
        } catch (ArithmeticException e) {
            energyCapacity = Long.MAX_VALUE;
        }

        minValueInput.setMin(0L);
        maxValueInput.setMin(0L);

        if (usePercent) {
            // This needs to be before setting the maximum, because otherwise the value would be limited to 100 EU
            // before converting to percent.
            if (!wasPercent) {
                minValueInput.setValue(GTMath.clamp((long) (((double) minValue / energyCapacity) * 100), 0, 100));
                maxValueInput.setValue(GTMath.clamp((long) (((double) maxValue / energyCapacity) * 100), 0, 100));
            }

            minValueInput.setMax(100L);
            maxValueInput.setMax(100L);
        } else {
            minValueInput.setMax(energyCapacity);
            maxValueInput.setMax(energyCapacity);

            // This needs to be after setting the maximum, because otherwise the converted value would be
            // limited to 100.
            if (wasPercent) {
                minValueInput.setValue(
                        GTMath.clamp((long) Math.ceil((minValue / 100.0) * energyCapacity), 0, energyCapacity));
                maxValueInput.setValue(
                        GTMath.clamp((long) Math.ceil((maxValue / 100.0) * energyCapacity), 0, energyCapacity));
            }
        }
    }
}
