package com.gregtechceu.gtceu.integration.ae2.gui.widget.list;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.client.TooltipsHandler;
import com.gregtechceu.gtceu.integration.ae2.utils.AEUtil;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.integration.xei.emi.LDLibEMIPlugin;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;
import com.lowdragmc.lowdraglib2.integration.xei.rei.LDLibREIPlugin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.stacks.GenericStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import lombok.Getter;
import me.shedaniel.rei.api.common.util.EntryStacks;
import mezz.jei.api.forge.ForgeTypes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class AEFluidDisplayWidget extends UIElement {

    private static final ColorRectTexture HOVER = new ColorRectTexture(0x80FFFFFF);

    @Getter
    private FluidStack fluid = FluidStack.EMPTY;
    private long amount;

    public AEFluidDisplayWidget() {
        layout(layout -> layout.width(18).height(18).paddingAll(1));
        style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        if (LDLib2.isClient() && !LDLib2.isServer()) {
            if (LDLib2.isJeiLoaded()) {
                JEISupport.clickableIngredient(this);
            }
            if (LDLib2.isReiLoaded()) {
                REISupport.focusedStack(this);
            }
            if (LDLib2.isEmiLoaded()) {
                EMISupport.stackProvider(this);
            }
        }
    }

    public AEFluidDisplayWidget setStack(@Nullable GenericStack stack) {
        this.fluid = stack == null ? FluidStack.EMPTY : AEUtil.toFluidStack(stack);
        this.amount = stack == null ? 0 : stack.amount();
        return this;
    }

    private void onHoverTooltips(UIEvent event) {
        if (fluid.isEmpty()) return;
        var tooltips = new ArrayList<Component>();
        tooltips.add(fluid.getDisplayName());
        tooltips.add(Component.literal(String.format("%,d mB", amount)));
        TooltipsHandler.appendFluidTooltips(fluid, tooltips::add, TooltipFlag.NORMAL);
        event.hoverTooltips = new HoverTooltips(tooltips, null, null, null);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        var x = getContentX();
        var y = getContentY();
        var width = getContentWidth();
        var height = getContentHeight();
        if (!fluid.isEmpty()) {
            DrawerHelper.drawFluidForGui(guiContext.graphics, fluid, x, y, width, height, -1);
        }
        if (isHover()) {
            guiContext.drawTexture(HOVER, x, y, width, height);
        }
    }

    private static class JEISupport {

        static void clickableIngredient(AEFluidDisplayWidget widget) {
            LDLibJEIPlugin.clickableIngredient(widget, () -> {
                if (widget.fluid.isEmpty()) return null;
                return LDLibJEIPlugin.createTypedIngredient(ForgeTypes.FLUID_STACK, widget.fluid).orElse(null);
            });
        }
    }

    private static class REISupport {

        static void focusedStack(AEFluidDisplayWidget widget) {
            LDLibREIPlugin.focusedStack(widget, () -> {
                if (widget.fluid.isEmpty()) return null;
                return EntryStacks.of(FluidStackHooksForge.fromForge(widget.fluid));
            });
        }
    }

    private static class EMISupport {

        static void stackProvider(AEFluidDisplayWidget widget) {
            LDLibEMIPlugin.stackProvider(widget, () -> {
                if (widget.fluid.isEmpty()) return null;
                var fluid = widget.fluid;
                return new EmiStackInteraction(EmiStack.of(fluid.getFluid(), fluid.getTag(), fluid.getAmount()),
                        null, false);
            });
        }
    }
}
