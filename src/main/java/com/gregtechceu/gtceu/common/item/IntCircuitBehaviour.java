package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IntCircuitBehaviour implements IItemUIFactory, IAddInformation {

    public static final int CIRCUIT_MAX = 32;

    public static ItemStack stack(int configuration) {
        var stack = GTItems.PROGRAMMED_CIRCUIT.asStack();
        setCircuitConfiguration(stack, configuration);
        return stack;
    }

    public static void setCircuitConfiguration(ItemStack itemStack, int configuration) {
        if (configuration < 0 || configuration > CIRCUIT_MAX)
            throw new IllegalArgumentException("Given configuration number is out of range!");
        var tagCompound = itemStack.getOrCreateTag();
        tagCompound.putInt("Configuration", configuration);
    }

    public static int getCircuitConfiguration(ItemStack itemStack) {
        if (!isIntegratedCircuit(itemStack)) return 0;
        var tagCompound = itemStack.getTag();
        if (tagCompound != null) {
            return tagCompound.getInt("Configuration");
        }
        return 0;
    }

    public static boolean isIntegratedCircuit(ItemStack itemStack) {
        boolean isCircuit = GTItems.PROGRAMMED_CIRCUIT.isIn(itemStack);
        if (isCircuit && !itemStack.hasTag()) {
            var compound = new CompoundTag();
            compound.putInt("Configuration", 0);
            itemStack.setTag(compound);
        }
        return isCircuit;
    }

    // deprecated, not needed (for now)
    @Deprecated
    public static void adjustConfiguration(ItemStack stack, int amount) {
        if (!isIntegratedCircuit(stack)) return;
        int configuration = getCircuitConfiguration(stack);
        configuration += amount;
        configuration = Mth.clamp(configuration, 0, CIRCUIT_MAX);
        setCircuitConfiguration(stack, configuration);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        int configuration = getCircuitConfiguration(stack);
        tooltipComponents.add(Component.translatable("metaitem.int_circuit.configuration", configuration));
    }

    @Override
    public ModularUI createUI(HeldItemUIMenuType.HeldItemUIHolder holder, Player entityPlayer) {
        var root = UITemplate.panel();
        root.layout(layout -> layout.width(184).paddingAll(9).gapAll(4));

        var label = new Label().setText(Component.literal("Programmed Circuit Configuration"));
        label.textStyle(style -> style.textColor(0x404040).textShadow(false));
        root.addChild(label);

        var slot = new ItemSlot().bind(DataBindingBuilder
                .itemStackS2C(() -> stack(getCircuitConfiguration(holder.itemStack))).build());
        slot.style(style -> style.backgroundTexture(GuiTextures.SLOT));
        var slotRow = new UIElement().layout(layout -> layout.alignItems(AlignItems.CENTER));
        slotRow.addChild(slot);
        root.addChild(slotRow);

        var grid = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP));
        for (int idx = 0; idx <= 32; idx++) {
            int finalIdx = idx;
            var button = new Button().noText();
            button.layout(layout -> layout.width(18).height(18));
            button.buttonStyle(style -> style.baseTexture(IGuiTexture.group(GuiTextures.SLOT,
                    new ItemStackTexture(stack(finalIdx)).scale(16f / 18))));
            button.setOnServerClick(event -> setCircuitConfiguration(holder.itemStack, finalIdx));
            grid.addChild(button);
        }
        root.addChild(grid);
        return UITemplate.modularUI(root, entityPlayer);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var stack = context.getItemInHand();
        int circuitSetting = getCircuitConfiguration(stack);
        BlockEntity entity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (entity instanceof IMachineBlockEntity machineEntity && context.isSecondaryUseActive()) {
            if (machineEntity.getMetaMachine() instanceof IHasCircuitSlot circuitMachine &&
                    circuitMachine.getCircuitInventory().getSlots() > 0) {
                if (!circuitMachine.isCircuitPastingBlocked()) {
                    setCircuitConfig(circuitMachine.getCircuitInventory(), circuitSetting);
                    if (!ConfigHolder.INSTANCE.machines.ghostCircuit)
                        stack.shrink(1);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.SUCCESS;
        }
        return IItemUIFactory.super.useOn(context);
    }

    void setCircuitConfig(NotifiableItemStackHandler circuit, int value) {
        circuit.setStackInSlot(0, IntCircuitBehaviour.stack(value));
    }
}
