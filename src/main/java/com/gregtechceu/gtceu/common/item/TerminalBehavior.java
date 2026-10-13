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

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TerminalBehavior implements IInteractionItem, IAddInformation {

    private static TerminalMode getMode(ItemStack stack) {
        var modes = TerminalMode.values();

        return stack.hasTag() ? modes[stack.getTag().getByte("Mode") % modes.length] : TerminalMode.NORMAL;
    }

    private static Component modeLine(ItemStack stack) {
        return Component.translatable("metaitem.terminal.mode", getMode(stack).getName());
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
                        getMode(stack).isFlipped() && controller.self().allowFlip());
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
                        var mode = getMode(context.getItemInHand());

                        controller.self().setFlipped(mode.isFlipped() && controller.self().allowFlip());
                        controller.getPattern().autoBuild(context.getPlayer(), controller.getMultiblockState(),
                                mode.isNoHatches());
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

        heldItem.getOrCreateTag().putByte("Mode",
                (byte) ((getMode(heldItem).ordinal() + 1) % TerminalMode.values().length));
        player.displayClientMessage(modeLine(heldItem), true);

        return InteractionResultHolder.success(heldItem);
    }

    @Getter
    public enum TerminalMode {

        NORMAL(Component.translatable("metaitem.terminal.mode.normal")),
        FLIPPED(Component.translatable("metaitem.terminal.mode.flipped")),
        NO_HATCHES(Component.translatable("metaitem.terminal.mode.no_hatches")),
        FLIPPED_NO_HATCHES(Component.translatable("metaitem.terminal.mode.flipped_no_hatches"));

        private final Component name;

        TerminalMode(Component name) {
            this.name = name;
        }

        public boolean isFlipped() {
            return this == FLIPPED || this == FLIPPED_NO_HATCHES;
        }

        public boolean isNoHatches() {
            return this == NO_HATCHES || this == FLIPPED_NO_HATCHES;
        }
    }
}
