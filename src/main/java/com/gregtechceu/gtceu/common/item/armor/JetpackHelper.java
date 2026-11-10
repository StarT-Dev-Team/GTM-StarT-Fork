package com.gregtechceu.gtceu.common.item.armor;

import com.gregtechceu.gtceu.api.item.armor.ArmorUtils;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMappings;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class JetpackHelper {

    public static final byte TOGGLE_COOLDOWN = 5;

    private JetpackHelper() {}

    @FunctionalInterface
    public interface ExtraToggle {

        @Nullable
        String handle(Player player, CompoundTag data);
    }

    public static long scaleCapacity(long baseCapacity, int baseTier, int tier) {
        return baseCapacity * (long) Math.max(1, Math.pow(4, tier - baseTier));
    }

    public static void initTag(CompoundTag data) {
        if (!data.contains("enabled")) {
            data.putBoolean("enabled", true);
            data.putBoolean("hover", false);
            data.putByte("toggleTimer", (byte) 0);
        }
    }

    public static void tickToggles(Level level, Player player, CompoundTag data, @Nullable ExtraToggle extra) {
        byte toggleTimer = data.getByte("toggleTimer");
        if (toggleTimer == 0) {
            String messageKey = null;
            if (SyncedKeyMappings.JETPACK_ENABLE.isKeyDown(player)) {
                boolean enabled = !data.getBoolean("enabled");
                data.putBoolean("enabled", enabled);
                messageKey = "metaarmor.jetpack.flight." + (enabled ? "enable" : "disable");
            } else if (SyncedKeyMappings.ARMOR_HOVER.isKeyDown(player)) {
                boolean hover = !data.getBoolean("hover");
                data.putBoolean("hover", hover);
                messageKey = "metaarmor.jetpack.hover." + (hover ? "enable" : "disable");
            } else if (extra != null) {
                messageKey = extra.handle(player, data);
            }

            if (messageKey != null) {
                toggleTimer = TOGGLE_COOLDOWN;
                if (!level.isClientSide) player.displayClientMessage(Component.translatable(messageKey), true);
            }
        }

        if (toggleTimer > 0) toggleTimer--;
        data.putByte("toggleTimer", toggleTimer);
    }

    public static Component statusComponent(boolean on) {
        return Component.translatable(on ? "metaarmor.hud.status.enabled" : "metaarmor.hud.status.disabled");
    }

    public static Component engineLine(CompoundTag data) {
        boolean enabled = !data.contains("enabled") || data.getBoolean("enabled");
        return Component.translatable("metaarmor.hud.engine_enabled", statusComponent(enabled));
    }

    public static Component hoverLine(CompoundTag data) {
        return Component.translatable("metaarmor.hud.hover_mode", statusComponent(data.getBoolean("hover")));
    }

    public static void addTooltip(List<Component> lines, CompoundTag data) {
        lines.add(engineLine(data));
        lines.add(hoverLine(data));
    }

    @OnlyIn(Dist.CLIENT)
    public static void addEngineHUD(ArmorUtils.ModularHUD hud, @Nullable CompoundTag data) {
        if (data != null && data.contains("enabled")) {
            hud.newString(Component.translatable("metaarmor.hud.engine_enabled",
                    statusComponent(data.getBoolean("enabled"))));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void addHoverHUD(ArmorUtils.ModularHUD hud, @Nullable CompoundTag data) {
        if (data != null && data.contains("hover")) {
            hud.newString(hoverLine(data));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void addStatusHUD(ArmorUtils.ModularHUD hud, @Nullable CompoundTag data) {
        addEngineHUD(hud, data);
        addHoverHUD(hud, data);
    }
}
