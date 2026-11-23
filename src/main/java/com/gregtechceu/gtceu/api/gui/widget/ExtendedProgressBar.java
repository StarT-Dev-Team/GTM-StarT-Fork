package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.api.gui.texture.UVSpriteTexture;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ExtendedProgressBar extends ProgressTextureBar {

    private List<Component> serverTooltips = new ArrayList<>();

    public ExtendedProgressBar(SpriteTexture sheet, FillDirection direction, int width, int height) {
        super(UVSpriteTexture.of(sheet, 0, 0, 1, 0.5f), UVSpriteTexture.of(sheet, 0, 0.5f, 1, 0.5f), direction, width,
                height);
        addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            var tips = new ArrayList<>(getStyle().tooltips().asList());
            tips.addAll(serverTooltips);
            if (!tips.isEmpty()) {
                event.hoverTooltips = new HoverTooltips(tips, null, null, null);
            }
        });
    }

    public ExtendedProgressBar setServerTooltipSupplier(Consumer<List<Component>> supplier) {
        addChild(new BindableValue<Tag>().bind(DataBindingBuilder.tagS2C(() -> {
            var buffer = new ArrayList<Component>();
            supplier.accept(buffer);
            var list = new ListTag();
            for (var component : buffer) {
                list.add(StringTag.valueOf(Component.Serializer.toJson(component)));
            }
            return list;
        }).remoteSetter(tag -> {
            var tooltips = new ArrayList<Component>();
            if (tag instanceof ListTag list) {
                for (var entry : list) {
                    var component = Component.Serializer.fromJson(entry.getAsString());
                    if (component != null) tooltips.add(component);
                }
            }
            serverTooltips = tooltips;
        }).build()));
        return this;
    }
}
