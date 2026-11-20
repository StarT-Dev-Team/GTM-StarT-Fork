package com.gregtechceu.gtceu.integration.ae2.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEConfigDisplayWidget;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEConfigSlotWidget;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot;
import com.gregtechceu.gtceu.integration.ae2.slot.PortAEInventory;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class ConfigWidget extends WidgetGroup {

    protected static final int UPDATE_ID = 1000;

    protected final IConfigurableSlot[] config;
    protected final IConfigurableSlot[] cached;
    protected final IConfigurableSlot[] displayList;
    protected final Int2ObjectMap<IConfigurableSlot> changeMap = new Int2ObjectOpenHashMap<>();

    @Getter
    protected final boolean isStocking;

    private final BooleanSupplier autoPull;
    private final Predicate<GenericStack> hasStackInConfig;
    @Nullable
    private final Class<? extends AEKey> keyType;

    private static final int SLOTS_PER_ROW = 8;
    private static final int ROWS_PER_PAGE = 3;
    private static final int SLOTS_PER_PAGE = SLOTS_PER_ROW * ROWS_PER_PAGE;
    private static final int ROW_HEIGHT = 18 * 2 + 2;
    private static final int PAGER_HEIGHT = 14;
    private static final int PAGER_BUTTON_SIZE = 12;

    private final List<List<Widget>> pageWidgets = new ArrayList<>();
    private final int pageCount;
    private int currentPage;

    public ConfigWidget(int x, int y, IConfigurableSlot[] config, boolean isStocking, BooleanSupplier autoPull,
                        Predicate<GenericStack> hasStackInConfig, @Nullable Class<? extends AEKey> keyType) {
        super(new Position(x, y),
                new Size(Math.min(config.length, SLOTS_PER_ROW) * 18, calculateHeight(config.length)));

        this.pageCount = calculatePageCount(config.length);
        this.config = config;
        this.isStocking = isStocking;
        this.autoPull = autoPull;
        this.hasStackInConfig = hasStackInConfig;
        this.keyType = keyType;
        this.cached = new IConfigurableSlot[config.length];
        this.displayList = new IConfigurableSlot[config.length];

        for (int index = 0; index < this.pageCount; index++) {
            this.pageWidgets.add(new ArrayList<>());
        }

        for (int index = 0; index < config.length; index++) {
            this.displayList[index] = new PortAEInventory.Snapshot();
            this.cached[index] = new PortAEInventory.Snapshot();

            int line = index / SLOTS_PER_ROW;
            int slotX = (index - line * SLOTS_PER_ROW) * 18;
            int slotY = line % ROWS_PER_PAGE * ROW_HEIGHT;
            Widget view = new AEConfigDisplayWidget(slotX, slotY, this, index);
            Widget slot = new AEConfigSlotWidget(slotX, slotY, this, index);

            addWidget(view);
            addWidget(slot);

            this.pageWidgets.get(index / SLOTS_PER_PAGE).add(view);
            this.pageWidgets.get(index / SLOTS_PER_PAGE).add(slot);
        }

        if (this.pageCount > 1) {
            createPageControls();
            showPage(0);
        }
    }

    private static int calculatePageCount(int slots) {
        return (slots + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE;
    }

    private static int calculateHeight(int slots) {
        int rows = Math.min((slots + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW, ROWS_PER_PAGE);

        return rows * ROW_HEIGHT + (calculatePageCount(slots) > 1 ? PAGER_HEIGHT : 0);
    }

    private void createPageControls() {
        int width = getSize().width;
        int y = getSize().height - PAGER_HEIGHT + 2;

        addWidget(createPageButton(0, y, GuiTextures.BUTTON_PAGE_LEFT, -1));
        addWidget(createPageButton(width - PAGER_BUTTON_SIZE, y, GuiTextures.BUTTON_PAGE_RIGHT, 1));
        addWidget(new ImageWidget(PAGER_BUTTON_SIZE, y, width - 2 * PAGER_BUTTON_SIZE, PAGER_BUTTON_SIZE,
                new TextTexture("").setSupplier(() -> this.currentPage + 1 + " / " + this.pageCount)
                        .setColor(0xFF404040)
                        .setDropShadow(false))
                .setClientSideWidget());
    }

    private Widget createPageButton(int x, int y, IGuiTexture icon, int direction) {
        return new ButtonWidget(x, y, PAGER_BUTTON_SIZE, PAGER_BUTTON_SIZE,
                new GuiTextureGroup(GuiTextures.BUTTON, icon),
                clickData -> showPage(this.currentPage + direction)).setClientSideWidget();
    }

    private void showPage(int newPage) {
        this.currentPage = Math.floorMod(newPage, this.pageCount);

        for (int index = 0; index < this.pageWidgets.size(); index++) {
            boolean visible = index == this.currentPage;

            for (Widget widget : this.pageWidgets.get(index)) {
                widget.setVisible(visible);
            }
        }
    }

    public boolean hasStackInConfig(GenericStack stack) {
        return this.hasStackInConfig.test(stack);
    }

    public boolean isAutoPull() {
        return this.autoPull.getAsBoolean();
    }

    public boolean acceptsKey(AEKey key) {
        return this.keyType == null || this.keyType.isInstance(key);
    }

    public boolean acceptsItems() {
        return this.keyType == null || this.keyType == AEItemKey.class;
    }

    public boolean acceptsFluids() {
        return this.keyType == null || this.keyType == AEFluidKey.class;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        this.changeMap.clear();

        for (int index = 0; index < this.config.length; index++) {
            IConfigurableSlot newSlot = this.config[index];
            IConfigurableSlot oldSlot = this.cached[index];
            GenericStack newConfig = newSlot.getConfig();
            GenericStack newStock = newSlot.getStock();
            GenericStack oldConfig = oldSlot.getConfig();
            GenericStack oldStock = oldSlot.getStock();

            if (areAEStackCountsNotEqual(newConfig, oldConfig) || areAEStackCountsNotEqual(newStock, oldStock)) {
                this.changeMap.put(index, newSlot.copy());
                this.cached[index] = this.config[index].copy();
                this.gui.holder.markAsDirty();
            }
        }

        if (!this.changeMap.isEmpty()) {
            this.writeUpdateInfo(UPDATE_ID, buf -> {
                buf.writeVarInt(this.changeMap.size());

                for (int index : this.changeMap.keySet()) {
                    GenericStack changedConfig = this.changeMap.get(index).getConfig();
                    GenericStack changedStock = this.changeMap.get(index).getStock();

                    buf.writeVarInt(index);

                    if (changedConfig != null) {
                        buf.writeBoolean(true);
                        GenericStack.writeBuffer(changedConfig, buf);
                    } else {
                        buf.writeBoolean(false);
                    }

                    if (changedStock != null) {
                        buf.writeBoolean(true);
                        GenericStack.writeBuffer(changedStock, buf);
                    } else {
                        buf.writeBoolean(false);
                    }
                }
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        super.readUpdateInfo(id, buffer);

        if (id == UPDATE_ID) {
            int size = buffer.readVarInt();

            for (int i = 0; i < size; i++) {
                int index = buffer.readVarInt();
                IConfigurableSlot slot = this.displayList[index];

                slot.setConfig(buffer.readBoolean() ? GenericStack.readBuffer(buffer) : null);
                slot.setStock(buffer.readBoolean() ? GenericStack.readBuffer(buffer) : null);
            }
        }
    }

    public final IConfigurableSlot getConfig(int index) {
        return this.config[index];
    }

    public final IConfigurableSlot getDisplay(int index) {
        return this.displayList[index];
    }

    protected final boolean areAEStackCountsNotEqual(GenericStack first, GenericStack second) {
        if (second == first) return false;
        if (first != null && second != null) return first.amount() != second.amount() || !first.what().matches(second);

        return true;
    }
}
