package com.gregtechceu.gtceu.common.cover.ender;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandlers;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.misc.virtualregistry.EntryTypes;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEnderRegistry;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry;
import com.gregtechceu.gtceu.api.misc.virtualregistry.entries.VirtualTank;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class EnderFluidLinkCover extends AbstractEnderLinkCover<VirtualTank> {

    public static final int TRANSFER_RATE = 8000; // mB/t

    @Persisted
    @DescSynced
    protected VirtualTank visualTank;

    @Getter
    @Persisted
    @DescSynced
    protected final FilterHandler<FluidStack, FluidFilter> filterHandler;
    protected int mBLeftToTransferLastSecond;

    public EnderFluidLinkCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        this.mBLeftToTransferLastSecond = TRANSFER_RATE * 20;
        filterHandler = FilterHandlers.fluid(this);
        if (!isRemote()) visualTank = VirtualEnderRegistry.getInstance()
                .getOrCreateEntry(getOwner(), EntryTypes.ENDER_FLUID, getChannelName());
    }

    @Override
    protected VirtualTank getEntry() {
        return visualTank;
    }

    @Override
    protected void setEntry(VirtualEntry entry) {
        visualTank = (VirtualTank) entry;
    }

    @Override
    public boolean canAttach() {
        return FluidUtil.getFluidHandler(coverHolder.getLevel(), coverHolder.getPos(), attachedSide).isPresent();
    }

    @Override
    protected EntryTypes<VirtualTank> getEntryType() {
        return EntryTypes.ENDER_FLUID;
    }

    @Override
    protected String identifier() {
        return "EFLink#";
    }

    @Override
    protected void transfer() {
        long timer = coverHolder.getOffsetTimer();
        if (mBLeftToTransferLastSecond > 0) {
            int platformTransferredFluid = doTransferFluids(mBLeftToTransferLastSecond);
            this.mBLeftToTransferLastSecond -= platformTransferredFluid;
        }

        if (timer % 20 == 0) {
            this.mBLeftToTransferLastSecond = TRANSFER_RATE * 20;
        }
    }

    protected @Nullable IFluidHandlerModifiable getOwnFluidHandler() {
        return coverHolder.getFluidHandlerCap(attachedSide, false);
    }

    private int doTransferFluids(int platformTransferLimit) {
        var ownFluidHandler = getOwnFluidHandler();

        if (ownFluidHandler != null) {
            return switch (io) {
                case IN -> GTTransferUtils.transferFluidsFiltered(ownFluidHandler, visualTank.getFluidTank(),
                        filterHandler.getFilter(), platformTransferLimit);
                case OUT -> GTTransferUtils.transferFluidsFiltered(visualTank.getFluidTank(), ownFluidHandler,
                        filterHandler.getFilter(), platformTransferLimit);
                default -> 0;
            };

        }
        return 0;
    }

    //////////////////////////////////////
    // ************ GUI ************ //
    //////////////////////////////////////

    @Override
    protected UIElement addVirtualEntryWidget(Supplier<? extends VirtualEntry> entry, boolean interactive) {
        var group = new UIElement();
        var slot = new FluidSlot();
        slot.style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));
        slot.layout(layout -> layout.width(18).height(18));
        if (interactive) {
            slot.bind(new EntryHandler(entry), 0);
        } else {
            slot.setAllowClickFilled(false);
            slot.setAllowClickDrained(false);
            slot.addEventListener(UIEvents.TICK,
                    event -> slot.setValue(((VirtualTank) entry.get()).getFluidTank().getFluid(), false));
        }
        group.addChild(slot);
        return group;
    }

    private static class EntryHandler implements IFluidHandler {

        private final Supplier<? extends VirtualEntry> entry;

        private EntryHandler(Supplier<? extends VirtualEntry> entry) {
            this.entry = entry;
        }

        private IFluidHandler delegate() {
            return ((VirtualTank) entry.get()).getFluidTank();
        }

        @Override
        public int getTanks() {
            return delegate().getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return delegate().getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return delegate().getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return delegate().isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return delegate().fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return delegate().drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return delegate().drain(maxDrain, action);
        }
    }

    @NotNull
    @Override
    protected String getUITitle() {
        return "cover.ender_fluid_link.title";
    }
}
