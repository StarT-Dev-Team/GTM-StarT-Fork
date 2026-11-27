package com.gregtechceu.gtceu.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.PropertyFluidFilter;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiblockTankMachine extends MultiblockControllerMachine implements IFancyUIMachine {

    @Persisted
    @Getter
    @NotNull
    private final NotifiableFluidTank tank;

    public MultiblockTankMachine(IMachineBlockEntity holder, int capacity, @Nullable PropertyFluidFilter filter,
                                 Object... args) {
        super(holder);

        this.tank = createTank(capacity, filter, args);
    }

    protected NotifiableFluidTank createTank(int capacity, @Nullable PropertyFluidFilter filter, Object... args) {
        var fluidTank = new NotifiableFluidTank(this, 1, capacity, IO.BOTH);

        if (filter != null)
            fluidTank.setFilter(filter);

        return fluidTank;
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        var superResult = super.onUse(state, world, pos, player, hand, hit);

        if (superResult != InteractionResult.PASS) return superResult;
        if (!isFormed()) return InteractionResult.FAIL;

        return InteractionResult.PASS; // Otherwise let MetaMachineBlock.use() open the UI
    }

    @Override
    @Nullable
    public IFluidHandlerModifiable getFluidHandlerCap(@Nullable Direction side, boolean useCoverCapability) {
        if (isFormed) {
            return super.getFluidHandlerCap(side, useCoverCapability);
        }
        return null;
    }

    /////////////////////////////////////
    // *********** GUI ***********//
    /////////////////////////////////////

    @Override
    public UIElement createUIWidget() {
        var group = new UIElement();
        group.layout(layout -> layout.width(90).height(63));
        group.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

        var display = new UIElement();
        display.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(4).top(4).width(82).height(55));
        display.style(style -> style.backgroundTexture(GuiTextures.DISPLAY));

        var title = new Label();
        title.setText(Component.translatable("gtceu.gui.fluid_amount"));
        title.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(8).top(8));

        var amount = new Label();
        amount.textStyle(style -> style.textColor(-1).textShadow(true));
        amount.bind(DataBindingBuilder.componentS2C(() -> Component.literal(getFluidLabel())).build());
        amount.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(8).top(18));

        var slot = new FluidSlot().bind(tank.getStorages()[0], 0);
        slot.setAllowClickFilled(true);
        slot.setAllowClickDrained(true);
        slot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(68).top(23));
        slot.style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));

        group.addChildren(display, title, amount, slot);
        return group;
    }

    private String getFluidLabel() {
        return String.valueOf(tank.getFluidInTank(0).getAmount());
    }

    //////////////////////////////////////
    // ***** LDLib SyncData ******//
    //////////////////////////////////////
}
