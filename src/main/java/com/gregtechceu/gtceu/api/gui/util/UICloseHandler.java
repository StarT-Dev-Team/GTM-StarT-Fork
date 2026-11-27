package com.gregtechceu.gtceu.api.gui.util;

import com.gregtechceu.gtceu.GTCEu;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = GTCEu.MOD_ID)
public final class UICloseHandler {

    private static final Map<UIElement, List<Runnable>> SERVER_LISTENERS = Collections
            .synchronizedMap(new WeakHashMap<>());

    private UICloseHandler() {}

    public static void onClose(UIElement root, Runnable listener) {
        root.addEventListener(UIEvents.REMOVED, e -> {
            if (e.target == root) {
                listener.run();
            }
        });
        if (!LDLib2.isRemote()) {
            SERVER_LISTENERS.computeIfAbsent(root, r -> new ArrayList<>()).add(listener);
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getContainer() instanceof ModularUIContainerMenu menu) {
            var listeners = SERVER_LISTENERS.remove(menu.getModularUI().ui.rootElement);
            if (listeners != null) {
                listeners.forEach(Runnable::run);
            }
        }
    }
}
