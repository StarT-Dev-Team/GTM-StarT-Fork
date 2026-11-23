package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraftforge.items.IItemHandlerModifiable;

import dev.vfyjxf.taffy.style.TaffyPosition;

import java.util.function.BooleanSupplier;

public class BlockableItemSlot extends ItemSlot {

    private static final int OVERLAY_COLOR = 0x80404040;

    private BooleanSupplier isBlocked = () -> false;
    private final UIElement overlay = new UIElement();

    public BlockableItemSlot() {
        overlay.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).right(0).bottom(0))
                .style(style -> style.backgroundTexture(new ColorRectTexture(OVERLAY_COLOR)))
                .setDisplay(false);
        overlay.setAllowHitTest(false);
        addChild(overlay);
        addEventListener(UIEvents.TICK, this::onTick);
    }

    public BlockableItemSlot setIsBlocked(BooleanSupplier isBlocked) {
        this.isBlocked = isBlocked;
        return this;
    }

    @Override
    public ItemSlot bind(IItemHandlerModifiable itemHandler, int index) {
        return bind(new ItemHandlerSlot(itemHandler, index)
                .setCanPlace(stack -> !isBlocked.getAsBoolean())
                .setCanTake(player -> !isBlocked.getAsBoolean()));
    }

    private void onTick(UIEvent event) {
        overlay.setDisplay(isBlocked.getAsBoolean());
    }

    @Override
    protected void onMouseDown(UIEvent event) {
        if (isBlocked.getAsBoolean()) {
            event.stopPropagation();
            return;
        }
        super.onMouseDown(event);
    }
}
