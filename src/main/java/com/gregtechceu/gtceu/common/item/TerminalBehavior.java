package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMappings;

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

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

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

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
    public ModularUI createUI(HeldItemUIMenuType.HeldItemUIHolder holder, Player player) {
        Supplier<ItemStack> held = () -> player.getItemInHand(holder.hand);
        var root = UITemplate.panel();
        root.layout(layout -> layout.minWidth(176).paddingAll(8).gapAll(4));

        root.addChild(new Label().setText(Component.translatable("metaitem.terminal.ui.title")));

        root.addChild(toggleRow(held, "metaitem.terminal.help.flipped", "metaitem.terminal.ui.flipped", FLIPPED_KEY,
                "metaitem.terminal.ui.flipped.tooltip"));
        root.addChild(toggleRow(held, "metaitem.terminal.help.no_hatches", "metaitem.terminal.ui.no_hatches",
                NO_HATCHES_KEY, "metaitem.terminal.ui.no_hatches.tooltip"));

        root.addChild(labelRow("metaitem.terminal.help.layers", "metaitem.terminal.ui.layers"));
        root.addChild(new IntInput(() -> getLayers(held.get()),
                value -> held.get().getOrCreateTag().putInt(LAYERS_KEY, value)).setMin(0).setMax(MAX_LAYERS)
                .layout(layout -> layout.widthPercent(100).height(20)));

        root.addChild(labelRow("metaitem.terminal.help.variant", "metaitem.terminal.ui.variant"));
        root.addChild(new IntInput(() -> getVariant(held.get()),
                value -> held.get().getOrCreateTag().putInt(VARIANT_KEY, value)).setMin(0).setMax(MAX_VARIANT)
                .layout(layout -> layout.widthPercent(100).height(20)));

        return UITemplate.modularUI(root, player);
    }

    private static UIElement labelRow(String info, String label) {
        var row = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(2)
                .alignItems(AlignItems.CENTER));
        row.addChild(createInfoIcon(info));
        row.addChild(new Label().setText(Component.translatable(label)));
        return row;
    }

    private static UIElement toggleRow(Supplier<ItemStack> held, String info, String label, String key,
                                       String tooltip) {
        var row = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).gapAll(2)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.SPACE_BETWEEN));
        var state = new MutableBoolean(isSet(held.get(), key));
        var sync = new BindableValue<Boolean>().bind(DataBindingBuilder.boolS2C(() -> isSet(held.get(), key))
                .remoteSetter(state::setValue)
                .build());
        sync.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE));
        row.addChildren(labelRow(info, label), createToggle(held, state, key, tooltip), sync);
        return row;
    }

    private static UIElement createInfoIcon(String info) {
        var icon = new UIElement().layout(layout -> layout.width(10).height(10));
        icon.style(style -> style.backgroundTexture(GuiTextures.INFO_ICON).tooltips(info));
        return icon;
    }

    private static Button createToggle(Supplier<ItemStack> held, MutableBoolean state, String key, String tooltip) {
        var toggle = new Button().noText();
        toggle.layout(layout -> layout.width(52).height(20));
        var off = createToggleTexture(false);
        var on = createToggleTexture(true);
        var texture = DynamicTexture.of(() -> state.booleanValue() ? on : off);
        toggle.buttonStyle(style -> style.baseTexture(texture).hoverTexture(texture).pressedTexture(texture));
        toggle.style(style -> style.tooltips(tooltip));
        toggle.setOnServerClick(event -> {
            var stack = held.get();
            stack.getOrCreateTag().putBoolean(key, !isSet(stack, key));
        });
        return toggle;
    }

    private static boolean isSet(ItemStack stack, String key) {
        return stack.hasTag() && stack.getTag().getBoolean(key);
    }

    private static IGuiTexture createToggleTexture(boolean on) {
        return IGuiTexture.group(GuiTextures.VANILLA_BUTTON,
                new TextTexture("cover.voiding.label." + (on ? "enabled" : "disabled")));
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
