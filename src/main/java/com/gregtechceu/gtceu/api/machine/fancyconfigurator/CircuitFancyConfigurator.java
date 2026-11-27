package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyCustomMiddleClickAction;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyCustomMouseWheelAction;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.lang.LangHandler;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;

import dev.vfyjxf.taffy.style.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CircuitFancyConfigurator implements IFancyConfigurator, IFancyCustomMouseWheelAction,
                                      IFancyCustomMiddleClickAction {

    private static final int NO_CONFIG = -1;

    @Nullable
    final IHasCircuitSlot circuitMachine;
    final ItemStackHandler circuitSlot;

    public CircuitFancyConfigurator(IHasCircuitSlot circuitMachine) {
        this(circuitMachine, circuitMachine.getCircuitInventory().storage);
    }

    public CircuitFancyConfigurator(ItemStackHandler circuitSlot) {
        this(null, circuitSlot);
    }

    public CircuitFancyConfigurator(@Nullable IHasCircuitSlot circuitMachine, ItemStackHandler circuitSlot) {
        this.circuitMachine = circuitMachine;
        this.circuitSlot = circuitSlot;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.gui.circuit.title");
    }

    @Override
    public IGuiTexture getIcon() {
        if (IntCircuitBehaviour.isIntegratedCircuit(circuitSlot.getStackInSlot(0))) {
            return new ItemStackTexture(circuitSlot.getStackInSlot(0));
        }
        return new GuiTextureGroup(new ItemStackTexture(IntCircuitBehaviour.stack(0)),
                new ItemStackTexture(Items.BARRIER));
    }

    @Override
    public boolean mouseWheelMove(double wheelDelta) {
        if (wheelDelta == 0) return false;
        if (!ConfigHolder.INSTANCE.machines.ghostCircuit && circuitSlot.getStackInSlot(0).isEmpty()) return false;
        int nextValue = getNextValue(wheelDelta > 0);
        if (nextValue == NO_CONFIG) {
            if (ConfigHolder.INSTANCE.machines.ghostCircuit) {
                circuitSlot.setStackInSlot(0, ItemStack.EMPTY);
            }
        } else {
            circuitSlot.setStackInSlot(0, IntCircuitBehaviour.stack(nextValue));
        }
        return true;
    }

    @Override
    public void onMiddleClick() {
        if (!ConfigHolder.INSTANCE.machines.ghostCircuit && !circuitSlot.getStackInSlot(0).isEmpty())
            circuitSlot.setStackInSlot(0, IntCircuitBehaviour.stack(0));
        else
            circuitSlot.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    public UIElement createConfigurator() {
        var group = new UIElement().layout(layout -> layout.width(174).paddingAll(5)
                .flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP));

        var ghost = ConfigHolder.INSTANCE.machines.ghostCircuit;

        var slotWrapper = new UIElement();
        slotWrapper.getLayoutStyle().widthPercent(100).flexDirection(FlexDirection.COLUMN)
                .alignItems(AlignItems.CENTER).marginLeft(-1);
        group.addChild(slotWrapper);

        var slot = new ItemSlot()
                .bind(new ItemHandlerSlot(circuitSlot, 0).setCanPlace(stack -> !ghost).setCanTake(player -> !ghost));
        slot.getStyle().backgroundTexture(IGuiTexture.group(GuiTextures.SLOT, GuiTextures.INT_CIRCUIT_OVERLAY));

        if (ghost) {
            slot.addServerEventListener(UIEvents.MOUSE_DOWN, event -> circuitSlot.setStackInSlot(0, ItemStack.EMPTY));
        }
        slotWrapper.addChildren(slot);

        for (var index = 0; index <= 32; index++) {
            group.addChild(circuitButton(index));
        }

        if (circuitMachine != null && circuitMachine.isCircuitPastingEnabled()) {
            var machine = circuitMachine;

            var circuitPastingGroup = new UIElement();
            circuitPastingGroup.getLayoutStyle().widthPercent(100).alignItems(AlignItems.END).paddingTop(4);
            group.addChild(circuitPastingGroup);

            var toggleButton = new ToggleButton(GuiTextures.BUTTON_LOCK, machine::isCircuitPastingBlocked,
                    machine::setCircuitPastingBlocked)
                    .setTooltipText("gtceu.gui.circuit_pasting.tooltip")
                    .setShouldUseBaseBackground();
            circuitPastingGroup.addChild(toggleButton);
        }

        return group;
    }

    private Button circuitButton(int index) {
        var circuitTexture = new ItemStackTexture(IntCircuitBehaviour.stack(index)).scale(16f / 18);
        var slotButtonTexture = IGuiTexture.group(GuiTextures.SLOT, circuitTexture);
        var slotButtonHoverTexture = IGuiTexture.group(slotButtonTexture,
                new ColorRectTexture(0x80FFFFFF).scale(16f / 18));

        var button = new Button().noText();
        button.getLayoutStyle().width(18).height(18);
        button.getButtonStyle().baseTexture(slotButtonTexture).hoverTexture(slotButtonHoverTexture)
                .pressedTexture(slotButtonHoverTexture);
        button.setOnServerClick(event -> {
            ItemStack stack = circuitSlot.getStackInSlot(0).copy();
            if (IntCircuitBehaviour.isIntegratedCircuit(stack)) {
                IntCircuitBehaviour.setCircuitConfiguration(stack, index);
                circuitSlot.setStackInSlot(0, stack);
            } else if (ConfigHolder.INSTANCE.machines.ghostCircuit) {
                circuitSlot.setStackInSlot(0, IntCircuitBehaviour.stack(index));
            }
        });
        return button;
    }

    @Override
    public List<Component> getTooltips() {
        var list = new ArrayList<>(IFancyConfigurator.super.getTooltips());
        list.addAll(Arrays.stream(
                LangHandler.getMultiLang("gtceu.gui.configurator_slot.tooltip").toArray(new MutableComponent[0]))
                .toList());
        return list;
    }

    private int getNextValue(boolean increment) {
        int currentValue = IntCircuitBehaviour.getCircuitConfiguration(circuitSlot.getStackInSlot(0));
        if (increment) {
            // if at max, loop around to no circuit
            if (currentValue == IntCircuitBehaviour.CIRCUIT_MAX) {
                return 0;
            }
            // if at no circuit, skip 0 and return 1
            if (this.circuitSlot.getStackInSlot(0).isEmpty()) {
                return 1;
            }
            // normal case: increment by 1
            return currentValue + 1;
        } else {
            // if at no circuit, loop around to max
            if (this.circuitSlot.getStackInSlot(0).isEmpty() ||
                    (currentValue == 0 && !ConfigHolder.INSTANCE.machines.ghostCircuit)) {
                return IntCircuitBehaviour.CIRCUIT_MAX;
            }
            // if at 1, skip 0 and return no circuit
            if (currentValue == 1 && ConfigHolder.INSTANCE.machines.ghostCircuit) {
                return -1;
            }
            // normal case: decrement by 1
            return currentValue - 1;
        }
    }
}
