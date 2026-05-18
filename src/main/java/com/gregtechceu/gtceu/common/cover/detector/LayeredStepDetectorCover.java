package com.gregtechceu.gtceu.common.cover.detector;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget;
import com.gregtechceu.gtceu.common.machine.trait.LayeredRecipeLogic;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextBoxWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LayeredStepDetectorCover extends DetectorCover implements IUICover {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            LayeredStepDetectorCover.class, DetectorCover.MANAGED_FIELD_HOLDER);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Persisted
    @DescSynced
    @Getter
    @Setter
    private boolean isStrongSignal;
    @Persisted
    @DescSynced
    @Getter
    private int ticksPerCycle;

    public LayeredStepDetectorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        this.ticksPerCycle = ConfigHolder.INSTANCE.machines.coverDefaultTicksPerCycle;
    }

    @Override
    public boolean canAttach() {
        return super.canAttach() && GTCapabilityHelper.getRecipeLogic(coverHolder.getLevel(), coverHolder.getPos(),
                attachedSide) instanceof LayeredRecipeLogic;
    }

    public void setTicksPerCycle(int ticksPerCycle) {
        this.ticksPerCycle = Mth.clamp(ticksPerCycle, ConfigHolder.INSTANCE.machines.coverMinTicksPerCycle,
                ticksPerCycle);
    }

    @Override
    protected void update() {
        if (this.coverHolder.getOffsetTimer() % ticksPerCycle != 0) {
            return;
        }

        var recipeLogic = GTCapabilityHelper.getRecipeLogic(coverHolder.getLevel(), coverHolder.getPos(), attachedSide);
        var layeredStep = recipeLogic instanceof LayeredRecipeLogic layeredRecipeLogic ?
                layeredRecipeLogic.getCoverRedstoneOutput() : 0;
        var output = isInverted() ? 15 - layeredStep : layeredStep;

        if (isStrongSignal) {
            setRedstoneSignalOutput(output);
            setRedstoneDirectSignalOutput(output);
        } else {
            setRedstoneSignalOutput(output);
            setRedstoneDirectSignalOutput(0);
        }
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 176, 80);
        group.addWidget(new LabelWidget(10, 5, "cover.layered_step_detector.label"));

        group.addWidget(new TextBoxWidget(10, 55, 65,
                List.of(LocalizationUtils.format("cover.advanced_detector.ticks_per_cycle")))
                .setHoverTooltips(Component.translatable("cover.advanced_detector.ticks_per_cycle.tooltip")));

        group.addWidget(new IntInputWidget(80, 50, 176 - 80 - 10, 20, this::getTicksPerCycle, this::setTicksPerCycle)
                .setMin(ConfigHolder.INSTANCE.machines.coverMinTicksPerCycle));

        group.addWidget(new ToggleButtonWidget(
                9, 20, 20, 20,
                GuiTextures.INVERT_REDSTONE_BUTTON, this::isInverted, this::setInverted)
                .isMultiLang()
                .setTooltipText("cover.layered_step_detector.invert"));

        group.addWidget(new ToggleButtonWidget(31, 21, 18, 18,
                GuiTextures.BUTTON_REDSTONE_STRENGTH, this::isStrongSignal, this::setStrongSignal)
                .isMultiLang()
                .setTooltipText("cover.advanced_detector.signal"));

        return group;
    }
}
