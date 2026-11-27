package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import org.jetbrains.annotations.NotNull;

public class SteamItemBusPartMachine extends ItemBusPartMachine {

    private final String autoTooltipKey;

    public SteamItemBusPartMachine(IMachineBlockEntity holder, IO io, Object... args) {
        super(holder, 1, io, args);
        autoTooltipKey = io == IO.IN ? "gtceu.gui.item_auto_input.tooltip" : "gtceu.gui.item_auto_output.tooltip";
    }

    @NotNull
    @Override
    public ModularUI createUI(@NotNull Player entityPlayer) {
        int rowSize = (int) Math.sqrt(getInventorySize());
        boolean steel = ConfigHolder.INSTANCE.machines.steelSteamMultiblocks;

        var title = new Label();
        title.setText(Component.translatable(getBlockState().getBlock().getDescriptionId()));
        title.layout(layout -> layout.alignSelf(AlignItems.FLEX_START).marginLeft(5));

        var toggle = new ToggleButton(GuiTextures.BUTTON_ITEM_OUTPUT, this::isWorkingEnabled,
                this::setWorkingEnabled)
                .setShouldUseBaseBackground()
                .setTooltipText(autoTooltipKey);
        toggle.layout(layout -> layout.alignSelf(AlignItems.FLEX_START).marginLeft(2));

        var grid = new UIElement();
        boolean canPut = io.support(IO.IN);
        for (int y = 0; y < rowSize; y++) {
            var row = new UIElement();
            row.layout(layout -> layout.flexDirection(FlexDirection.ROW));
            for (int x = 0; x < rowSize; x++) {
                var slot = new ItemSlot().bind(new ItemHandlerSlot(getInventory().storage, y * rowSize + x)
                        .setCanPlace(stack -> canPut));
                slot.style(style -> style.backgroundTexture(GuiTextures.SLOT_STEAM.get(steel)));
                row.addChild(slot);
            }
            grid.addChild(row);
        }

        var root = UITemplate.panel();
        root.style(style -> style.background(GuiTextures.BACKGROUND_STEAM.get(steel)));
        root.layout(layout -> layout.minWidth(176).paddingAll(5).paddingBottom(7));
        root.addChildren(title, grid, toggle, UITemplate.bindPlayerInventory(GuiTextures.SLOT_STEAM.get(steel)));
        return UITemplate.modularUI(root, entityPlayer);
    }

    @Override
    public boolean swapIO() {
        BlockPos blockPos = getHolder().pos();
        MachineDefinition newDefinition = null;
        if (io == IO.IN) {
            newDefinition = GTMachines.STEAM_EXPORT_BUS;
        } else if (io == IO.OUT) {
            newDefinition = GTMachines.STEAM_IMPORT_BUS;
        }

        if (newDefinition == null) return false;
        BlockState newBlockState = newDefinition.getBlock().defaultBlockState();

        getLevel().setBlockAndUpdate(blockPos, newBlockState);

        if (getLevel().getBlockEntity(blockPos) instanceof IMachineBlockEntity newHolder) {
            if (newHolder.getMetaMachine() instanceof SteamItemBusPartMachine newMachine) {
                // We don't set the circuit or distinct busses, since
                // that doesn't make sense on an output bus.
                // Furthermore, existing inventory items
                // and conveyors will drop to the floor on block override.
                newMachine.setFrontFacing(this.getFrontFacing());
                newMachine.setUpwardsFacing(this.getUpwardsFacing());
            }
        }
        return true;
    }
}
