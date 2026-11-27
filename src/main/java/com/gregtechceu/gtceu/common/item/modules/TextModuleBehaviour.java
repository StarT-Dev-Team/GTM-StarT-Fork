package com.gregtechceu.gtceu.common.item.modules;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IMonitorModuleItem;
import com.gregtechceu.gtceu.api.placeholder.MultiLineComponent;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderContext;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderHandler;
import com.gregtechceu.gtceu.client.renderer.monitor.IMonitorRenderer;
import com.gregtechceu.gtceu.client.renderer.monitor.MonitorTextRenderer;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CentralMonitorMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;
import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.common.network.packets.SCPacketMonitorGroupNBTChange;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TextModuleBehaviour implements IMonitorModuleItem, IAddInformation {

    private void updateText(ItemStack stack, PlaceholderContext ctx) {
        if (!stack.getOrCreateTag().contains("placeholderUUID")) {
            stack.getOrCreateTag().putUUID("placeholderUUID", UUID.randomUUID());
        }
        MultiLineComponent text = PlaceholderHandler.processPlaceholders(
                getPlaceholderText(stack),
                ctx);
        stack.getOrCreateTag().put("text", text.toTag());
    }

    private PlaceholderContext makeContext(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        return new PlaceholderContext(
                group.getTargetLevel(machine.getLevel()),
                group.getTarget(machine.getLevel()),
                group.getTargetCoverSide(),
                group.getPlaceholderSlotsHandler(),
                group.getTargetCover(machine.getLevel()),
                null,
                stack.getOrCreateTag().contains("placeholderUUID") ? stack.getOrCreateTag().getUUID("placeholderUUID") :
                        null);
    }

    @Override
    public void tick(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        this.updateText(stack, makeContext(stack, machine, group));
    }

    @Override
    public void tickInPlaceholder(ItemStack stack, PlaceholderContext context) {
        this.updateText(stack, context);
    }

    @Override
    public IMonitorRenderer getRenderer(ItemStack stack) {
        return new MonitorTextRenderer(
                getText(stack),
                Math.max(getScale(stack), .0001));
    }

    @Override
    public UIElement createUIWidget(ItemStack stack, CentralMonitorMachine machine, MonitorGroup group) {
        var builder = new UIElement().layout(layout -> layout.gapAll(3));
        var editor = new CodeEditor();
        editor.layout(layout -> layout.width(120).height(80));
        var scaleInput = new TextField().setAnyString();
        scaleInput.layout(layout -> layout.width(40).height(10));
        scaleInput.style(style -> style.tooltips(Component.translatable("gtceu.gui.central_monitor.text_scale")));
        if (!stack.getOrCreateTag().contains("scale")) {
            stack.getOrCreateTag().putDouble("scale", 1);
            if (machine.isRemote()) {
                GTNetwork.sendToServer(new SCPacketMonitorGroupNBTChange(stack, group, machine));
            }
        }
        scaleInput.setValue(String.valueOf(Mth.clamp(stack.getOrCreateTag().getDouble("scale"), .0001, 1000)), false);

        var saveButton = new Button().noText();
        saveButton.layout(layout -> layout.width(20).height(20));
        saveButton.buttonStyle(style -> style.baseTexture(GuiTextures.BUTTON_CHECK));
        saveButton.setOnClick(event -> {
            ListTag listTag = new ListTag();
            for (var line : editor.getValue()) {
                listTag.add(StringTag.valueOf(line.replaceAll("\r", "")));
            }
            CompoundTag tag2 = stack.getOrCreateTag();
            tag2.put("formatStringLines", listTag);
            try {
                tag2.putDouble("scale", Double.parseDouble(scaleInput.getValue()));
            } catch (NumberFormatException ignored) {}
            stack.setTag(tag2);
            GTNetwork.sendToServer(new SCPacketMonitorGroupNBTChange(stack, group, machine));
        });

        ListTag tag = stack.getOrCreateTag().getList("formatStringLines", Tag.TAG_STRING);
        List<String> formatStringLines = new ArrayList<>();
        for (Tag line : tag) formatStringLines.add(line.getAsString());
        editor.setValue(formatStringLines.toArray(new String[0]), false);
        builder.addChildren(editor, saveButton, scaleInput, PlaceholderHandler.getPlaceholderHandlerUI(""));
        return builder;
    }

    @Override
    public String getType() {
        return "text";
    }

    public MultiLineComponent getText(ItemStack stack) {
        return MultiLineComponent.fromTag(stack.getOrCreateTag().get("text"));
    }

    public double getScale(ItemStack stack) {
        return Math.max(stack.getOrCreateTag().getDouble("scale"), .0001);
    }

    public void setScale(ItemStack stack, double scale) {
        stack.getOrCreateTag().putDouble("scale", scale);
    }

    public void setPlaceholderText(ItemStack stack, String text) {
        ListTag listTag = new ListTag();
        for (String line : text.split("\n")) listTag.add(StringTag.valueOf(line.replaceAll("\r", "")));
        stack.getOrCreateTag().put("formatStringLines", listTag);
    }

    public String getPlaceholderText(ItemStack stack) {
        StringBuilder formatStringLines = new StringBuilder();
        ListTag tag = stack.getOrCreateTag().getList("formatStringLines", StringTag.TAG_STRING);
        for (Tag value : tag) {
            formatStringLines.append(value.getAsString()).append('\n');
        }
        return formatStringLines.toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        if (isAdvanced.isAdvanced()) {
            tooltipComponents.add(Component.literal("Placeholder text:").withStyle(ChatFormatting.GOLD));
            tooltipComponents.addAll(MultiLineComponent.literal(getPlaceholderText(stack)));
            tooltipComponents.add(Component.literal("Processed text:").withStyle(ChatFormatting.GOLD));
            tooltipComponents.addAll(getText(stack));
        }
    }
}
