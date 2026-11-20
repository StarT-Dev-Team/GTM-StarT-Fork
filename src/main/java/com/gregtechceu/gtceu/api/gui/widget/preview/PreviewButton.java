package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class PreviewButton extends ButtonWidget {

    public static final int SIZE = 18;

    @Nullable
    private Runnable onRightClick;
    @Nullable
    private DoubleConsumer onWheel;
    @Nullable
    private Supplier<List<Component>> dynamicTooltip;

    public PreviewButton(int x, int y, Supplier<String> label, Consumer<ClickData> onPress) {
        super(x, y, SIZE, SIZE, new GuiTextureGroup(ColorPattern.T_GRAY.rectTexture(),
                new TextTexture("1").setSupplier(label)), onPress);
        setHoverBorderTexture(1, -1);
    }

    public PreviewButton onRightClick(Runnable action) {
        this.onRightClick = action;
        return this;
    }

    /**
     * @param action receives the raw wheel delta
     */
    public PreviewButton onWheel(DoubleConsumer action) {
        this.onWheel = action;
        return this;
    }

    public PreviewButton tooltip(String translationKey) {
        setHoverTooltips(Component.translatable(translationKey));
        return this;
    }

    public PreviewButton dynamicTooltip(Supplier<List<Component>> supplier) {
        this.dynamicTooltip = supplier;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (onRightClick != null && button == 1 && isMouseOverElement(mouseX, mouseY)) {
            onRightClick.run();
            playButtonClickSound();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (onWheel != null && isMouseOverElement(mouseX, mouseY)) {
            onWheel.accept(wheelDelta);
            return true;
        }
        return super.mouseWheelMove(mouseX, mouseY, wheelDelta);
    }

    @Override
    public List<Component> getTooltipTexts() {
        return dynamicTooltip != null ? dynamicTooltip.get() : super.getTooltipTexts();
    }

    @Override
    protected void drawTooltipTexts(int mouseX, int mouseY) {
        if (dynamicTooltip == null) {
            super.drawTooltipTexts(mouseX, mouseY);
            return;
        }
        List<Component> list = getTooltipTexts();
        this.tooltipTexts.clear();
        this.tooltipTexts.addAll(list);
        if (!list.isEmpty() && isMouseOverElement(mouseX, mouseY) &&
                getHoverElement(mouseX, mouseY) == this &&
                gui != null && gui.getModularUIGui() != null) {
            gui.getModularUIGui().setHoverTooltip(list, ItemStack.EMPTY, null, null);
        }
    }
}
