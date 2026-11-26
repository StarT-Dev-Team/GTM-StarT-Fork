package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;
import com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerDelegate;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.common.cover.data.FilterMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidFilterCover extends CoverBehavior implements IUICover {

    protected FluidFilter fluidFilter;
    @Persisted
    @DescSynced
    @Getter
    protected FilterMode filterMode = FilterMode.FILTER_INSERT;
    private FilteredFluidHandlerWrapper fluidFilterWrapper;
    @Persisted
    @Setter
    @Getter
    protected ManualIOMode allowFlow = ManualIOMode.DISABLED;

    public FluidFilterCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    public void setFilterMode(FilterMode filterMode) {
        this.filterMode = filterMode;
        coverHolder.markDirty();
    }

    @Override
    public boolean canAttach() {
        return super.canAttach() && coverHolder.getFluidHandlerCap(attachedSide, false) != null;
    }

    public FluidFilter getFluidFilter() {
        if (fluidFilter == null) {
            fluidFilter = FluidFilter.loadFilter(attachItem);
        }
        return fluidFilter;
    }

    @Override
    public @Nullable IFluidHandlerModifiable getFluidHandlerCap(@Nullable IFluidHandlerModifiable defaultValue) {
        if (defaultValue == null) {
            return null;
        }

        if (fluidFilterWrapper == null || fluidFilterWrapper.delegate != defaultValue) {
            this.fluidFilterWrapper = new FilteredFluidHandlerWrapper(defaultValue);
        }

        return fluidFilterWrapper;
    }

    @Override
    public Component getCoverUITitle() {
        return Component.translatable(attachItem.getDescriptionId());
    }

    @Override
    public UIElement createUIWidget() {
        var group = new UIElement().layout(layout -> layout.minWidth(168).paddingAll(4).gapAll(3)
                .alignItems(AlignItems.CENTER));
        group.addChild(new Label().setText(Component.translatable(attachItem.getDescriptionId())));

        var body = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(8));
        var selectors = new UIElement().layout(layout -> layout.gapAll(2));
        selectors.addChild(new EnumSelector<>(FilterMode.VALUES, () -> filterMode, this::setFilterMode)
                .layout(layout -> layout.width(18).height(18)));
        selectors.addChild(new EnumSelector<>(ManualIOMode.VALUES, () -> allowFlow, this::setAllowFlow)
                .layout(layout -> layout.width(18).height(18)));
        body.addChildren(selectors, getFluidFilter().openConfigurator());
        group.addChild(body);
        return group;
    }

    private class FilteredFluidHandlerWrapper extends FluidHandlerDelegate {

        public FilteredFluidHandlerWrapper(IFluidHandlerModifiable delegate) {
            super(delegate);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if ((filterMode == FilterMode.FILTER_EXTRACT) && allowFlow == ManualIOMode.UNFILTERED)
                return super.fill(resource, action);
            if (filterMode != FilterMode.FILTER_EXTRACT && getFluidFilter().test(resource))
                return super.fill(resource, action);
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if ((filterMode == FilterMode.FILTER_INSERT) && allowFlow == ManualIOMode.UNFILTERED)
                return super.drain(resource, action);
            if (filterMode != FilterMode.FILTER_INSERT && getFluidFilter().test(resource))
                return super.drain(resource, action);
            return FluidStack.EMPTY;
        }
    }
}
