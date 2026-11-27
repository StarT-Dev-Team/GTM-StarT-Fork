package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMufflerMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemHandlerHelper;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MufflerPartMachine extends TieredPartMachine implements IMufflerMachine, IUIMachine {

    @Getter
    private final int recoveryChance;
    @Getter
    @Persisted
    private final CustomItemStackHandler inventory;

    private TickableSubscription snowSubscription;

    public MufflerPartMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
        this.recoveryChance = Math.max(1, tier * 10);
        this.inventory = new CustomItemStackHandler((int) Math.pow(tier + 1, 2));
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    //////////////////////////////////////
    // ******** Muffler *********//
    //////////////////////////////////////

    @Override
    public void recoverItemsTable(ItemStack... recoveryItems) {
        int numRolls = Math.min(recoveryItems.length, inventory.getSlots());
        IntStream.range(0, numRolls).forEach(slot -> {
            if (calculateChance()) {
                ItemHandlerHelper.insertItemStacked(inventory, recoveryItems[slot].copy(), false);
            }
        });
    }

    private boolean calculateChance() {
        return recoveryChance >= 100 || recoveryChance >= GTValues.RNG.nextInt(100);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        super.clientTick();
        for (IMultiController controller : getControllers()) {
            if (controller instanceof IRecipeLogicMachine recipeLogicMachine &&
                    recipeLogicMachine.getRecipeLogic().isWorking()) {
                emitPollutionParticles();
                break;
            }
        }
    }

    @Override
    public void addedToController(IMultiController controller) {
        super.addedToController(controller);
        if (snowSubscription == null) {
            this.snowSubscription = subscribeServerTick(null, this::tryBreakSnow);
        }
    }

    @MustBeInvokedByOverriders
    @Override
    public void removedFromController(IMultiController controller) {
        super.removedFromController(controller);
        if (controllers.isEmpty()) {
            unsubscribe(snowSubscription);
            snowSubscription = null;
        }
    }

    private void tryBreakSnow() {
        if (getOffsetTimer() % 10 == 0) {
            for (IMultiController controller : getControllers()) {
                if (controller instanceof IRecipeLogicMachine recipeLogicMachine &&
                        recipeLogicMachine.getRecipeLogic().isWorking()) {
                    BlockPos mufflerPos = getPos().relative(getFrontFacing());
                    GTUtil.tryBreakSnow(getLevel(), mufflerPos, getLevel().getBlockState(mufflerPos), true);
                }
            }
        }
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////
    @Override
    public ModularUI createUI(Player entityPlayer) {
        int rowSize = (int) Math.sqrt(inventory.getSlots());

        var title = new Label();
        title.setText(Component.translatable(getBlockState().getBlock().getDescriptionId()));
        title.layout(layout -> layout.alignSelf(AlignItems.FLEX_START).marginLeft(5));

        var grid = new UIElement();
        for (int y = 0; y < rowSize; y++) {
            var row = new UIElement();
            row.layout(layout -> layout.flexDirection(FlexDirection.ROW));
            for (int x = 0; x < rowSize; x++) {
                var slot = new ItemSlot().bind(new ItemHandlerSlot(inventory, y * rowSize + x)
                        .setCanPlace(stack -> false));
                row.addChild(slot);
            }
            grid.addChild(row);
        }

        var root = UITemplate.panel();
        root.layout(layout -> layout.minWidth(176).paddingAll(5).paddingBottom(7));
        root.addChildren(title, grid, UITemplate.playerInventory());
        return UITemplate.modularUI(root, entityPlayer);
    }
}
