package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public class BlockableTankWidget extends TankWidget {

    private static final int OVERLAY_COLOR = 0x80404040;

    @Setter
    @Accessors(chain = true)
    private BooleanSupplier isBlocked = () -> false;

    public BlockableTankWidget() {
        super();
    }

    public BlockableTankWidget(IFluidHandler fluidTank, int x, int y, boolean allowClickContainerFilling,
                               boolean allowClickContainerEmptying) {
        super(fluidTank, x, y, allowClickContainerFilling, allowClickContainerEmptying);
    }

    public BlockableTankWidget(@Nullable IFluidHandler fluidTank, int x, int y, int width, int height,
                               boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
        super(fluidTank, x, y, width, height, allowClickContainerFilling, allowClickContainerEmptying);
    }

    public BlockableTankWidget(IFluidHandler fluidHandler, int tank, int x, int y, boolean allowClickContainerFilling,
                               boolean allowClickContainerEmptying) {
        super(fluidHandler, tank, x, y, allowClickContainerFilling, allowClickContainerEmptying);
    }

    public BlockableTankWidget(@Nullable IFluidHandler fluidHandler, int tank, int x, int y, int width, int height,
                               boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
        super(fluidHandler, tank, x, y, width, height, allowClickContainerFilling, allowClickContainerEmptying);
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        if (isBlocked.getAsBoolean()) {
            Position pos = getPosition();
            Size size = getSize();
            RenderSystem.disableDepthTest();
            RenderSystem.colorMask(true, true, true, false);
            graphics.fill(pos.getX() + 1, pos.getY() + 1, pos.getX() + 1 + size.getWidth() - 2,
                    pos.getY() + 1 + size.getHeight() - 2, OVERLAY_COLOR);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
        }
    }

    @Override
    public boolean isMouseOverElement(double mouseX, double mouseY) {
        // prevent slot removal and hover highlighting when slot is blocked
        return super.isMouseOverElement(mouseX, mouseY) && !isBlocked.getAsBoolean();
    }
}
