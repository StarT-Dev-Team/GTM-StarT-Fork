package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.placeholder.IPlaceholderInfoProviderCover;
import com.gregtechceu.gtceu.api.placeholder.MultiLineComponent;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderContext;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderHandler;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.client.renderer.cover.CoverTextRenderer;
import com.gregtechceu.gtceu.client.renderer.cover.IDynamicCoverRenderer;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.integration.create.GTCreateIntegration;
import com.gregtechceu.gtceu.utils.GTStringUtils;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ComputerMonitorCover extends CoverBehavior
                                  implements IUICover, IDataStickInteractable, IPlaceholderInfoProviderCover {

    private TickableSubscription subscription;
    private final CoverTextRenderer renderer;
    @Persisted
    @Getter
    private final List<String> formatStringArgs = new ArrayList<>(8);
    @Persisted
    @Getter
    private final List<String> formatStringLines = new ArrayList<>(8);
    @Persisted
    @DescSynced
    @Getter
    private final List<MutableComponent> text = new ArrayList<>();
    @Persisted
    public final CustomItemStackHandler itemStackHandler = new CustomItemStackHandler(8);
    @Setter
    private String placeholderSearch = "";
    @Setter
    @Getter
    @Persisted
    private int updateInterval = 100;
    @Getter
    @Persisted
    private long ticksSincePlaced = 0;
    @Persisted
    @Getter
    private final List<MutableComponent> createDisplayTargetBuffer = new ArrayList<>();
    @Persisted
    @Getter
    private final List<MutableComponent> computerCraftTextBuffer = new ArrayList<>();
    @Persisted
    @Getter
    private final UUID placeholderUUID;

    public ComputerMonitorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        renderer = new CoverTextRenderer(this::getText);
        placeholderUUID = UUID.randomUUID();
        for (int i = 0; i < 100; i++) {
            createDisplayTargetBuffer.add(MutableComponent.create(ComponentContents.EMPTY));
            computerCraftTextBuffer.add(MutableComponent.create(ComponentContents.EMPTY));
        }
    }

    public List<MutableComponent> getRenderedText() {
        String s = formatStringLines.stream().reduce((a, b) -> a + "\n" + b).orElse("");
        List<String> tmp = new ArrayList<>(formatStringArgs);
        tmp = tmp.stream().map(str -> '{' + str + '}').toList();
        return PlaceholderHandler.processPlaceholders(
                GTStringUtils.replace(s, "\\{}", tmp),
                new PlaceholderContext(coverHolder.getLevel(), coverHolder.getPos(), attachedSide, itemStackHandler,
                        this, new MultiLineComponent(text), placeholderUUID));
    }

    public void setDisplayTargetBufferLine(int line, MutableComponent component) {
        createDisplayTargetBuffer.set(line, component);
    }

    @Override
    public void setComputerCraftTextBufferLine(int line, MutableComponent component) {
        computerCraftTextBuffer.set(line, component);
    }

    @Override
    public boolean canPipePassThrough() {
        return false;
    }

    @Override
    public Supplier<IDynamicCoverRenderer> getDynamicRenderer() {
        return () -> renderer;
    }

    @Override
    public Component getCoverUITitle() {
        return Component.translatable("gtceu.computer_monitor_cover.title");
    }

    @Override
    public UIElement createUIWidget() {
        var group = new UIElement().layout(layout -> layout.paddingAll(5).gapAll(3));

        var mainPage = new UIElement();
        var blankPlaceholdersPage = new UIElement();
        group.addChildren(mainPage, blankPlaceholdersPage);

        {
            mainPage.layout(s -> s.flexDirection(FlexDirection.COLUMN).gapAll(8));

            var cols = new UIElement().layout(s -> s.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(8));
            mainPage.addChild(cols);

            var controls = new UIElement();
            cols.addChild(controls);

            for (int i = 0; i < 8; i++) {
                int finalI = i;
                if (i >= formatStringLines.size()) formatStringLines.add("");
                var row = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(3)
                    .alignItems(AlignItems.CENTER));
                var formatStringInput = new TextField().setAnyString();
                formatStringInput.layout(layout -> layout.width(160).height(15));
                formatStringInput.style(style -> style.tooltips(toTooltips(LangHandler.getMultiLang(
                    "gtceu.gui.computer_monitor_cover.main_textbox_tooltip", finalI + 1))));
                formatStringInput.bind(DataBindingBuilder.string(() -> formatStringLines.get(finalI),
                    s -> formatStringLines.set(finalI, s)).build());
                var slot = new ItemSlot().bind(itemStackHandler, i);
                slot.style(style -> style.tooltips(toTooltips(LangHandler.getMultiLang(
                    "gtceu.gui.computer_monitor_cover.slot_tooltip", finalI + 1))));
                row.addChildren(formatStringInput, slot);
                controls.addChild(row);
            }

            var updateIntervalInput = new IntInput(this::getUpdateInterval, this::setUpdateInterval);
            updateIntervalInput.setMin(1);
            updateIntervalInput.setMax(60 * 20);
            updateIntervalInput.setTooltips("gtceu.gui.computer_monitor_cover.update_interval");
            updateIntervalInput.layout(layout -> layout.widthAuto().height(20).marginTop(4));
            controls.addChild(updateIntervalInput);

            cols.addChild(PlaceholderHandler.getPlaceholderHandlerUI(""));

            var navigationRow = new UIElement().layout(s -> s.flexDirection(FlexDirection.ROW).justifyContent(AlignContent.FLEX_END));
            mainPage.addChild(navigationRow);

            var nextPage = new Button().noText();
            nextPage.layout(layout -> layout.width(20).height(20).alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
            nextPage.style(style -> style
                .tooltips("gtceu.gui.computer_monitor_cover.edit_blank_placeholders"));
            nextPage.setOnClick(event -> {
                mainPage.setDisplay(false);
                blankPlaceholdersPage.setDisplay(true);
            });
            nextPage.addChild(new UIElement()
                .layout(s -> s.width(12).height(12))
                .style(s -> s.background(GuiTextures.BUTTON_PAGE_RIGHT)));
            navigationRow.addChild(nextPage);
        }

        {
            blankPlaceholdersPage.layout(layout -> layout.gapAll(8)).setDisplay(false);

            var controls = new UIElement().layout(s -> s.gapAll(2));
            blankPlaceholdersPage.addChild(controls);

            for (int i = 0; i < 8; i++) {
                int finalI = i;
                if (i >= formatStringArgs.size()) formatStringArgs.add("");
                var formatStringArgsInput = new TextField().setAnyString();
                formatStringArgsInput.layout(layout -> layout.width(160).height(15));
                formatStringArgsInput.style(style -> style.tooltips(toTooltips(LangHandler.getMultiLang(
                    "gtceu.gui.computer_monitor_cover.second_page_textbox_tooltip",
                    GTStringUtils.getIntOrderingSuffix(finalI + 1)))));
                formatStringArgsInput.bind(DataBindingBuilder.string(() -> formatStringArgs.get(finalI),
                    s -> formatStringArgs.set(finalI, s)).build());
                controls.addChild(formatStringArgsInput);
            }

            var navigationRow = new UIElement().layout(s -> s.flexDirection(FlexDirection.ROW).justifyContent(AlignContent.FLEX_START));
            blankPlaceholdersPage.addChild(navigationRow);

            var prevPage = new Button().noText();
            prevPage.layout(layout -> layout.width(20).height(20).alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
            prevPage.style(style -> style
                .tooltips("gtceu.gui.computer_monitor_cover.edit_blank_placeholders"));
            prevPage.setOnClick(event -> {
                mainPage.setDisplay(true);
                blankPlaceholdersPage.setDisplay(false);
            });
            prevPage.addChild(new UIElement()
                .layout(s -> s.width(12).height(12))
                .style(s -> s.background(GuiTextures.BUTTON_PAGE_LEFT)));
            navigationRow.addChild(prevPage);
        }
        return group;
    }

    private static Component[] toTooltips(List<MutableComponent> components) {
        return components.toArray(new Component[0]);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        subscription = coverHolder.subscribeServerTick(subscription, this::update);
    }

    private void update() {
        ticksSincePlaced++;
        if (coverHolder.getOffsetTimer() % updateInterval == 0) {
            List<MutableComponent> newText;
            try {
                if (GTCEu.Mods.isCreateLoaded())
                    GTCreateIntegration.TemporaryRedstoneLinkTransmitter.destroyAll();
                setRedstoneSignalOutput(0);
                newText = getRenderedText();
            } catch (RuntimeException e) {
                newText = GTUtil
                        .list(Component.translatable("gtceu.computer_monitor_cover.error.exception", e.getMessage()));
            }
            if (!text.equals(newText)) {
                text.clear();
                text.addAll(newText);
            }
        }
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public List<ItemStack> getAdditionalDrops() {
        List<ItemStack> drops = super.getAdditionalDrops();
        for (int i = 0; i < 8; i++) {
            if (!itemStackHandler.getStackInSlot(i).isEmpty())
                drops.add(itemStackHandler.getStackInSlot(i));
        }
        return drops;
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        CompoundTag tag = dataStick.getTagElement("computer_monitor_cover_config");
        if (tag == null) return InteractionResult.FAIL;
        List<String> stringLines = new ArrayList<>();
        ListTag stringLinesTag = tag.getList("lines", Tag.TAG_STRING);
        for (int i = 0; i < stringLinesTag.size(); i++) stringLines.add(stringLinesTag.getString(i));
        formatStringLines.clear();
        formatStringLines.addAll(stringLines);
        List<String> stringArgs = new ArrayList<>();
        ListTag stringArgsTag = tag.getList("args", Tag.TAG_STRING);
        for (int i = 0; i < stringArgsTag.size(); i++) stringArgs.add(stringArgsTag.getString(i));
        formatStringArgs.clear();
        formatStringArgs.addAll(stringArgs);
        updateInterval = tag.getInt("updateInterval");
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        CompoundTag tag = dataStick.getOrCreateTagElement("computer_monitor_cover_config");
        ListTag stringLinesTag = new ListTag();
        formatStringLines.forEach(line -> stringLinesTag.add(StringTag.valueOf(line)));
        tag.put("lines", stringLinesTag);
        ListTag stringArgsTag = new ListTag();
        formatStringArgs.forEach(line -> stringArgsTag.add(StringTag.valueOf(line)));
        tag.put("args", stringArgsTag);
        tag.putInt("updateInterval", updateInterval);
        return InteractionResult.SUCCESS;
    }
}
