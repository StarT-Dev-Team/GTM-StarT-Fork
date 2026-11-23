package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidType;

import dev.vfyjxf.taffy.style.TaffyPosition;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PumpHatchPartMachine extends FluidHatchPartMachine {

    public PumpHatchPartMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, 0, IO.OUT, FluidType.BUCKET_VOLUME, 1, args);
    }

    @Override
    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        return super.createTank(initialCapacity, slots)
                .setFilter(fluidStack -> fluidStack.getFluid().is(GTMaterials.Water.getFluidTag()));
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var area = new UIElement();
        area.layout(layout -> layout.height(82));

        var display = new UIElement();
        display.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(16).width(81).height(55));
        display.style(style -> style.backgroundTexture(GuiTextures.DISPLAY));

        var amountTitle = new Label();
        amountTitle.setText(Component.translatable("gtceu.gui.fluid_amount"));
        amountTitle.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(11).top(20));

        var amount = new Label();
        amount.textStyle(style -> style.textColor(-1).textShadow(true));
        amount.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal(String.valueOf(tank.getFluidInTank(0).getAmount()))).build());
        amount.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(11).top(30));

        var title = new Label();
        title.setText(Component.translatable(getBlockState().getBlock().getDescriptionId()));
        title.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(6).top(6));

        var tankSlot = new FluidSlot().bind(tank.getStorages()[0], 0);
        tankSlot.setAllowClickFilled(true);
        tankSlot.setAllowClickDrained(io.support(IO.IN));
        tankSlot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(90).top(35));
        tankSlot.style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));

        var toggle = new ToggleButton(GuiTextures.BUTTON_FLUID_OUTPUT, this::isWorkingEnabled,
                this::setWorkingEnabled)
                .setShouldUseBaseBackground()
                .setTooltipText("gtceu.gui.fluid_auto_input.tooltip");
        toggle.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(53));

        area.addChildren(display, amountTitle, amount, title, tankSlot, toggle);

        var root = UITemplate.panel();
        root.layout(layout -> layout.width(176).paddingAll(0).paddingBottom(7));
        root.addChildren(area, UITemplate.playerInventory());
        return UITemplate.modularUI(root, entityPlayer);
    }

    // By returning false here, we don't allow shift-clicking
    // with a screwdriver to swap the IO.
    @Override
    public boolean swapIO() {
        return false;
    }
}
