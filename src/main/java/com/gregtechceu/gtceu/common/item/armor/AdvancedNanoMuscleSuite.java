package com.gregtechceu.gtceu.common.item.armor;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.api.item.armor.ArmorUtils;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMappings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AdvancedNanoMuscleSuite extends NanoMuscleSuite implements IElectricJetpack {

    // A replacement for checking the current world time, to get around the gamerule that stops it
    private long timer = 0L;
    private List<Pair<NonNullList<ItemStack>, IntList>> inventoryIndexMap;

    public AdvancedNanoMuscleSuite(int energyPerUse, long capacity, int tier) {
        super(ArmorItem.Type.CHESTPLATE, energyPerUse, capacity, tier);
    }

    @Override
    public void onArmorTick(Level world, Player player, @NotNull ItemStack item) {
        IElectricItem cont = GTCapabilityHelper.getElectricItem(item);
        if (cont == null) {
            return;
        }

        CompoundTag data = item.getOrCreateTag();
        if (!data.contains("canShare")) data.putBoolean("canShare", false);

        this.tickJetpack(world, player, item, (p, tag) -> {
            if (!SyncedKeyMappings.ARMOR_CHARGING.isKeyDown(p)) return null;
            boolean share = !tag.getBoolean("canShare");
            String key;
            if (share && cont.getCharge() == 0) {
                key = "metaarmor.nms.share.error";
                share = false;
            } else {
                key = "metaarmor.nms.share." + (share ? "enable" : "disable");
            }
            tag.putBoolean("canShare", share);
            return key;
        });
        boolean canShare = data.getBoolean("canShare");

        // Charging mechanics
        if (canShare && !world.isClientSide) {
            // Check for new things to charge every 5 seconds
            if (timer % 100 == 0)
                inventoryIndexMap = ArmorUtils.getChargeableItem(player, cont.getTier());

            if (inventoryIndexMap != null && !inventoryIndexMap.isEmpty()) {
                // Charge all inventory slots
                for (int i = 0; i < inventoryIndexMap.size(); i++) {
                    Pair<NonNullList<ItemStack>, IntList> inventoryMap = inventoryIndexMap.get(i);
                    var inventoryIterator = inventoryMap.getSecond().iterator();
                    while (inventoryIterator.hasNext()) {
                        int slot = inventoryIterator.nextInt();
                        IElectricItem chargable = GTCapabilityHelper.getElectricItem(inventoryMap.getFirst().get(slot));

                        // Safety check the null, it should not actually happen. Also don't try and charge itself
                        if (chargable == null || chargable == cont) {
                            inventoryIterator.remove();
                            continue;
                        }

                        long attemptedChargeAmount = chargable.getTransferLimit() * 10;

                        // Accounts for tick differences when charging items
                        if (chargable.getCharge() < chargable.getMaxCharge() && cont.canUse(attemptedChargeAmount) &&
                                timer % 10 == 0) {
                            long delta = chargable.charge(attemptedChargeAmount, cont.getTier(), true, false);
                            if (delta > 0) {
                                cont.discharge(delta, cont.getTier(), true, false, false);
                            }
                            if (chargable.getCharge() == chargable.getMaxCharge()) {
                                inventoryIterator.remove();
                            }
                            player.inventoryMenu.sendAllDataToRemote();
                        }
                    }

                    if (inventoryMap.getSecond().isEmpty()) inventoryIndexMap.remove(inventoryMap);
                }
            }
        }

        timer++;
        if (timer == Long.MAX_VALUE)
            timer = 0;
    }

    @Override
    public void addInfo(ItemStack itemStack, List<Component> lines) {
        super.addInfo(itemStack, lines);
        CompoundTag data = itemStack.getOrCreateTag();
        lines.add(JetpackHelper.engineLine(data));
        lines.add(Component.translatable("metaarmor.energy_share.tooltip",
                JetpackHelper.statusComponent(data.getBoolean("canShare"))));
        lines.add(Component.translatable("metaarmor.energy_share.tooltip.guide"));
        lines.add(JetpackHelper.hoverLine(data));
    }

    @Override
    public InteractionResultHolder<ItemStack> onRightClick(Level world, @NotNull Player player, InteractionHand hand) {
        ItemStack armor = player.getItemInHand(hand);

        if (armor.getItem() instanceof ArmorComponentItem && player.isShiftKeyDown()) {
            CompoundTag data = armor.getOrCreateTag();
            boolean canShare = data.contains("canShare") && data.getBoolean("canShare");
            IElectricItem cont = GTCapabilityHelper.getElectricItem(armor);
            if (cont == null) {
                return InteractionResultHolder.fail(armor);
            }

            canShare = !canShare;
            if (!world.isClientSide) {
                if (canShare && cont.getCharge() == 0) {
                    player.sendSystemMessage(Component.translatable("metaarmor.energy_share.error"));
                } else if (canShare) {
                    player.sendSystemMessage(Component.translatable("metaarmor.energy_share.enable"));
                } else {
                    player.sendSystemMessage(Component.translatable("metaarmor.energy_share.disable"));
                }
            }

            canShare = canShare && (cont.getCharge() != 0);
            data.putBoolean("canShare", canShare);
            return InteractionResultHolder.success(armor);
        }

        return super.onRightClick(world, player, hand);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawHUD(ItemStack item, GuiGraphics guiGraphics) {
        addCapacityHUD(item, this.HUD);
        IElectricItem cont = GTCapabilityHelper.getElectricItem(item);
        if (cont == null) return;
        if (!cont.canUse(energyPerUse)) return;
        CompoundTag data = item.getTag();
        JetpackHelper.addEngineHUD(this.HUD, data);
        if (data != null && data.contains("canShare")) {
            this.HUD.newString(Component.translatable("mataarmor.hud.supply_mode",
                    JetpackHelper.statusComponent(data.getBoolean("canShare"))));
        }
        JetpackHelper.addHoverHUD(this.HUD, data);
        this.HUD.draw(guiGraphics);
        this.HUD.reset();
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return GTCEu.id("textures/armor/advanced_nano_muscle_suite_1.png");
    }

    @Override
    public JetpackStats getJetpackStats() {
        return JetpackStats.NANO_SUITE;
    }

    @Override
    public boolean isPPE() {
        return true;
    }
}
