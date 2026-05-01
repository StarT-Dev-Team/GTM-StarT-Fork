package com.gregtechceu.gtceu.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class TooltipPageManager {

    private static final Map<ResourceLocation, Integer> PAGE_STATES = new HashMap<>();
    private static final Map<ResourceLocation, Integer> MODIFIER_STATES = new HashMap<>();
    private static final Map<ResourceLocation, Long> LAST_CHANGE_TIMES = new HashMap<>();

    public static int getCurrentPage(ResourceLocation tooltipId) {
        return PAGE_STATES.getOrDefault(tooltipId, 0);
    }

    public static int getCurrentModifierPage(ResourceLocation tooltipId) {
        return MODIFIER_STATES.getOrDefault(tooltipId, 0);
    }

    public static void setCurrentPage(ResourceLocation tooltipId, int page) {
        PAGE_STATES.put(tooltipId, Math.max(0, page));
    }

    public static void setCurrentModifierPage(ResourceLocation tooltipId, int modifier) {
        MODIFIER_STATES.put(tooltipId, Math.max(0, modifier));
    }

    public static Long getLastChangeTime(ResourceLocation tooltipId) {
        return LAST_CHANGE_TIMES.get(tooltipId) != null ? LAST_CHANGE_TIMES.get(tooltipId) : 0;
    }

    public static void setLastChangeTime(ResourceLocation tooltipId, long time) {
        LAST_CHANGE_TIMES.put(tooltipId, time);
    }

    public static void reset(ResourceLocation tooltipId) {
        PAGE_STATES.put(tooltipId, 0);
        LAST_CHANGE_TIMES.remove(tooltipId);
    }
}
