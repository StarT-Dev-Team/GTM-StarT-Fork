package com.gregtechceu.gtceu.integration.ae2.gui.widget.slot;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.ConfigWidget;
import com.gregtechceu.gtceu.integration.ae2.utils.AEUtil;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.TextFormattingUtil;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.Nullable;

public class AEConfigDisplayWidget extends WidgetGroup {

    private final ConfigWidget parentWidget;
    private final int index;
    private final StackRow configRow;
    private final StackRow stockRow;
    private Boolean autoPull;
    private IGuiTexture configBackground = IGuiTexture.EMPTY;

    public AEConfigDisplayWidget(int x, int y, ConfigWidget parentWidget, int index) {
        super(x, y, 18, 36);

        this.parentWidget = parentWidget;
        this.index = index;

        setClientSideWidget();

        this.configRow = new StackRow(0);
        this.stockRow = new StackRow(18);

        this.configRow.addTo(this);
        this.stockRow.addTo(this);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        var slot = this.parentWidget.getDisplay(this.index);

        boolean autoPull = this.parentWidget.isAutoPull();

        if (this.autoPull == null || this.autoPull != autoPull) {
            this.autoPull = autoPull;
            this.configBackground = new GuiTextureGroup(
                    autoPull ? GuiTextures.SLOT_DARK : GuiTextures.SLOT,
                    autoPull ? GuiTextures.CONFIG_ARROW : GuiTextures.CONFIG_ARROW_DARK);
        }

        this.configRow.update(slot.getConfig(), !this.parentWidget.isStocking(), this.configBackground);
        this.stockRow.update(slot.getStock(), true, GuiTextures.SLOT_DARK);
    }

    private static class StackRow {

        private final CustomItemStackHandler itemHandler = new CustomItemStackHandler(1);
        private final CustomFluidTank fluidTank = new CustomFluidTank(1);
        private final SlotWidget itemSlot;
        private final TankWidget fluidSlot;
        private final ImageWidget amountImage;
        private String amountText = "";

        StackRow(int y) {
            this.itemSlot = (SlotWidget) new SlotWidget(this.itemHandler, 0, 0, y, false, false)
                    .setDrawHoverTips(false).setClientSideWidget();
            this.fluidSlot = new TankWidget(this.fluidTank, 0, y, 18, 18, false, false).setShowAmount(false)
                    .setDrawHoverTips(false).setClientSideWidget();

            this.amountImage = new ImageWidget(-8, y + 7, 34, 18,
                    new TextTexture("").setType(TextTexture.TextType.RIGHT).setColor(-1).setDropShadow(true)
                            .setSupplier(() -> this.amountText).scale(0.5f));

            this.itemSlot.setActive(false);
            this.fluidSlot.setActive(false);
            this.fluidSlot.setVisible(false);
        }

        void addTo(WidgetGroup group) {
            group.addWidget(this.itemSlot);
            group.addWidget(this.fluidSlot);
            group.addWidget(this.amountImage);
        }

        void update(@Nullable GenericStack stack, boolean showAmount, IGuiTexture background) {
            boolean isFluid = stack != null && stack.what() instanceof AEFluidKey;

            this.itemSlot.setVisible(!isFluid);
            this.fluidSlot.setVisible(isFluid);
            this.itemSlot.setBackgroundTexture(background);
            this.fluidSlot.setBackground(background);

            this.amountText = "";

            if (isFluid) {
                this.fluidTank.setCapacity(Math.max(1, GTMath.saturatedCast(stack.amount())));
                this.fluidTank.setFluid(AEUtil.toFluidStack(stack));

                if (showAmount) {
                    this.amountText = FormattingUtil.formatNumberReadable(stack.amount(), true,
                            FormattingUtil.DECIMAL_FORMAT_0F, "B");
                }
            } else {
                this.fluidTank.setFluid(FluidStack.EMPTY);
            }

            ItemStack displayed = ItemStack.EMPTY;

            if (stack != null && stack.what() instanceof AEItemKey key) {
                displayed = key.toStack();

                if (showAmount) {
                    this.amountText = TextFormattingUtil.formatLongToCompactString(stack.amount(), 4);
                }
            }

            if (!ItemStack.matches(this.itemHandler.getStackInSlot(0), displayed)) {
                this.itemHandler.setStackInSlot(0, displayed);
            }
        }
    }
}
