package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.util.UICloseHandler;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelector;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.item.component.IItemLifeCycle;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.tterrag.registrate.util.entry.ItemEntry;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

public class ItemMagnetBehavior implements IInteractionItem, IItemLifeCycle, IAddInformation, IItemUIFactory {

    public static final String FILTER_TAG = "MagnetFilter";
    public static final String FILTER_ORDINAL_TAG = "FilterOrdinal";

    private final int range;
    private final long energyDraw;

    public ItemMagnetBehavior(int range) {
        this.range = range;
        this.energyDraw = GTValues.V[range > 8 ? GTValues.HV : GTValues.LV];
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public ModularUI createUI(HeldItemUIMenuType.HeldItemUIHolder holder, Player entityPlayer) {
        var held = holder.itemStack;
        var tag = held.getOrCreateTag();
        var selected = Filter.get(tag.getInt(FILTER_ORDINAL_TAG));
        var views = new HashMap<Filter, UIElement>();
        var stacks = new HashMap<Filter, ItemStack>();

        var root = UITemplate.panel();
        root.layout(layout -> layout.minWidth(176).paddingAll(5).gapAll(5));

        var selector = new EnumSelector<>(Filter.values(), () -> Filter.get(tag.getInt(FILTER_ORDINAL_TAG)),
                filter -> tag.putInt(FILTER_ORDINAL_TAG, filter.ordinal()));
        var header = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW)
                .justifyContent(AlignContent.FLEX_END));
        header.addChild(selector);
        root.addChild(header);

