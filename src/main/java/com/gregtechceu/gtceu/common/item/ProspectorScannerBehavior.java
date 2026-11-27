package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.misc.ProspectorMode;
import com.gregtechceu.gtceu.api.gui.texture.UVSpriteTexture;
import com.gregtechceu.gtceu.api.gui.widget.ProspectingMap;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ProspectorScannerBehavior implements IItemUIFactory, IInteractionItem, IAddInformation {

    private final int radius;
    private final long cost;
    private final ProspectorMode<?>[] modes;

    public ProspectorScannerBehavior(int radius, long cost, ProspectorMode<?>... modes) {
        this.radius = radius + 1;
        this.modes = Arrays.stream(modes).filter(Objects::nonNull).toArray(ProspectorMode[]::new);
        this.cost = cost;
    }

    @NotNull
    public ProspectorMode<?> getMode(ItemStack stack) {
        if (stack == ItemStack.EMPTY) {
            return modes[0];
        }
        var tag = stack.getTag();
        if (tag == null) {
            return modes[0];
        }
        return modes[tag.getInt("Mode") % modes.length];
    }

    public void setNextMode(ItemStack stack) {
        var tag = stack.getOrCreateTag();
        tag.putInt("Mode", (tag.getInt("Mode") + 1) % modes.length);
    }

    public boolean drainEnergy(@NotNull ItemStack stack, boolean simulate) {
        IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
        if (electricItem == null) return false;

        int amount = Math.round(cost * (ConfigHolder.INSTANCE.machines.prospectorEnergyUseMultiplier / 100F));

        return electricItem.discharge(amount, Integer.MAX_VALUE, true, false, simulate) >= amount;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack heldItem = player.getItemInHand(usedHand);
        if (player.isShiftKeyDown() && modes.length > 1) {
            if (!level.isClientSide) {
                setNextMode(heldItem);
                var mode = getMode(heldItem);
                player.sendSystemMessage(Component.translatable(mode.unlocalizedName));
            }
            return InteractionResultHolder.success(heldItem);
        }
        if (!player.isCreative() && !drainEnergy(heldItem, true)) {
            player.sendSystemMessage(Component.translatable("behavior.prospector.not_enough_energy"));
            return InteractionResultHolder.success(heldItem);
        }
        return IItemUIFactory.super.use(item, level, player, usedHand);
    }

    @Override
    public ModularUI createUI(HeldItemUIMenuType.HeldItemUIHolder holder, Player entityPlayer) {
        var mode = getMode(entityPlayer.getItemInHand(InteractionHand.MAIN_HAND));
        var map = new ProspectingMap(entityPlayer, radius, mode, 1);

        var root = UITemplate.panel();
        root.layout(layout -> layout.width(332).height(200).paddingAll(4));
        map.layout(layout -> layout.widthPercent(100).heightPercent(100));
        root.addChild(map);

        var steam = GuiTextures.PROGRESS_BAR_SOLAR_STEAM.get(true);
        var light = IGuiTexture.group(GuiTextures.BUTTON, UVSpriteTexture.of(steam, 0, 0.5f, 1, 0.5f).scale(0.8f));
        var dark = IGuiTexture.group(GuiTextures.BUTTON, UVSpriteTexture.of(steam, 0, 0, 1, 0.5f).scale(0.8f));
        var toggle = new Button().noText();
        toggle.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(-20).top(4).width(18).height(18));
        var texture = DynamicTexture.of(() -> map.isDarkMode() ? dark : light);
        toggle.buttonStyle(style -> style.baseTexture(texture).hoverTexture(texture).pressedTexture(texture));
        toggle.setOnClick(event -> map.setDarkMode(!map.isDarkMode()));
        root.addChild(toggle);

        return UITemplate.modularUI(root, entityPlayer);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("metaitem.prospector.tooltip.radius", radius));
        tooltipComponents.add(Component.translatable("metaitem.prospector.tooltip.modes"));
        for (ProspectorMode<?> mode : modes) {
            tooltipComponents.add(Component.literal(" -").append(Component.translatable(mode.unlocalizedName))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        }
    }
}
