package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.IUICover;

import com.lowdragmc.lowdraglib2.gui.factory.ContainerUIOpener;
import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.jetbrains.annotations.Nullable;

public class CoverUIFactory {

    public static final CoverUIFactory INSTANCE = new CoverUIFactory();

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            GTCEu.MOD_ID);

    public static final RegistryObject<MenuType<ModularUIContainerMenu>> COVER_UI = MENUS.register("cover_ui",
            () -> IForgeMenuType.create(CoverUIFactory::createMenu));

    private CoverUIFactory() {}

    public static void init(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerScreens() {
        MenuScreens.register(COVER_UI.get(), ModularUIContainerScreen::new);
    }

    public boolean openUI(CoverBehavior cover, ServerPlayer player) {
        if (!(cover instanceof IUICover)) return false;
        return ContainerUIOpener.open(player, new CoverUIHolder(cover, player));
    }

    private static ModularUIContainerMenu createMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        var pos = data.readBlockPos();
        var side = data.readEnum(Direction.class);
        var cover = findCover(inventory.player, pos, side);
        if (cover == null) {
            throw new IllegalArgumentException("No cover ui found at " + pos + " " + side);
        }
        return new ModularUIContainerMenu(COVER_UI.get(), windowId, inventory, new CoverUIHolder(cover,
                inventory.player));
    }

    @Nullable
    private static CoverBehavior findCover(Player player, BlockPos pos, Direction side) {
        var coverable = GTCapabilityHelper.getCoverable(player.level(), pos, side);
        return coverable == null ? null : coverable.getCoverAtSide(side);
    }

    public static class CoverUIHolder implements MenuProvider, IContainerUIHolder {

        public final CoverBehavior cover;
        public final Player player;

        public CoverUIHolder(CoverBehavior cover, Player player) {
            this.cover = cover;
            this.player = player;
        }

        @Override
        public ModularUI createUI(Player player) {
            if (cover instanceof IUICover uiCover) {
                return uiCover.createUI(player);
            }
            return new ModularUI(UI.empty(), player);
        }

        @Override
        public boolean isStillValid(Player player) {
            return cover instanceof IUICover uiCover && uiCover.isStillValid();
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, FriendlyByteBuf buffer) {
            buffer.writeBlockPos(cover.coverHolder.getPos());
            buffer.writeEnum(cover.attachedSide);
        }

        @Override
        public Component getDisplayName() {
            return cover.getPickItem().getHoverName();
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new ModularUIContainerMenu(COVER_UI.get(), containerId, inventory, this);
        }
    }
}
