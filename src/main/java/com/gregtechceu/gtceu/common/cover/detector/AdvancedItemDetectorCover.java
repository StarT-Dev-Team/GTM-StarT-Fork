package com.gregtechceu.gtceu.common.cover.detector;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandlers;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AdvancedItemDetectorCover extends ItemDetectorCover implements IUICover {

    private static final int DEFAULT_MIN = 64;
    private static final int DEFAULT_MAX = 512;

    @Persisted
    @Getter
    private int minValue, maxValue;

    @Persisted
    @DescSynced
    @Getter
    @Setter
    private boolean isLatched;
    @Persisted
    @DescSynced
    @Getter
    protected final FilterHandler<ItemStack, ItemFilter> filterHandler;
    @Persisted
    @DescSynced
    @Getter
    @Setter
    private boolean isStrongSignal;
    @Persisted
    @DescSynced
    @Getter
    private int ticksPerCycle;

    public AdvancedItemDetectorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);

        this.minValue = DEFAULT_MIN;
        this.maxValue = DEFAULT_MAX;
        this.ticksPerCycle = ConfigHolder.INSTANCE.machines.coverDefaultTicksPerCycle;

        filterHandler = FilterHandlers.item(this);
    }

    @Override
    public List<ItemStack> getAdditionalDrops() {
        var list = super.getAdditionalDrops();
        if (!filterHandler.getFilterItem().isEmpty()) {
            list.add(filterHandler.getFilterItem());
        }
        return list;
    }

    @Override
    protected void update() {
        if (this.coverHolder.getOffsetTimer() % ticksPerCycle != 0)
            return;

        ItemFilter filter = filterHandler.getFilter();
        IItemHandler handler = getItemHandler();
        if (handler == null)
            return;

        int storedItems = 0;

        for (int i = 0; i < handler.getSlots(); i++) {
            if (filter.test(handler.getStackInSlot(i)))
                storedItems += handler.getStackInSlot(i).getCount();
        }

        var output = 0;
        if (isLatched) {
            output = RedstoneUtil.computeLatchedRedstoneBetweenValues(storedItems, maxValue, minValue,
                    isInverted(), redstoneSignalOutput);
        } else {
            output = RedstoneUtil.computeRedstoneBetweenValues(storedItems, maxValue, minValue, isInverted());
        }
        if (isStrongSignal) {
            setRedstoneSignalOutput(output);
            setRedstoneDirectSignalOutput(output);
        } else {
            setRedstoneSignalOutput(output);
            setRedstoneDirectSignalOutput(0);
        }
    }

    public void setMinValue(int minValue) {
        this.minValue = Mth.clamp(minValue, 0, maxValue - 1);
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = Math.max(maxValue, 0);
    }

    public void setTicksPerCycle(int ticksPerCycle) {
        this.ticksPerCycle = Mth.clamp(ticksPerCycle, ConfigHolder.INSTANCE.machines.coverMinTicksPerCycle,
                ticksPerCycle);
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public Component getCoverUITitle() {
        return Component.translatable("cover.advanced_item_detector.label");
    }

    @Override
    public UIElement createUIWidget() {
        var group = new UIElement().layout(layout -> layout.minWidth(176).paddingAll(5).gapAll(3));
        group.addChild(new Label().setText(Component.translatable("cover.advanced_item_detector.label")));

        var toggles = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3));
        toggles.addChild(new ToggleButton(GuiTextures.INVERT_REDSTONE_BUTTON, this::isInverted, this::setInverted)
                .isMultiLang()
                .setTooltipText("cover.advanced_item_detector.invert"));
        toggles.addChild(new ToggleButton(GuiTextures.BUTTON_LOCK, this::isLatched, this::setLatched)
                .setShouldUseBaseBackground()
                .isMultiLang()
                .setTooltipText("cover.advanced_detector.latch"));
        toggles.addChild(
                new ToggleButton(GuiTextures.BUTTON_REDSTONE_STRENGTH, this::isStrongSignal, this::setStrongSignal)
                        .isMultiLang()
                        .setTooltipText("cover.advanced_detector.signal"));
        group.addChild(toggles);

        var row0 = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3)
                .alignItems(AlignItems.CENTER));
        var label0 = new Label().setText(Component.translatable("cover.advanced_item_detector.min"));
        label0.layout(layout -> layout.width(65));
        var input0 = new IntInput(this::getMinValue, this::setMinValue);
        input0.layout(layout -> layout.flex(1).height(20));
        row0.addChildren(label0, input0);
        group.addChild(row0);

        var row1 = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3)
                .alignItems(AlignItems.CENTER));
        var label1 = new Label().setText(Component.translatable("cover.advanced_item_detector.max"));
        label1.layout(layout -> layout.width(65));
        var input1 = new IntInput(this::getMaxValue, this::setMaxValue);
        input1.layout(layout -> layout.flex(1).height(20));
        row1.addChildren(label1, input1);
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

        var filter = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3)
                .justifyContent(AlignContent.SPACE_BETWEEN));
        filter.addChild(filterHandler.createFilterConfigUI());
        filter.addChild(filterHandler.createFilterSlotUI());
        group.addChild(filter);

        return group;
    }
}
