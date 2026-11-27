package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.ClickData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ComponentPanel extends UIElement {

    private static final String CLICK_MESSAGE = "component_click";

    private Consumer<List<Component>> textSupplier;
    private List<Component> lines = new ArrayList<>();
    @Nullable
    private BiConsumer<String, ClickData> clickHandler;
    private int maxWidthLimit = 0;

    public ComponentPanel() {
        this(lines -> {});
    }

    public ComponentPanel(Consumer<List<Component>> textSupplier) {
        this.textSupplier = textSupplier;
        addChild(new BindableValue<Tag>().bind(DataBindingBuilder.tagS2C(this::collect)
                .remoteSetter(this::receive).build()));
        onMessage(CLICK_MESSAGE, this::onClickMessage);
    }

    public ComponentPanel textSupplier(Consumer<List<Component>> textSupplier) {
        this.textSupplier = textSupplier;
        return this;
    }

    public static MutableComponent withButton(Component text, String componentData) {
        var copy = text.copy();
        return copy.setStyle(copy.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                "@!" + componentData)));
    }

    public ComponentPanel setMaxWidthLimit(int maxWidthLimit) {
        this.maxWidthLimit = maxWidthLimit;
        layout(layout -> layout.maxWidth(maxWidthLimit));
        rebuild();
        return this;
    }

    public ComponentPanel clickHandler(BiConsumer<String, ClickData> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }

    private Tag collect() {
        var buffer = new ArrayList<Component>();
        textSupplier.accept(buffer);
        var list = new ListTag();
        for (var component : buffer) {
            list.add(StringTag.valueOf(Component.Serializer.toJson(component)));
        }
        return list;
    }

    private void receive(Tag tag) {
        var result = new ArrayList<Component>();
        if (tag instanceof ListTag list) {
            for (var entry : list) {
                var component = Component.Serializer.fromJson(entry.getAsString());
                if (component != null) result.add(component);
            }
        }
        lines = result;
        rebuild();
    }

    private void rebuild() {
        for (var child : new ArrayList<>(getChildren())) {
            if (child instanceof Label) removeChild(child);
        }
        for (var line : lines) {
            var label = new Label().setText(line);
            label.layout(layout -> layout.heightAuto());
            label.textStyle(style -> style.adaptiveHeight(true).textWrap(TextWrap.WRAP));
            if (maxWidthLimit > 0) {
                label.layout(layout -> layout.width(maxWidthLimit));
            }
            var data = findClickData(line);
            if (data != null) {
                label.addEventListener(UIEvents.MOUSE_DOWN, event -> sendClick(data, event));
            }
            addChild(label);
        }
    }

    @Nullable
    private static String findClickData(Component component) {
        Optional<String> found = component.visit((Style style, String text) -> {
            var click = style.getClickEvent();
            if (click != null && click.getAction() == ClickEvent.Action.OPEN_URL &&
                    click.getValue().startsWith("@!")) {
                return Optional.of(click.getValue().substring(2));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found.orElse(null);
    }

    private void sendClick(String data, UIEvent event) {
        var clickData = new ClickData();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        clickData.writeToBuf(buffer);
        var bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        var tag = new CompoundTag();
        tag.putString("data", data);
        tag.putByteArray("click", bytes);
        sendMessage(CLICK_MESSAGE, tag);
    }

    private void onClickMessage(CompoundTag tag) {
        if (clickHandler == null) return;
        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(tag.getByteArray("click")));
        clickHandler.accept(tag.getString("data"), ClickData.readFromBuf(buffer));
    }
}
