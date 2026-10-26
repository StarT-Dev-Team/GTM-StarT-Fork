package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMappings;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.settings.KeyModifier;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TerminalBehavior implements IItemUIFactory, IAddInformation {

    private static final String FLIPPED_KEY = "Flipped";
    private static final String NO_HATCHES_KEY = "NoHatches";
    private static final String LAYERS_KEY = "Layers";
    private static final String VARIANT_KEY = "Variant";

    private static final int MAX_LAYERS = 32;
    private static final int MAX_VARIANT = 64;
    private static final int MAX_MISSING_LINES = 5;

    private static boolean isFlipped(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(FLIPPED_KEY);
    }

    private static boolean isNoHatches(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(NO_HATCHES_KEY);
    }

    private static int getLayers(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(LAYERS_KEY) : 0;
    }

    private static int getVariant(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(VARIANT_KEY) : 0;
    }

    private static Component getStateName(boolean enabled) {
        return Component.translatable("cover.voiding.label." + (enabled ? "enabled" : "disabled"));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        int layers = getLayers(stack);

        Component isFlipped = Component.translatable("metaitem.terminal.tooltip.flipped",
                getStateName(isFlipped(stack)));
        Component noHatches = Component.translatable("metaitem.terminal.tooltip.no_hatches",
                getStateName(isNoHatches(stack)));
        Component layerDisplay = Component.translatable("metaitem.terminal.tooltip.layers", layers > 0 ?
                Component.literal(String.valueOf(layers)) : Component.translatable("metaitem.terminal.layers.min"));
        Component variantDisplay = Component.translatable("metaitem.terminal.tooltip.variant",
                Component.literal(String.valueOf(getVariant(stack))));

        if (GTUtil.isShiftDown()) {
            tooltipComponents.add(Component.translatable("metaitem.terminal.help.preview"));
            Component sneakKey = SyncedKeyMappings.VANILLA_SNEAK.getBracketedDisplayName(KeyModifier.NONE);

            tooltipComponents.add(Component.translatable("metaitem.terminal.help.build", sneakKey)
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("metaitem.terminal.help.settings", sneakKey)
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(isFlipped.copy().append(Component.translatable("metaitem.terminal.help.flipped")));
            tooltipComponents.add(noHatches.copy().append(Component.translatable("metaitem.terminal.help.no_hatches")));
            tooltipComponents.add(layerDisplay.copy().append(Component.translatable("metaitem.terminal.help.layers")));
            tooltipComponents
                    .add(variantDisplay.copy().append(Component.translatable("metaitem.terminal.help.variant")));
        } else {
            tooltipComponents.add(isFlipped);
            tooltipComponents.add(noHatches);
            tooltipComponents.add(layerDisplay);
            tooltipComponents.add(variantDisplay);
            tooltipComponents.add(Component.translatable("metaitem.terminal.tooltip.hold_shift"));
        }
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
                int layers = getLayers(stack);
                int variant = getVariant(stack);

                MultiblockInWorldPreviewRenderer.showPreview(blockPos, controller.self(),
                        ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20,
                        isFlipped(stack) && controller.self().allowFlip(), layers, variant);
                displayRange(player, controller, layers, variant);
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
                        ItemStack stack = context.getItemInHand();
                        boolean noHatches = isNoHatches(stack);
                        int layers = getLayers(stack);

                        controller.self().setFlipped(isFlipped(stack) && controller.self().allowFlip());
                        displayBuildResult(context.getPlayer(),
                                controller.getPattern(layers).autoBuild(context.getPlayer(),
                                        controller.getMultiblockState(), noHatches, layers),
                                noHatches);
                    }

                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(player.getItemInHand(usedHand));

        return IItemUIFactory.super.use(item, level, player, usedHand);
    }

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player player) {
        return new ModularUI(176, 152, holder, player).background(GuiTextures.BACKGROUND)
                .widget(new LabelWidget(8, 6, "metaitem.terminal.ui.title"))
                .widget(createInfoIcon(27, "metaitem.terminal.help.flipped"))
                .widget(new LabelWidget(20, 28, "metaitem.terminal.ui.flipped"))
                .widget(createToggle(holder, 22, FLIPPED_KEY, "metaitem.terminal.ui.flipped.tooltip"))
                .widget(createInfoIcon(51, "metaitem.terminal.help.no_hatches"))
                .widget(new LabelWidget(20, 52, "metaitem.terminal.ui.no_hatches"))
                .widget(createToggle(holder, 46, NO_HATCHES_KEY, "metaitem.terminal.ui.no_hatches.tooltip"))
                .widget(createInfoIcon(75, "metaitem.terminal.help.layers"))
                .widget(new LabelWidget(20, 76, "metaitem.terminal.ui.layers"))
                .widget(new IntInputWidget(8, 88, 160, 20, () -> getLayers(holder.getHeld()), value -> {
                    holder.getHeld().getOrCreateTag().putInt(LAYERS_KEY, value);
                    holder.markAsDirty();
                }).setMin(0).setMax(MAX_LAYERS))
                .widget(createInfoIcon(111, "metaitem.terminal.help.variant"))
                .widget(new LabelWidget(20, 112, "metaitem.terminal.ui.variant"))
                .widget(new IntInputWidget(8, 124, 160, 20, () -> getVariant(holder.getHeld()), value -> {
                    holder.getHeld().getOrCreateTag().putInt(VARIANT_KEY, value);
                    holder.markAsDirty();
                }).setMin(0).setMax(MAX_VARIANT));
    }

    private static Widget createInfoIcon(int y, String info) {
        return new ImageWidget(8, y, 10, 10, GuiTextures.INFO_ICON).setHoverTooltips(info);
    }

    private static ToggleButtonWidget createToggle(HeldItemUIFactory.HeldItemHolder holder, int y, String key,
                                                   String tooltip) {
        ToggleButtonWidget toggle = new ToggleButtonWidget(116, y, 52, 20,
                () -> holder.getHeld().hasTag() && holder.getHeld().getTag().getBoolean(key), value -> {
                    holder.getHeld().getOrCreateTag().putBoolean(key, value);
                    holder.markAsDirty();
                });

        toggle.setTexture(createToggleTexture(false), createToggleTexture(true));
        toggle.setHoverTooltips(tooltip);

        return toggle;
    }

    private static IGuiTexture createToggleTexture(boolean on) {
        return new GuiTextureGroup(GuiTextures.VANILLA_BUTTON,
                new TextTexture(Component.translatable("cover.voiding.label." + (on ? "enabled" : "disabled"))
                        .getString()));
    }

    private static void displayRange(Player player, IMultiController controller, int layers, int variant) {
        int[] layerRange = controller.getLayerRange();
        int variantCount = controller.self().getDefinition().getVariantCount();

        // layers take priority over the variant, same as in the preview
        if (layerRange != null && (layers > 0 || variantCount == 1)) {
            displayRange(player, "metaitem.terminal.layers.label",
                    layers <= 0 ? layerRange[0] : Mth.clamp(layers, layerRange[0], layerRange[1]), layerRange[0],
                    layerRange[1]);
        } else if (variantCount > 1) {
            displayRange(player, "metaitem.terminal.variant.label", Mth.clamp(variant, 0, variantCount - 1), 0,
                    variantCount - 1);
        }
    }

    private static void displayRange(Player player, String labelKey, int value, int min, int max) {
        player.displayClientMessage(Component.translatable(labelKey)
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" " + value).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + min + "-" + max + ")").withStyle(ChatFormatting.GRAY)),
                true);
    }

    private static void displayBuildResult(Player player, BlockPattern.BuildResult result, boolean noHatches) {
        if (result.missing().isEmpty()) {
            if (result.placed() > 0) {
                player.displayClientMessage(Component.translatable(
                        noHatches ? "metaitem.terminal.build.success.no_hatches" : "metaitem.terminal.build.success",
                        result.placed()).withStyle(ChatFormatting.GREEN), true);
            } else if (result.failed() == 0) {
                player.displayClientMessage(Component.translatable("metaitem.terminal.build.nothing")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
        } else {
            player.sendSystemMessage(result.placed() == 0 ?
                    Component.translatable("metaitem.terminal.build.no_blocks").withStyle(ChatFormatting.RED) :
                    Component.translatable("metaitem.terminal.build.partial", result.placed())
                            .withStyle(ChatFormatting.RED));
            result.missing().entrySet().stream().limit(MAX_MISSING_LINES)
                    .forEach(entry -> player.sendSystemMessage(Component
                            .translatable("metaitem.terminal.build.missing_entry", entry.getValue(), entry.getKey())
                            .withStyle(ChatFormatting.GRAY)));

            int more = result.missing().size() - MAX_MISSING_LINES;

            if (more > 0) {
                player.sendSystemMessage(Component.translatable("metaitem.terminal.build.missing_more", more)
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        if (result.failed() > 0) {
            player.sendSystemMessage(Component.translatable("metaitem.terminal.build.obstructed", result.failed())
                    .withStyle(ChatFormatting.YELLOW));
        }
    }
}
