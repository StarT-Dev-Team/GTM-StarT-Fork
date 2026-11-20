package com.gregtechceu.gtceu.integration.ae2.gui.widget.list;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.integration.ae2.utils.AEUtil;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;

import java.util.Locale;

public class AEGenericDisplayWidget extends WidgetGroup {

    private final AEListGridWidget gridWidget;
    private final int index;
    private final CustomItemStackHandler itemHandler = new CustomItemStackHandler(1);
    private final CustomFluidTank fluidTank = new CustomFluidTank(1);
    private final SlotWidget itemSlot;
    private final TankWidget fluidSlot;

    public AEGenericDisplayWidget(int x, int y, AEListGridWidget gridWidget, int index) {
        super(x, y, 18 + 140, 18);

        this.gridWidget = gridWidget;
        this.index = index;

        setClientSideWidget();

        addWidget(new ImageWidget(18, 0, 140, 18, GuiTextures.NUMBER_BACKGROUND));

        this.itemSlot = (SlotWidget) new SlotWidget(this.itemHandler, 0, 0, 0, false, false).setClientSideWidget();
        this.fluidSlot = new TankWidget(this.fluidTank, 0, 0, 18, 18, false, false).setShowAmount(false)
                .setBackground(GuiTextures.SLOT).setClientSideWidget();

        addWidget(this.itemSlot);
        addWidget(this.fluidSlot);
        addWidget(new LabelWidget(22, 5, this::getAmountText).setTextColor(-1));

        this.fluidSlot.setVisible(false);
    }

    private String getAmountText() {
        GenericStack stack = this.gridWidget.getAt(this.index);

        if (stack == null) return "";

        Locale locale = Minecraft.getInstance().getLanguageManager().getJavaLocale();

        return String.format(locale, "x%,d", stack.amount());
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        GenericStack stack = this.gridWidget.getAt(this.index);

        boolean isFluid = stack != null && stack.what() instanceof AEFluidKey;

        this.itemSlot.setVisible(!isFluid);
        this.fluidSlot.setVisible(isFluid);

        if (isFluid) {
            this.fluidTank.setCapacity(Math.max(1, GTMath.saturatedCast(stack.amount())));
            this.fluidTank.setFluid(AEUtil.toFluidStack(stack));
        } else {
            this.fluidTank.setFluid(FluidStack.EMPTY);
        }

        ItemStack displayed = stack != null && stack.what() instanceof AEItemKey key ? key.toStack() : ItemStack.EMPTY;

        if (!ItemStack.matches(this.itemHandler.getStackInSlot(0), displayed)) {
            this.itemHandler.setStackInSlot(0, displayed);
        }
    }
}
