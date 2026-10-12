package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TerminalBehavior implements IInteractionItem, IAddInformation {

    private static final String FLIPPED_KEY = "flipped";

    private static boolean isFlippedMode(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(FLIPPED_KEY);
    }

    private static Component modeLine(ItemStack stack) {
        return Component.translatable("metaitem.terminal.mode", Component.translatable(
                isFlippedMode(stack) ? "metaitem.terminal.mode.flipped" : "metaitem.terminal.mode.normal"));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        tooltipComponents.add(modeLine(stack));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();

        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();

        if (MetaMachine.getMachine(level, blockPos) instanceof IMultiController controller &&
                !controller.isFormed()) {
            if (level.isClientSide) {
                MultiblockInWorldPreviewRenderer.showPreview(blockPos, controller.self(),
                        ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20,
                        isFlippedMode(stack) && controller.self().allowFlip());
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos blockPos = context.getClickedPos();
            if (context.getPlayer() != null &&
                    MetaMachine.getMachine(level, blockPos) instanceof IMultiController controller) {
                if (!controller.isFormed()) {
                    if (!level.isClientSide) {
                        controller.self().setFlipped(
                                isFlippedMode(context.getItemInHand()) && controller.self().allowFlip());
                        controller.getPattern().autoBuild(context.getPlayer(), controller.getMultiblockState());
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack heldItem = player.getItemInHand(usedHand);

        if (!(level instanceof ServerLevel) || !player.isShiftKeyDown()) return InteractionResultHolder.pass(heldItem);

        heldItem.getOrCreateTag().putBoolean(FLIPPED_KEY, !isFlippedMode(heldItem));
        player.displayClientMessage(modeLine(heldItem), true);

        return InteractionResultHolder.success(heldItem);
    }
}