        for (var f : Filter.values()) {
            var stack = f.getFilter(held);
            stack.setTag(tag.getCompound(FILTER_TAG).copy());
            stacks.put(f, stack);
            var view = new UIElement().layout(layout -> layout.alignItems(AlignItems.CENTER).gapAll(5));
            view.addChild(new Label().setText(Component.translatable(stack.getDescriptionId())));
            view.addChild(ItemFilter.loadFilter(stack).openConfigurator());
            view.setDisplay(f == selected);
            views.put(f, view);
            root.addChild(view);
        }
        root.addEventListener(UIEvents.TICK, event -> {
            var current = selector.getValue();
            views.forEach((filter, view) -> view.setDisplay(filter == current));
        });
        UICloseHandler.onClose(root, () -> {
            var selection = Filter.get(tag.getInt(FILTER_ORDINAL_TAG));
            tag.put(FILTER_TAG, stacks.get(selection).getOrCreateTag());
        });
        root.addChild(UITemplate.playerInventory());
        return UITemplate.modularUI(root, entityPlayer);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level world, @NotNull Player player,
                                                  InteractionHand hand) {
        if (!player.level().isClientSide && player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable(toggleActive(player.getItemInHand(hand)) ?
                    "behavior.item_magnet.enabled" : "behavior.item_magnet.disabled"), true);
        } else {
            IItemUIFactory.super.use(item, world, player, hand);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    private static boolean isActive(ItemStack stack) {
        if (stack == ItemStack.EMPTY) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return false;
        }
        if (tag.contains("IsActive")) {
            return tag.getBoolean("IsActive");
        }
        return false;
    }

    private static boolean toggleActive(ItemStack stack) {
        boolean isActive = isActive(stack);
        // noinspection ConstantConditions
        stack.getOrCreateTag().putBoolean("IsActive", !isActive);
        return !isActive;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // Adapted logic from Draconic Evolution
        // https://github.com/Draconic-Inc/Draconic-Evolution/blob/1.12.2/src/main/java/com/brandon3055/draconicevolution/items/tools/Magnet.java
        if (!entity.isShiftKeyDown() && entity.tickCount % 10 == 0 && isActive(stack) &&
                entity instanceof Player player) {
            Level world = entity.level();
            if (!drainEnergy(true, stack, energyDraw)) {
                return;
            }

            List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class,
                    new AABB(entity.getX(), entity.getY(), entity.getZ(), entity.getX(), entity.getY(), entity.getZ())
                            .inflate(range, range, range));

            ItemFilter filter = null;
            boolean didMoveEntity = false;
            for (ItemEntity itemEntity : items) {
                if (itemEntity.isRemoved()) {
                    continue;
                }

                CompoundTag itemTag = itemEntity.getPersistentData();
                if (itemTag.contains("PreventRemoteMovement")) {
                    continue;
                }

                if (itemEntity.getOwner() != null && itemEntity.getOwner().equals(entity) &&
                        itemEntity.hasPickUpDelay()) {
                    continue;
                }

                Player closest = world.getNearestPlayer(itemEntity, 4);
                if (closest != null && closest != entity) {
                    continue;
                }

                if (!world.isClientSide) {
                    if (filter == null) {
                        filter = Filter.get(stack.getOrCreateTag().getInt(FILTER_ORDINAL_TAG)).loadFilter(stack);
                    }

                    if (!filter.test(itemEntity.getItem())) {
                        continue;
                    }

                    if (itemEntity.hasPickUpDelay()) {
                        itemEntity.setNoPickUpDelay();
                    }
                    itemEntity.setDeltaMovement(0, 0, 0);
                    itemEntity.setPos(entity.getX() - 0.2 + (world.random.nextDouble() * 0.4), entity.getY() - 0.6,
                            entity.getZ() - 0.2 + (world.random.nextDouble() * 0.4));
                    didMoveEntity = true;
                }
            }

            if (didMoveEntity) {
                world.playSound(null, entity, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                        0.1F, 0.5F * ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 2F));
            }

            List<ExperienceOrb> xp = world.getEntitiesOfClass(ExperienceOrb.class,
                    new AABB(entity.getX(), entity.getY(), entity.getZ(), entity.getX(), entity.getY(), entity.getZ())
                            .inflate(4, 4, 4));

            for (ExperienceOrb orb : xp) {
                if (!world.isClientSide && !orb.isRemoved()) {
                    if (player.takeXpDelay == 0) {
                        if (MinecraftForge.EVENT_BUS.post(new PlayerXpEvent.PickupXp(player, orb))) {
                            continue;
                        }
                        world.playSound(null, entity, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                                0.1F, 0.5F * ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.8F));
                        player.take(orb, 1);
                        player.giveExperiencePoints(orb.value);
                        orb.discard();
                        didMoveEntity = true;
                    }
                }
            }

            if (didMoveEntity) {
                drainEnergy(false, stack, energyDraw);
            }
        }
    }

    @SubscribeEvent
    public void onItemToss(@NotNull ItemTossEvent event) {
        if (event.getPlayer() == null) return;
        if (hasMagnet(event.getPlayer())) {
            event.getEntity().setPickUpDelay(60);
        }
    }

    private boolean hasMagnet(@NotNull Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stackInSlot = inventory.getItem(i);
            if (isMagnet(stackInSlot) && isActive(stackInSlot)) {
                return true;
            }
        }

        if (!GTCEu.Mods.isCuriosLoaded()) {
            return false;
        }
        return CuriosUtils.hasMagnetCurios(player);
    }

    private static boolean isMagnet(@NotNull ItemStack stack) {
        if (stack.getItem() instanceof IComponentItem metaItem) {
            for (var behavior : metaItem.getComponents()) {
                if (behavior instanceof ItemMagnetBehavior) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean drainEnergy(boolean simulate, @NotNull ItemStack stack, long amount) {
        IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
        if (electricItem == null)
            return false;

        return electricItem.discharge(amount, Integer.MAX_VALUE, true, false, simulate) >= amount;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> lines,
                                TooltipFlag isAdvanced) {
        lines.add(Component
                .translatable(isActive(itemStack) ? "behavior.item_magnet.enabled" : "behavior.item_magnet.disabled"));
    }

    private static class CuriosUtils {

        public static boolean hasMagnetCurios(Player player) {
            return CuriosApi.getCuriosInventory(player)
                    .map(curios -> curios.findFirstCurio(i -> isMagnet(i) && isActive(i)).isPresent())
                    .orElse(false);
        }
    }

    public enum Filter implements EnumSelector.SelectableEnum {

        SIMPLE(GTItems.ITEM_FILTER, "item_filter"),
        TAG(GTItems.TAG_FILTER, "item_tag_filter");

        public final ItemEntry<ComponentItem> item;
        public final String texture;

        Filter(ItemEntry<ComponentItem> item, String texture) {
            this.item = item;
            this.texture = texture;
        }

        public ItemStack getFilter(ItemStack magnet) {
            var tag = magnet.getOrCreateTag();
            var mockStack = new ItemStack(item);
            mockStack.setTag(tag.getCompound(FILTER_TAG));
            return mockStack;
        }

        public ItemFilter loadFilter(ItemStack magnet) {
            var stack = getFilter(magnet);
            return ItemFilter.loadFilter(stack);
        }

        public static Filter get(int ordinal) {
            return Filter.values()[ordinal];
        }

        @Override
        public @NotNull String getTooltip() {
            return item.asItem().getDescriptionId();
        }

        @Override
        public @NotNull IGuiTexture getIcon() {
            return SpriteTexture.of("gtceu:textures/item/" + texture + ".png");
        }
    }
}
