package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IAutoOutputBoth;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BufferMachine extends TieredMachine implements IMachineLife, IAutoOutputBoth, IFancyUIMachine {

    public static final int TANK_SIZE = 64000;

    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected Direction outputFacingItems;
    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected Direction outputFacingFluids;
    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected boolean autoOutputItems;
    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected boolean autoOutputFluids;
    @Getter
    @Setter
    @Persisted
    protected boolean allowInputFromOutputSideItems;
    @Getter
    @Setter
    @Persisted
    protected boolean allowInputFromOutputSideFluids;

    @Persisted
    @Getter
    protected final NotifiableItemStackHandler inventory;

    @Persisted
    @Getter
    protected final NotifiableFluidTank tank;

    @Nullable
    protected TickableSubscription autoOutputSubs;

    @Nullable
    protected ISubscription invSubs, tankSubs;

    public BufferMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier);
        this.inventory = createInventory(args);
        this.tank = createTank(args);
    }

    ////////////////////////////////
    // ***** Initialization ******//
    ////////////////////////////////

    public static int getInventorySize(int tier) {
        return (int) Math.pow(tier + 2, 2);
    }

    public static int getTankSize(int tier) {
        return tier + 2;
    }

    protected NotifiableItemStackHandler createInventory(Object... args) {
        return new NotifiableItemStackHandler(this, getInventorySize(tier), IO.BOTH);
    }

    protected NotifiableFluidTank createTank(Object... args) {
        return new NotifiableFluidTank(this, getTankSize(tier), TANK_SIZE, IO.BOTH);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateAutoOutputSubscription));
        }
        this.invSubs = inventory.addChangedListener(this::updateAutoOutputSubscription);
        this.tankSubs = tank.addChangedListener(this::updateAutoOutputSubscription);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (invSubs != null) {
            invSubs.unsubscribe();
            this.invSubs = null;
        }

        if (tankSubs != null) {
            tankSubs.unsubscribe();
            this.tankSubs = null;
        }
    }

    ////////////////////////////////
    // ******* Auto Output *******//
    ////////////////////////////////

    @Override
    public void setAutoOutputFluids(boolean allow) {
        this.autoOutputFluids = allow;
        updateAutoOutputSubscription();
    }

    @Override
    public void setOutputFacingFluids(@Nullable Direction outputFacing) {
        this.outputFacingFluids = outputFacing;
        updateAutoOutputSubscription();
    }

    @Override
    public void setAutoOutputItems(boolean allow) {
        this.autoOutputItems = allow;
        updateAutoOutputSubscription();
    }

    @Override
    public void setOutputFacingItems(@Nullable Direction outputFacing) {
        this.outputFacingItems = outputFacing;
        updateAutoOutputSubscription();
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateAutoOutputSubscription();
    }

    protected void updateAutoOutputSubscription() {
        var outputFacingItems = getOutputFacingItems();
        var outputFacingFluids = getOutputFacingFluids();
        if ((isAutoOutputItems() && !inventory.isEmpty() && outputFacingItems != null &&
                GTTransferUtils.hasAdjacentItemHandler(getLevel(), getPos(), outputFacingItems)) ||
                (isAutoOutputFluids() && !tank.isEmpty() && outputFacingFluids != null &&
                        GTTransferUtils.hasAdjacentFluidHandler(getLevel(), getPos(), outputFacingFluids))) {
            autoOutputSubs = subscribeServerTick(autoOutputSubs, this::autoOutput);
        } else if (autoOutputSubs != null) {
            autoOutputSubs.unsubscribe();
            autoOutputSubs = null;
        }
    }

    protected void autoOutput() {
        if (getOffsetTimer() % 5 == 0) {
            if (isAutoOutputFluids() && getOutputFacingFluids() != null) {
                tank.exportToNearby(getOutputFacingFluids());
            }
            if (isAutoOutputItems() && getOutputFacingItems() != null) {
                inventory.exportToNearby(getOutputFacingItems());
            }
        }
        updateAutoOutputSubscription();
    }

    ////////////////////////////////
    // ********** GUI *********** //
    ////////////////////////////////

    @Override
    public UIElement createUIWidget() {
        int invTier = getTankSize(tier);
        var container = new UIElement();
        container.layout(layout -> layout.flexDirection(FlexDirection.ROW).paddingAll(4));
        container.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

        var slots = new UIElement();
        int index = 0;
        for (int y = 0; y < invTier; y++) {
            var row = new UIElement();
            row.layout(layout -> layout.flexDirection(FlexDirection.ROW));
            for (int x = 0; x < invTier; x++) {
                row.addChild(new ItemSlot().bind(getInventory().storage, index++));
            }
            slots.addChild(row);
        }

        var tanks = new UIElement();
        for (int i = 0; i < invTier; i++) {
            var slot = new FluidSlot().bind(tank.getStorages()[i], 0);
            slot.setAllowClickFilled(true);
            slot.setAllowClickDrained(true);
            slot.style(style -> style.backgroundTexture(GuiTextures.FLUID_SLOT));
            tanks.addChild(slot);
        }

        container.addChildren(slots, tanks);
        var group = new UIElement();
        group.layout(layout -> layout.alignItems(AlignItems.CENTER).paddingAll(4));
        group.addChild(container);
        return group;
    }

    ///////////////////////////////
    // ******* Rendering ********//
    ///////////////////////////////
    @Override
    public @Nullable SpriteTexture sideTips(Player player, BlockPos pos, BlockState state, Set<GTToolType> toolTypes,
                                            Direction side) {
        if (toolTypes.contains(GTToolType.SCREWDRIVER)) {
            if (side == getOutputFacingItems() || side == getOutputFacingFluids()) {
                return GuiTextures.TOOL_ALLOW_INPUT;
            }
        }
        return super.sideTips(player, pos, state, toolTypes, side);
    }

    ////////////////////////////////
    // ********** Misc ***********//
    ////////////////////////////////

    @Override
    public void onMachineRemoved() {
        clearInventory(inventory.storage);
    }
}
