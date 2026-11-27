package com.gregtechceu.gtceu.integration.ae2.gui.widget.list;

import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class AEListGridWidget extends UIElement {

    protected final KeyStorage list;
    private final int slotAmountY;
    private final Object2LongMap<AEKey> cached = new Object2LongOpenHashMap<>();
    private ListTag cachedTag = new ListTag();
    protected final List<GenericStack> displayList = new ArrayList<>();
    private final ScrollerView scroller = new ScrollerView();
    private final List<AEGenericDisplayWidget> rows = new ArrayList<>();

    public AEListGridWidget(int slotsY, KeyStorage internalList) {
        this.list = internalList;
        this.slotAmountY = slotsY;

        layout(layout -> layout.width(18 * 9).height(slotsY * 18));
        scroller.layout(layout -> layout.widthPercent(100).heightPercent(100));
        scroller.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL).verticalScrollDisplay(ScrollDisplay.AUTO));
        scroller.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        scroller.viewPort(viewPort -> {
            viewPort.layout(layout -> layout.paddingAll(0));
            viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        });
        scroller.verticalScroller(bar -> bar.layout(layout -> layout.width(4).marginLeft(1)));
        addChild(scroller);
        addChild(new BindableValue<Tag>().bind(DataBindingBuilder.tagS2C(this::collect)
                .remoteSetter(this::receive).build()));
        updateRows();
    }

    public GenericStack getAt(int index) {
        return index >= 0 && index < displayList.size() ? displayList.get(index) : null;
    }

    private Tag collect() {
        if (list == null) return cachedTag;
        if (!cached.equals(list.storage)) {
            cached.clear();
            cached.putAll(list.storage);
            cachedTag = list.serializeNBT();
        }
        return cachedTag;
    }

    private void receive(Tag tag) {
        var incoming = new LinkedHashMap<AEKey, Long>();
        if (tag instanceof ListTag entries) {
            for (int i = 0; i < entries.size(); i++) {
                var entry = entries.getCompound(i);
                var key = AEKey.fromTagGeneric(entry.getCompound("key"));
                var amount = entry.getLong("value");
                if (key != null && amount > 0) {
                    incoming.put(key, amount);
                }
            }
        }
        var result = new ArrayList<GenericStack>();
        for (var old : displayList) {
            var amount = incoming.remove(old.what());
            if (amount != null) {
                result.add(new GenericStack(old.what(), amount));
            }
        }
        incoming.forEach((key, amount) -> result.add(new GenericStack(key, amount)));
        displayList.clear();
        displayList.addAll(result);
        updateRows();
    }

    private void updateRows() {
        var required = Math.max(slotAmountY, displayList.size());
        while (rows.size() < required) {
            var row = new AEGenericDisplayWidget();
            rows.add(row);
            scroller.addScrollViewChild(row);
        }
        while (rows.size() > required) {
            scroller.removeScrollViewChild(rows.remove(rows.size() - 1));
        }
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).update(getAt(i));
        }
    }

    public static class Item extends AEListGridWidget {

        public Item(int slotsY, KeyStorage internalList) {
            super(slotsY, internalList);
        }
    }

    public static class Fluid extends AEListGridWidget {

        public Fluid(int slotsY, KeyStorage internalList) {
            super(slotsY, internalList);
        }
    }

    public static class Generic extends AEListGridWidget {

        public Generic(int slotsY, KeyStorage internalList) {
            super(slotsY, internalList);
        }
    }
}
