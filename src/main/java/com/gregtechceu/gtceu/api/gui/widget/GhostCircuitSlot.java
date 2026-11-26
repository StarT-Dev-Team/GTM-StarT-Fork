package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

public class GhostCircuitSlot extends ItemSlot {

    private static final int NO_CONFIG = -1;

    @Getter
    private IItemHandlerModifiable circuitInventory;
    @Nullable
    @Setter
    private IHasCircuitSlot circuitMachine;
    private final UIElement configurator = new UIElement();

    public GhostCircuitSlot() {
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);
        configurator.setDisplay(false);
        addChild(configurator);
    }

    public GhostCircuitSlot setCircuitInventory(IItemHandlerModifiable circuitInventory) {
        this.circuitInventory = circuitInventory;
        bind(DataBindingBuilder.itemStack(() -> circuitInventory.getStackInSlot(0),
                stack -> circuitInventory.setStackInSlot(0, stack)).build());
        buildConfigurator();
        return this;
    }

    public boolean isConfiguratorOpen() {
        return configurator.isDisplayed();
    }

    @Override
    protected void onMouseDown(UIEvent event) {
        super.onMouseDown(event);
        if (!isActiveInHierarchy() || circuitInventory == null) return;
        if (event.button == 0 && event.isShiftDown()) {
            configurator.setDisplay(!isConfiguratorOpen());
        } else if (event.button == 0) {
            setCircuitValue(getNextValue(true));
        } else if (event.button == 1 && event.isShiftDown()) {
            setValue(ItemStack.EMPTY, true);
        } else if (event.button == 1) {
            setCircuitValue(getNextValue(false));
        }
    }

    private void onMouseWheel(UIEvent event) {
        if (isConfiguratorOpen() || circuitInventory == null) return;
        setCircuitValue(getNextValue(event.deltaY >= 0));
    }

    private int getNextValue(boolean increment) {
        var stack = getValue();
        int currentValue = IntCircuitBehaviour.getCircuitConfiguration(stack);
        if (increment) {
            if (currentValue == IntCircuitBehaviour.CIRCUIT_MAX) {
                return 0;
            }
            if (stack.isEmpty()) {
                return 1;
            }
            return currentValue + 1;
        } else {
            if (stack.isEmpty()) {
                return IntCircuitBehaviour.CIRCUIT_MAX;
            }
            if (currentValue == 1) {
                return NO_CONFIG;
            }
            return currentValue - 1;
        }
    }

    public void setCircuitValue(int newValue) {
        setValue(newValue == NO_CONFIG ? ItemStack.EMPTY : IntCircuitBehaviour.stack(newValue), true);
    }

    private void buildConfigurator() {
        configurator.clearAllChildren();
        configurator.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(0).top(20).width(174)
                .height(132).paddingAll(5).flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP))
                .style(style -> style.backgroundTexture(GuiTextures.BACKGROUND).zIndex(100));

        var title = new Label().setText(Component.literal("Programmed Circuit Configuration"));
        title.layout(layout -> layout.widthPercent(100).height(12));

        var ghost = ConfigHolder.INSTANCE.machines.ghostCircuit;
        var slot = new ItemSlot().bind(new ItemHandlerSlot(circuitInventory, 0).setCanPlace(stack -> !ghost)
                .setCanTake(player -> !ghost));
        slot.layout(layout -> layout.marginHorizontal(78));
        slot.style(style -> style.backgroundTexture(IGuiTexture.group(GuiTextures.SLOT,
                GuiTextures.INT_CIRCUIT_OVERLAY)));
        configurator.addChildren(title, slot);

        if (circuitMachine != null && circuitMachine.isCircuitPastingEnabled()) {
            var machine = circuitMachine;
            configurator.addChild(new ToggleButton(GuiTextures.BUTTON_LOCK, machine::isCircuitPastingBlocked,
                    machine::setCircuitPastingBlocked)
                    .setTooltipText("gtceu.gui.circuit_pasting.tooltip")
                    .setShouldUseBaseBackground());
        }
        if (ghost) {
            var clear = new Button().noText();
            clear.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(78).top(20).width(18).height(18));
            clear.setOnServerClick(event -> circuitInventory.setStackInSlot(0, ItemStack.EMPTY));
            configurator.addChild(clear);
        }

        for (var index = 0; index <= 32; index++) {
            configurator.addChild(circuitButton(index));
        }
    }

    private Button circuitButton(int index) {
        var button = new Button().noText();
        button.layout(layout -> layout.width(18).height(18));
        button.buttonStyle(style -> style.baseTexture(IGuiTexture.group(GuiTextures.SLOT,
                new ItemStackTexture(IntCircuitBehaviour.stack(index)).scale(16f / 18))));
        button.setOnServerClick(event -> {
            var stack = circuitInventory.getStackInSlot(0).copy();
            if (IntCircuitBehaviour.isIntegratedCircuit(stack)) {
                IntCircuitBehaviour.setCircuitConfiguration(stack, index);
                circuitInventory.setStackInSlot(0, stack);
            } else if (ConfigHolder.INSTANCE.machines.ghostCircuit) {
                circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(index));
            }
        });
        return button;
    }

    @Override
    public String name() {
        return "item-slot";
    }
}
