package com.gregtechceu.gtceu.common.cover.ender;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.texture.UVSpriteTexture;
import com.gregtechceu.gtceu.api.gui.widget.ColorBlock;
import com.gregtechceu.gtceu.api.gui.widget.ConfirmTextInput;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.MachineCoverContainer;
import com.gregtechceu.gtceu.api.misc.virtualregistry.EntryTypes;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEnderRegistry;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry;
import com.gregtechceu.gtceu.api.misc.virtualregistry.entries.VirtualTank;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@SuppressWarnings("SameParameterValue")
public abstract class AbstractEnderLinkCover<T extends VirtualEntry> extends CoverBehavior
                                            implements IUICover, IControllable {

    public static final Pattern COLOR_INPUT_PATTERN = Pattern.compile("^[0-9a-fA-F]{0,8}$");

    protected final ConditionalSubscriptionHandler subscriptionHandler;

    @Persisted
    @DescSynced
    protected String colorStr = VirtualEntry.DEFAULT_COLOR;
    @Getter
    @Persisted
    @DescSynced
    protected Permissions permission = Permissions.PUBLIC;
    @Persisted
    @Getter
    protected boolean isWorkingEnabled = true;
    @Getter
    @Persisted
    @DescSynced
    protected ManualIOMode manualIOMode = ManualIOMode.DISABLED;
    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected IO io = IO.OUT;
    protected VirtualEntryWidget virtualEntryWidget;
    @DescSynced
    boolean isAnyChanged = false;

    public AbstractEnderLinkCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        subscriptionHandler = new ConditionalSubscriptionHandler(coverHolder, this::update, this::isSubscriptionActive);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        subscriptionHandler.initialize(coverHolder.getLevel());
    }

    @Override
    public abstract boolean canAttach();

    @Override
    public void onAttached(@NotNull ItemStack itemStack, @Nullable ServerPlayer player) {
        super.onAttached(itemStack, player);
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        subscriptionHandler.unsubscribe();
        if (!isRemote()) {
            VirtualEnderRegistry.getInstance()
                    .deleteEntryIf(getOwner(), getEntryType(), getChannelName(), VirtualEntry::canRemove);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        subscriptionHandler.unsubscribe();
        if (!isRemote()) {
            VirtualEnderRegistry.getInstance()
                    .deleteEntryIf(getOwner(), getEntryType(), getChannelName(), VirtualEntry::canRemove);
        }
    }

    @Override
    public void onUIClosed() {
        virtualEntryWidget = null;
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        if (this.isWorkingEnabled != isWorkingAllowed) {
            this.isWorkingEnabled = isWorkingAllowed;
            subscriptionHandler.updateSubscription();
        }
    }

    @Override
    public Component getCoverUITitle() {
        return Component.translatable(getUITitle());
    }

    @Override
    public UIElement createUIWidget() {
        virtualEntryWidget = new VirtualEntryWidget(this);
        return virtualEntryWidget;
    }

    public void setIo(IO io) {
        if (io == IO.IN || io == IO.OUT) {
            this.io = io;
            subscriptionHandler.updateSubscription();
        }
    }

    public UUID getOwner() {
        if (permission == Permissions.PRIVATE && coverHolder instanceof MachineCoverContainer mcc) {
            var owner = mcc.getMachine().getOwner();
            return owner != null ? owner.getPlayerUUID() : null;
        }
        return null;
    }

    protected boolean isSubscriptionActive() {
        return isWorkingEnabled();
    }

    protected abstract String identifier();

    protected abstract VirtualEntry getEntry();

    protected abstract void setEntry(VirtualEntry entry);

    protected final String getChannelName() {
        return identifier() + this.colorStr;
    }

    protected void setChannelName(String name) {
        if (isRemote()) return;
        VirtualEnderRegistry.getInstance().deleteEntryIf(getOwner(), getEntryType(), getChannelName(),
                VirtualEntry::canRemove);
        this.colorStr = name;
        setVirtualEntry();
    }

    protected final String getChannelName(VirtualEntry entry) {
        return identifier() + entry.getColorStr();
    }

    protected void setPermission(Permissions permission) {
        if (isRemote()) return;
        VirtualEnderRegistry.getInstance().deleteEntryIf(getOwner(), getEntryType(), getChannelName(),
                VirtualEntry::canRemove);
        this.permission = permission;
        setVirtualEntry();
    }

    protected void setVirtualEntry() {
        setEntry(VirtualEnderRegistry.getInstance().getOrCreateEntry(getOwner(), getEntryType(), getChannelName()));
        getEntry().setColor(this.colorStr);
        this.isAnyChanged = true;
        subscriptionHandler.updateSubscription();
    }

    protected abstract EntryTypes<T> getEntryType();

    protected void update() {
        long timer = coverHolder.getOffsetTimer();
        if (timer % 5 != 0) return;

        if (isWorkingEnabled() && !isRemote()) {
            var entry = VirtualEnderRegistry.getInstance().getOrCreateEntry(getOwner(), getEntryType(),
                    getChannelName());
            if (!entry.getColorStr().equals(this.colorStr)) {
                entry.setColor(this.colorStr);
            }
            if (!getEntry().equals(entry)) {
                setEntry(entry);
            }
            transfer();
        }

        if (isAnyChanged) {
            isAnyChanged = false;
        }
        subscriptionHandler.updateSubscription();
    }

    protected abstract void transfer();

    protected void setManualIOMode(ManualIOMode manualIOMode) {
        this.manualIOMode = manualIOMode;
        subscriptionHandler.updateSubscription();
    }

    @Nullable
    protected FilterHandler<?, ?> getFilterHandler() {
        return null;
    }

    protected abstract UIElement addVirtualEntryWidget(Supplier<? extends VirtualEntry> entry, boolean interactive);

    protected abstract String getUITitle();

    protected int getColor() {
        return VirtualEntry.parseColor(this.colorStr);
    }

    protected enum Permissions implements EnumSelector.SelectableEnum {

        PUBLIC("cover.ender_fluid_link.private.tooltip.disabled",
                UVSpriteTexture.of(GuiTextures.BUTTON_PUBLIC_PRIVATE, 0, 0, 1, 0.5f)),

        PRIVATE("cover.ender_fluid_link.private.tooltip.enabled",
                UVSpriteTexture.of(GuiTextures.BUTTON_PUBLIC_PRIVATE, 0, 0.5f, 1, 0.5f));

        @Getter
        private final String tooltip;
        @Getter
        private final IGuiTexture icon;

        Permissions(String tooltip, IGuiTexture icon) {
            this.tooltip = tooltip;
            this.icon = icon;
        }
    }

    protected static class VirtualEntryWidget extends UIElement {

        private static final int WIDGET_BOARD = 20;
        private static final int BUTTON_SIZE = 16;
        private static final String CHANNELS_REQUEST = "gtceu:ender_channels_request";
        private static final String CHANNELS_RESPONSE = "gtceu:ender_channels_response";
        private static final String CHANNEL_SELECT = "gtceu:ender_channel_select";
        private static final String CHANNEL_CLEAR = "gtceu:ender_channel_clear";

        private final AbstractEnderLinkCover<?> cover;
        private final UIElement mainGroup = new UIElement();
        private final ScrollerView channelsGroup = new ScrollerView();
        private final UIElement nameHolder = new UIElement();
        private String builtColor = "";
        private boolean showChannels;

        VirtualEntryWidget(AbstractEnderLinkCover<?> cover) {
            this.cover = cover;
            layout(layout -> layout.minWidth(176).paddingAll(5).gapAll(3));

            var titleGroup = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3)
                    .alignItems(AlignItems.CENTER));
            titleGroup.addChild(createToggleButton());
            titleGroup.addChild(new Label().setText(Component.translatable(cover.getUITitle())));

            mainGroup.layout(layout -> layout.gapAll(3));
            channelsGroup.layout(layout -> layout.width(TOTAL_WIDTH).height(110));
            channelsGroup.setDisplay(false);
            addChildren(titleGroup, mainGroup, channelsGroup);

            initWidgets();

            addEventListener(UIEvents.TICK, event -> {
                if (!Objects.equals(builtColor, cover.colorStr)) {
                    buildNameInput();
                }
            });
            onMessage(CHANNELS_REQUEST, tag -> {
                if (!cover.isRemote()) sendChannels();
            });
            onMessage(CHANNELS_RESPONSE, tag -> {
                if (cover.isRemote()) showChannelList(tag.getList("entries", Tag.TAG_COMPOUND));
            });
            onMessage(CHANNEL_SELECT, tag -> {
                if (!cover.isRemote()) cover.setChannelName(tag.getString("name"));
            });
            onMessage(CHANNEL_CLEAR, tag -> {
                if (cover.isRemote()) return;
                VirtualEnderRegistry.getInstance().getEntry(cover.getOwner(), cover.getEntryType(),
                        tag.getString("name")).setDescription("");
            });
        }

        private static final int TOTAL_WIDTH = 156;

        private void initWidgets() {
            var channelRow = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(2)
                    .alignItems(AlignItems.CENTER));
            channelRow.addChild(new EnumSelector<>(Permissions.values(), () -> cover.permission, cover::setPermission)
                    .layout(layout -> layout.width(WIDGET_BOARD).height(WIDGET_BOARD)));
            channelRow.addChild(new ColorBlock(cover::getColor)
                    .layout(layout -> layout.width(WIDGET_BOARD).height(WIDGET_BOARD)));
            nameHolder.layout(layout -> layout.flex(1));
            channelRow.addChild(nameHolder);
            buildNameInput();
            mainGroup.addChild(channelRow);

            var description = new TextField().setAnyString();
            description.layout(layout -> layout.widthPercent(100).height(WIDGET_BOARD));
            description.style(style -> style.tooltips("cover.ender_fluid_link.tooltip.channel_description"));
            description.bind(DataBindingBuilder.string(() -> cover.getEntry().getDescription(),
                    text -> cover.getEntry().setDescription(text == null ? "" : text)).build());
            mainGroup.addChild(description);

            var controls = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(2)
                    .alignItems(AlignItems.CENTER));
            controls.addChild(new ToggleButton(GuiTextures.BUTTON_POWER, cover::isWorkingEnabled,
                    cover::setWorkingEnabled).layout(layout -> layout.width(WIDGET_BOARD).height(WIDGET_BOARD)));
            controls.addChild(new EnumSelector<>(List.of(IO.IN, IO.OUT), () -> cover.io, cover::setIo)
                    .layout(layout -> layout.width(WIDGET_BOARD).height(WIDGET_BOARD)));
            var manual = new EnumSelector<>(ManualIOMode.VALUES, () -> cover.manualIOMode, cover::setManualIOMode);
            manual.layout(layout -> layout.width(WIDGET_BOARD).height(WIDGET_BOARD));
            manual.style(style -> style.tooltips("cover.universal.manual_import_export.mode.description"));
            controls.addChild(manual);
            mainGroup.addChild(controls);

            mainGroup.addChild(cover.addVirtualEntryWidget(cover::getEntry, true));

            var filterHandler = cover.getFilterHandler();
            if (filterHandler != null) {
                mainGroup.addChild(filterHandler.createFilterConfigUI());
                mainGroup.addChild(filterHandler.createFilterSlotUI());
            }
        }

        private void buildNameInput() {
            nameHolder.clearAllChildren();
            builtColor = cover.colorStr;
            var input = new ConfirmTextInput(cover.colorStr, cover::setChannelName, text -> {
                if (text == null || !COLOR_INPUT_PATTERN.matcher(text).matches()) {
                    return VirtualTank.DEFAULT_COLOR;
                }
                return text;
            }, text -> {
                if (text.length() < 8) {
                    text += "F".repeat(8 - text.length());
                }
                return text;
            }).setTooltip("cover.ender_fluid_link.tooltip.channel_name");
            input.layout(layout -> layout.widthPercent(100).height(WIDGET_BOARD));
            nameHolder.addChild(input);
        }

        private UIElement createToggleButton() {
            var button = new Button().noText();
            button.layout(layout -> layout.width(12).height(12));
            var back = DynamicTexture.of(() -> IGuiTexture.group(
                    GuiTextures.TOGGLE_BUTTON_BACK.copy().setSprite(0, showChannels ? 18 : 0, 18, 18),
                    GuiTextures.BUTTON_LIST));
            button.buttonStyle(style -> style.baseTexture(back).hoverTexture(back).pressedTexture(back));
            button.style(style -> style.tooltips("cover.ender_fluid_link.tooltip.list_button"));
            button.setOnClick(event -> {
                showChannels = !showChannels;
                mainGroup.setDisplay(!showChannels);
                channelsGroup.setDisplay(showChannels);
                if (showChannels) requestChannels();
            });
            return button;
        }

        private void requestChannels() {
            sendMessage(CHANNELS_REQUEST);
        }

        private void sendChannels() {
            var entries = VirtualEnderRegistry.getInstance().getEntryNames(cover.getOwner(), cover.getEntryType())
                    .stream().map(name -> VirtualEnderRegistry.getInstance().getEntry(cover.getOwner(),
                            cover.getEntryType(), name))
                    .sorted(Comparator.comparing(VirtualEntry::getColorStr)).toList();
            var list = new ListTag();
            for (var entry : entries) {
                list.add(entry.serializeNBT());
            }
            var tag = new CompoundTag();
            tag.put("entries", list);
            sendMessage(CHANNELS_RESPONSE, tag);
        }

        private void showChannelList(ListTag list) {
            channelsGroup.clearAllScrollViewChildren();
            for (int i = 0; i < list.size(); i++) {
                VirtualEntry entry = cover.getEntryType().createInstance();
                entry.deserializeNBT(list.getCompound(i));
                channelsGroup.addScrollViewChild(createChannelWidget(entry));
            }
        }

        private UIElement createChannelWidget(VirtualEntry entry) {
            int margin = 2;
            int availableWidth = TOTAL_WIDTH - (BUTTON_SIZE + margin) * 3;
            var channelName = cover.getChannelName(entry);

            var row = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(margin)
                    .alignItems(AlignItems.CENTER).width(TOTAL_WIDTH).height(BUTTON_SIZE + 4));
            row.style(style -> style.backgroundTexture(DynamicTexture.of(
                    () -> channelName.equals(cover.getChannelName()) ? ColorPattern.T_WHITE.rectTexture() :
                            IGuiTexture.EMPTY)));
            row.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (channelName.equals(cover.getChannelName())) return;
                var tag = new CompoundTag();
                tag.putString("name", entry.getColorStr());
                sendMessage(CHANNEL_SELECT, tag);
            });

            var color = VirtualEntry.parseColor(entry.getColorStr());
            row.addChild(new ColorBlock(() -> color).layout(layout -> layout.width(BUTTON_SIZE).height(BUTTON_SIZE)));

            var texts = new UIElement().layout(layout -> layout.width(availableWidth));
            texts.addChild(new Label().setText(Component.literal(entry.getColorStr())));
            var des = entry.getDescription();
            if (!des.isEmpty()) {
                var desLabel = new Label().setText(Component.literal(ChatFormatting.DARK_GRAY + des));
                desLabel.textStyle(style -> style.textShadow(false).textWrap(TextWrap.HOVER_ROLL));
                texts.addChild(desLabel);
            }
            row.addChild(texts);

            row.addChild(cover.addVirtualEntryWidget(() -> entry, false)
                    .layout(layout -> layout.width(BUTTON_SIZE).height(BUTTON_SIZE)));

            var clear = new Button().noText();
            clear.layout(layout -> layout.width(BUTTON_SIZE).height(BUTTON_SIZE));
            clear.buttonStyle(style -> style.baseTexture(GuiTextures.BUTTON_CLEAR_GRID)
                    .hoverTexture(GuiTextures.BUTTON_CLEAR_GRID).pressedTexture(GuiTextures.BUTTON_CLEAR_GRID));
            clear.style(style -> style.tooltips("cover.ender_fluid_link.tooltip.clear_button"));
            clear.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                event.stopPropagation();
                var tag = new CompoundTag();
                tag.putString("name", channelName);
                sendMessage(CHANNEL_CLEAR, tag);
                requestChannels();
            });
            row.addChild(clear);
            return row;
        }
    }
}
