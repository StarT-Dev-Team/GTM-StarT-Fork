package com.gregtechceu.gtceu.integration.ae2.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.list.AEFluidDisplayWidget;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class AEAmountDialog {

    private static final int MARGIN = 8;
    private static final int BUTTON_WIDTH = 36;
    private static final int ROW_HEIGHT = 16;
    private static final int GAP = 4;
    private static final int ICON_WIDTH = 20;
    private static final int FIELD_WIDTH = 4 * BUTTON_WIDTH + 3 * GAP;
    private static final int WIDTH = FIELD_WIDTH + 2 * MARGIN;

    private final AmountEntryModel model = new AmountEntryModel(
            Minecraft.getInstance().getLanguageManager().getJavaLocale());
    private final LongConsumer onConfirm;
    private final Dialog dialog = new Dialog();
    private final TextField amountField = new TextField();

    public static void open(UIElement anchor, @Nullable AEKey key, long initialAmount, LongConsumer onConfirm) {
        var modularUI = anchor.getModularUI();
        if (modularUI == null) return;
        var amountDialog = new AEAmountDialog(key, initialAmount, onConfirm);
        amountDialog.dialog.show(modularUI);
        amountDialog.amountField.focus();
    }

    private AEAmountDialog(@Nullable AEKey key, long initialAmount, LongConsumer onConfirm) {
        this.onConfirm = onConfirm;
        this.model.setKey(key);

        dialog.setClickOutsideClose(true);
        dialog.overlay.layout(layout -> layout.width(WIDTH));
        dialog.overlay.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));
        dialog.titleBar.layout(layout -> layout.paddingAll(0).paddingHorizontal(MARGIN).paddingTop(7));
        dialog.titleBar.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        dialog.contentContainer.layout(layout -> layout.paddingAll(MARGIN).gapAll(GAP));
        dialog.contentContainer.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));

        var title = new Label();
        title.setText(Component.translatable("gtceu.gui.amount_dialog.title"));
        title.textStyle(style -> style.textColor(0xFF404040).textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER));
        dialog.titleBar.addChild(title);

        var plusRow = createStepRow(1);
        var minusRow = createStepRow(-1);

        amountField.layout(layout -> layout.width(FIELD_WIDTH - ICON_WIDTH - 4).height(ROW_HEIGHT - 4));
        amountField.style(style -> style.backgroundTexture(GuiTextures.DISPLAY));
        amountField.setAnyString();
        amountField.setTextValidator(text -> text.length() <= 20);
        amountField.setText(model.format(initialAmount), false);
        amountField.setTextResponder(text -> updateError());
        amountField.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            var error = model.error(amountField.getValue());
            if (error != null) {
                event.hoverTooltips = new HoverTooltips(List.of(error), null, null, null);
            }
        });
        amountField.addEventListener(UIEvents.MOUSE_WHEEL, event -> {
            if (event.deltaY != 0) {
                amountField.setText(model.add(amountField.getValue(), event.deltaY > 0 ? 1 : -1));
            }
            event.stopPropagation();
        }, true);

        var fieldRow = new UIElement();
        fieldRow.layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .gapAll(2).width(FIELD_WIDTH).height(ROW_HEIGHT));
        fieldRow.addChildren(createIcon(key), amountField);

        var set = createButton(() -> Component.translatable("gtceu.gui.amount_dialog.set").getString(), FIELD_WIDTH);
        set.setOnClick(event -> confirm());

        dialog.contentContainer.addChildren(plusRow, fieldRow, minusRow, set);

        dialog.addEventListener(UIEvents.KEY_DOWN, event -> {
            if (event.keyCode == GLFW.GLFW_KEY_ENTER || event.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                confirm();
                event.stopPropagation();
            }
        }, true);

        updateError();
    }

    private static boolean isFineStep() {
        return Screen.hasShiftDown() || Screen.hasControlDown();
    }

    private UIElement createStepRow(int sign) {
        var row = new UIElement();
        row.layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(GAP));
        for (int i = 0; i < 4; i++) {
            int index = i;
            LongSupplier step = () -> sign *
                    (isFineStep() ? AmountEntryModel.STEPS_64 : AmountEntryModel.STEPS_1000)[index];
            var button = createButton(() -> (sign > 0 ? "+" : "-") + Math.abs(step.getAsLong()), BUTTON_WIDTH);
            button.setOnClick(event -> amountField.setText(model.add(amountField.getValue(), step.getAsLong())));
            row.addChild(button);
        }
        return row;
    }

    private static Button createButton(Supplier<String> text, int width) {
        var button = new Button();
        button.layout(layout -> layout.width(width).height(ROW_HEIGHT));
        button.buttonStyle(style -> style.baseTexture(GuiTextures.BUTTON)
                .hoverTexture(GuiTextures.BUTTON.copy().setColor(0xFFDDDDDD))
                .pressedTexture(GuiTextures.BUTTON.copy().setColor(0xFFBBBBBB)));
        button.text.setText(text.get(), false);
        button.addEventListener(UIEvents.TICK, event -> button.text.setText(text.get(), false));
        return button;
    }

    private static UIElement createIcon(@Nullable AEKey key) {
        if (key instanceof AEItemKey itemKey) {
            var slot = new ItemSlot();
            slot.style(style -> style.backgroundTexture(GuiTextures.SLOT));
            slot.setItem(itemKey.toStack(), false);
            return slot;
        }
        var fluid = new AEFluidDisplayWidget();
        if (key instanceof AEFluidKey) {
            fluid.setStack(new GenericStack(key, 1));
        } else {
            fluid.setStack(null);
        }
        return fluid;
    }

    private void updateError() {
        var error = model.error(amountField.getValue());
        amountField.textFieldStyle(style -> style.textColor(error != null ? 0xFFDF0000 : -1));
    }

    private void confirm() {
        model.external(amountField.getValue()).ifPresent(amount -> {
            onConfirm.accept(amount);
            dialog.close();
        });
    }
}
