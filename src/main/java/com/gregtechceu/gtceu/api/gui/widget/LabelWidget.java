package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberColor;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.layout.Align;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Configurable(name = "ldlib.gui.editor.register.widget.label", collapse = false)
@LDLRegister(name = "gtm_label", group = "widget.basic")
@Accessors(chain = true)
public class LabelWidget extends Widget implements IConfigurableWidget {

    @Nonnull
    @Getter
    protected Supplier<String> textSupplier;
    @Nullable
    @Getter
    protected Component component;
    @Configurable(name = "ldlib.gui.editor.name.text")
    private String lastTextValue;
    @Configurable(name = "ldlib.gui.editor.name.color")
    @NumberColor
    @Getter
    private int color;
    @Configurable(name = "ldlib.gui.editor.name.fontSize")
    @Getter
    @Setter
    public float fontSize = 9;
    @Configurable(name = "ldlib.gui.editor.name.isShadow")
    @Getter
    @Setter
    private boolean dropShadow;
    @Getter
    @Setter
    private Align textAlign = Align.CENTER;

    public LabelWidget(int x, int y, int width, int height, String text) {
        this(x, y, width, height, () -> text);
    }

    public LabelWidget(int x, int y, int width, int height, Component component) {
        super(new Position(x, y), new Size(width, height));
        this.lastTextValue = "";
        this.setDropShadow(true);
        this.setTextColor(-1);
        this.setComponent(component);
    }

    public LabelWidget(int x, int y, int width, int height, Supplier<String> text) {
        super(new Position(x, y), new Size(width, height));
        this.lastTextValue = "";
        this.setDropShadow(true);
        this.setTextColor(-1);
        this.setTextProvider(text);
    }

    @ConfigSetter(field = "lastTextValue")
    public void setText(String text) {
        this.textSupplier = () -> text;
        if (this.isRemote()) {
            this.lastTextValue = this.textSupplier.get();
        }
    }

    public void setTextProvider(Supplier<String> textProvider) {
        this.textSupplier = textProvider;
        if (this.isRemote()) {
            this.lastTextValue = this.textSupplier.get();
        }
    }

    public void setComponent(Component component) {
        this.component = component;
        if (this.isRemote()) {
            this.lastTextValue = component.getString();
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public LabelWidget setTextColor(int color) {
        this.color = color;
        if (this.component != null) {
            this.component = this.component.copy().withStyle(this.component.getStyle().withColor(color));
        }

        return this;
    }

    public void setColor(int color) {
        this.setTextColor(color);
    }

    public void writeInitialData(FriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        if (!this.isClientSideWidget) {
            if (this.component != null) {
                buffer.writeBoolean(true);
                buffer.writeComponent(this.component);
            } else {
                buffer.writeBoolean(false);
                this.lastTextValue = (String) this.textSupplier.get();
                buffer.writeUtf(this.lastTextValue);
            }
        } else {
            buffer.writeBoolean(false);
            buffer.writeUtf(this.lastTextValue);
        }
    }

    public void readInitialData(FriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        if (buffer.readBoolean()) {
            this.component = buffer.readComponent();
            this.lastTextValue = this.component.getString();
        } else {
            this.lastTextValue = buffer.readUtf();
        }
    }

    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!this.isClientSideWidget) {
            if (this.component != null) {
                String latest = this.component.getString();
                if (!latest.equals(this.lastTextValue)) {
                    this.lastTextValue = latest;
                    this.writeUpdateInfo(-2, (buffer) -> buffer.writeComponent(this.component));
                }
            }

            String latest = (String) this.textSupplier.get();
            if (!latest.equals(this.lastTextValue)) {
                this.lastTextValue = latest;
                this.writeUpdateInfo(-1, (buffer) -> buffer.writeUtf(this.lastTextValue));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id == -1) {
            this.lastTextValue = buffer.readUtf();
        } else if (id == -2) {
            this.component = buffer.readComponent();
            this.lastTextValue = this.component.getString();
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void updateScreen() {
        super.updateScreen();
        if (this.isClientSideWidget) {
            String latest = this.component == null ? (String) this.textSupplier.get() : this.component.getString();
            if (!latest.equals(this.lastTextValue)) {
                this.lastTextValue = latest;
            }
        }
    }

    private Vec2 calculateOffset(Font fontRenderer, Component component, float scale) {
        var width = fontRenderer.width(component) * scale;
        var height = fontRenderer.lineHeight * scale;
        var boxWidth = getSizeWidth();
        var boxHeight = getSizeHeight();

        return switch (textAlign) {
            case TOP_LEFT -> new Vec2(0, 0);
            case TOP_CENTER -> new Vec2((boxWidth - width) / 2f, 0);
            case TOP_RIGHT -> new Vec2(boxWidth - width, 0);
            case LEFT_CENTER -> new Vec2(0, (boxHeight - height) / 2f);
            case RIGHT_CENTER -> new Vec2(boxWidth - width, (boxHeight - height) / 2f);
            case BOTTOM_LEFT -> new Vec2(0, boxHeight - height);
            case BOTTOM_CENTER -> new Vec2((boxWidth - width) / 2f, boxHeight - height);
            case BOTTOM_RIGHT -> new Vec2(boxWidth - width, boxHeight - height);
            default -> new Vec2((boxWidth - width) / 2f, (boxHeight - height) / 2f);
        };
    }

    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        var fontRenderer = Minecraft.getInstance().font;
        var position = this.getPosition();

        var component = this.component != null ? this.component :
                Component.literal(LocalizationUtils.format(lastTextValue));
        var scale = fontSize / 9f;
        var off = calculateOffset(fontRenderer, component, scale);

        graphics.pose().pushPose();
        graphics.pose().translate(position.x + off.x, position.y + off.y, 0);
        graphics.pose().scale(scale, scale, 1);

        graphics.drawString(fontRenderer, component, 0, 0, color, dropShadow);

        graphics.pose().popPose();
    }

    public boolean handleDragging(Object dragging) {
        if (dragging instanceof String string) {
            this.setText(string);
            return true;
        } else {
            return IConfigurableWidget.super.handleDragging(dragging);
        }
    }

    public void setTextSupplier(@Nonnull Supplier<String> textSupplier) {
        if (textSupplier == null) {
            throw new NullPointerException("textSupplier is marked non-null but is null");
        } else {
            this.textSupplier = textSupplier;
        }
    }
}
