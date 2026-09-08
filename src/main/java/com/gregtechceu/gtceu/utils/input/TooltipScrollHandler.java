package com.gregtechceu.gtceu.utils.input;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TooltipScrollHandler {

    private static final Map<ResourceLocation, TooltipState> TOOLTIP_STATES = new HashMap<>();
    private static @Nullable ResourceLocation CURRENT_TOOLTIP_MACHINE_ID = null;
    private static @Nullable TooltipState CURRENT_TOOLTIP = null;

    public static int getCurrentTooltipPage() {
        if (CURRENT_TOOLTIP == null) return 0;
        return CURRENT_TOOLTIP.page;
    }

    public static int getCurrentTooltipModifier() {
        if (CURRENT_TOOLTIP == null) return 0;
        return CURRENT_TOOLTIP.modifier;
    }

    public static void setCurrentTooltipMachine(MachineDefinition definition) {
        var newId = definition.getId();
        if (CURRENT_TOOLTIP_MACHINE_ID != null && CURRENT_TOOLTIP_MACHINE_ID.equals(newId)) return;

        CURRENT_TOOLTIP_MACHINE_ID = newId;
        CURRENT_TOOLTIP = TOOLTIP_STATES.computeIfAbsent(newId, id -> new TooltipState(0, 0));
    }

    public static <T extends ScreenEvent> void onTooltipNext(T event) {
        if (CURRENT_TOOLTIP == null) return;

        if (GTUtil.isShiftDown()) {
            CURRENT_TOOLTIP.modifier++;
        } else {
            CURRENT_TOOLTIP.page++;
        }
        event.setCanceled(true);
    }

    public static <T extends ScreenEvent> void onTooltipPrev(T event) {
        if (CURRENT_TOOLTIP == null) return;

        if (GTUtil.isShiftDown()) {
            CURRENT_TOOLTIP.modifier--;
        } else {
            CURRENT_TOOLTIP.page--;
        }
        event.setCanceled(true);
    }

    public static void onTooltipEvent(ItemTooltipEvent event) {
        if (CURRENT_TOOLTIP == null || CURRENT_TOOLTIP_MACHINE_ID == null) return;

        if (event.getItemStack().getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() instanceof MetaMachineBlock metaMachineBlock) {
                if (metaMachineBlock.getDefinition().getId().equals(CURRENT_TOOLTIP_MACHINE_ID)) {
                    return;
                }
            }
        }

        CURRENT_TOOLTIP_MACHINE_ID = null;
        CURRENT_TOOLTIP = null;
    }

    public static KeyModifier getRequiredModifierForPageScroll(SyncedKeyMapping mapping) {
        if (mapping.getType() == InputConstants.Type.MOUSE &&
                (mapping.getKeyCode() == SyncedKeyMappings.KEY_MOUSEWHEEL_UP ||
                        mapping.getKeyCode() == SyncedKeyMappings.KEY_MOUSEWHEEL_DOWN)) {
            return KeyModifier.CONTROL;
        } else {
            return KeyModifier.NONE;
        }
    }

    private static class TooltipState {

        public int page;
        public int modifier;

        public TooltipState(int page, int modifier) {
            this.page = page;
            this.modifier = modifier;
        }
    }
}
