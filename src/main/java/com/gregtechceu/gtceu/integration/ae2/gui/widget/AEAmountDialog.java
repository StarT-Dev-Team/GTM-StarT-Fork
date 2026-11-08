package com.gregtechceu.gtceu.integration.ae2.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.integration.ae2.utils.AEUtil;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class AEAmountDialog extends DialogWidget {

    private static final int MARGIN = 8;
    private static final int BUTTON_WIDTH = 36;
    private static final int ROW_HEIGHT = 16;
    private static final int GAP = 4;
    private static final int ICON_WIDTH = 20;
    private static final int FIELD_WIDTH = 4 * BUTTON_WIDTH + 3 * GAP;
    private static final int WIDTH = FIELD_WIDTH + 2 * MARGIN;
    private static final int PLUS_Y = 20;
    private static final int FIELD_Y = PLUS_Y + ROW_HEIGHT + GAP;
    private static final int MINUS_Y = FIELD_Y + ROW_HEIGHT + GAP;
    private static final int SET_Y = MINUS_Y + ROW_HEIGHT + GAP;
    private static final int HEIGHT = SET_Y + ROW_HEIGHT + MARGIN;

    private final AmountEntryModel model = new AmountEntryModel(
            Minecraft.getInstance().getLanguageManager().getJavaLocale());
    private final LongConsumer onConfirm;
    private final WidgetGroup panel;
    private final TextFieldWidget amountField;

    public AEAmountDialog(WidgetGroup root, @Nullable AEKey key, long initialAmount, LongConsumer onConfirm) {
        super(root, true);

        this.onConfirm = onConfirm;

        this.model.setKey(key);

        this.panel = new WidgetGroup((getSize().width - WIDTH) / 2, (getSize().height - HEIGHT) / 2, WIDTH, HEIGHT);

        this.panel.setBackground(GuiTextures.BACKGROUND);

        addWidget(this.panel);

        this.panel.addWidget(new LabelWidget(MARGIN, 7, "gtceu.gui.amount_dialog.title").setTextColor(0x404040)
                .setDropShadow(false));

        for (int i = 0; i < 4; i++) {
            int x = MARGIN + i * (BUTTON_WIDTH + GAP);

            this.panel.addWidget(createStepButton(x, PLUS_Y, i, 1));
            this.panel.addWidget(createStepButton(x, MINUS_Y, i, -1));
        }

        this.panel.addWidget(createIcon(key));

        this.amountField = new TextFieldWidget(MARGIN + ICON_WIDTH + 2, FIELD_Y + 2, FIELD_WIDTH - ICON_WIDTH - 4,
                ROW_HEIGHT - 4, null, text -> updateError());

        this.amountField.setClientSideWidget();
        this.amountField.setBackground(GuiTextures.DISPLAY);
        this.amountField.setMaxStringLength(20);
        this.amountField.setCurrentString(this.model.format(initialAmount));

        this.panel.addWidget(this.amountField);

        this.amountField.setFocus(true);

        this.panel.addWidget(new ButtonWidget(MARGIN, SET_Y, FIELD_WIDTH, ROW_HEIGHT,
                createButtonTexture(() -> Component.translatable("gtceu.gui.amount_dialog.set").getString()),
                clickData -> confirm()));
    }

    private static boolean isFineStep() {
        return Screen.hasShiftDown() || Screen.hasControlDown();
    }

    private static IGuiTexture createButtonTexture(Supplier<String> text) {
        return new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("").setSupplier(text));
    }

    private ButtonWidget createStepButton(int x, int y, int index, int sign) {
        LongSupplier step = () -> sign *
                (isFineStep() ? AmountEntryModel.STEPS_64 : AmountEntryModel.STEPS_1000)[index];

        return new ButtonWidget(x, y, BUTTON_WIDTH, ROW_HEIGHT,
                createButtonTexture(() -> (sign > 0 ? "+" : "-") + Math.abs(step.getAsLong())),
                clickData -> this.amountField.setCurrentString(
                        this.model.add(this.amountField.getCurrentString(), step.getAsLong())));
    }

    private static Widget createIcon(@Nullable AEKey key) {
        if (key instanceof AEItemKey itemKey) {
            var itemHandler = new CustomItemStackHandler(itemKey.toStack());

            return new SlotWidget(itemHandler, 0, MARGIN, FIELD_Y - 1, false, false).setClientSideWidget();
        }

        var fluidStack = key instanceof AEFluidKey ? AEUtil.toFluidStack(new GenericStack(key, 1)) : FluidStack.EMPTY;
        var fluidTank = new CustomFluidTank(fluidStack);

        return new TankWidget(fluidTank, MARGIN, FIELD_Y - 1, 18, 18, false, false).setShowAmount(false)
                .setBackground(GuiTextures.SLOT).setClientSideWidget();
    }

    private void updateError() {
        Component error = this.model.error(this.amountField.getCurrentString());

        this.amountField.setTextColor(error != null ? 0xFFDF0000 : -1);
        this.amountField.setHoverTooltips(error == null ? List.of() : List.of(error));
    }

    private void confirm() {
        this.model.external(this.amountField.getCurrentString()).ifPresent(amount -> {
            this.onConfirm.accept(amount);

            close();
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.panel.isMouseOverElement(mouseX, mouseY)) {
            close();

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
        } else {
            super.keyPressed(keyCode, scanCode, modifiers);
        }

        return true;
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (wheelDelta != 0 && this.amountField.isMouseOverElement(mouseX, mouseY)) {
            this.amountField.setCurrentString(
                    this.model.add(this.amountField.getCurrentString(), wheelDelta > 0 ? 1 : -1));
        }

        return true;
    }
}
