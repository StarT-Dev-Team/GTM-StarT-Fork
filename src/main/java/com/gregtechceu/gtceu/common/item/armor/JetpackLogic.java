package com.gregtechceu.gtceu.common.item.armor;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.item.armor.ArmorLogicSuite;
import com.gregtechceu.gtceu.api.item.armor.ArmorUtils;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JetpackLogic extends ArmorLogicSuite implements IElectricJetpack {

    private final JetpackStats stats;
    private final ResourceLocation texture;

    @OnlyIn(Dist.CLIENT)
    protected ArmorUtils.ModularHUD HUD;

    public JetpackLogic(int energyPerUse, long capacity, int tier) {
        this(energyPerUse, capacity, tier, JetpackStats.ELECTRIC, GTCEu.id("textures/armor/jetpack.png"));
    }

    public JetpackLogic(int energyPerUse, long capacity, int tier, JetpackStats stats, ResourceLocation texture) {
        super(energyPerUse, capacity, tier, ArmorItem.Type.CHESTPLATE);
        this.stats = stats;
        this.texture = texture;
        if (GTCEu.isClientSide() && this.shouldDrawHUD()) {
            HUD = new ArmorUtils.ModularHUD();
        }
    }

    @Override
    public JetpackStats getJetpackStats() {
        return stats;
    }

    @Override
    public void onArmorTick(Level world, Player player, @NotNull ItemStack item) {
        if (GTCapabilityHelper.getElectricItem(item) == null) {
            return;
        }
        tickJetpack(world, player, item, null);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return texture;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawHUD(ItemStack item, GuiGraphics guiGraphics) {
        addCapacityHUD(item, this.HUD);
        JetpackHelper.addStatusHUD(this.HUD, item.getTag());
        this.HUD.draw(guiGraphics);
        this.HUD.reset();
    }

    @Override
    public void addInfo(ItemStack itemStack, List<Component> lines) {
        super.addInfo(itemStack, lines);
        JetpackHelper.addTooltip(lines, itemStack.getOrCreateTag());
    }
}
