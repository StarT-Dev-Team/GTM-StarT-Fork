package com.gregtechceu.gtceu.common.machine.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.IMiner;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.WidgetUtils;
import com.gregtechceu.gtceu.api.gui.editor.EditableMachineUI;
import com.gregtechceu.gtceu.api.gui.editor.EditableUI;
import com.gregtechceu.gtceu.api.gui.widget.ComponentPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.WorkableTieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IAutoOutputItem;
import com.gregtechceu.gtceu.api.machine.feature.IDataInfoProvider;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.common.item.PortableScannerBehavior;
import com.gregtechceu.gtceu.common.machine.trait.miner.MinerLogic;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MinerMachine extends WorkableTieredMachine
                          implements IMiner, IControllable, IFancyUIMachine, IDataInfoProvider, IAutoOutputItem {

    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected Direction outputFacingItems;
    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected boolean autoOutputItems;
    @Getter
    @Setter
    @Persisted
    protected boolean allowInputFromOutputSideItems;
    @Getter
    @Persisted
    protected final CustomItemStackHandler chargerInventory;
    private final long energyPerTick;
    @Nullable
    protected TickableSubscription autoOutputSubs, batterySubs;
    @Nullable
    protected ISubscription exportItemSubs, energySubs;

    public MinerMachine(IMachineBlockEntity holder, int tier, int speed, int maximumRadius, int fortune,
                        Object... args) {
        super(holder, tier, GTMachineUtils.defaultTankSizeFunction, args, (tier + 1) * (tier + 1), fortune, speed,
                maximumRadius);
        this.energyPerTick = GTValues.V[tier - 1];
        this.chargerInventory = createChargerItemHandler();
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////

    protected CustomItemStackHandler createChargerItemHandler(Object... args) {
        var handler = new CustomItemStackHandler();
        handler.setFilter(item -> GTCapabilityHelper.getElectricItem(item) != null ||
                (ConfigHolder.INSTANCE.compat.energy.nativeEUToFE &&
                        GTCapabilityHelper.getForgeEnergyItem(item) != null));
        return handler;
    }

    @Override
    protected NotifiableItemStackHandler createImportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, 0, IO.IN);
    }

    @Override
    protected NotifiableItemStackHandler createExportItemHandler(Object... args) {
        if (args.length > 3 && args[args.length - 4] instanceof Integer invSize) {
            return new NotifiableItemStackHandler(this, invSize, IO.OUT);
        }
        throw new IllegalArgumentException(
                "MinerMachine need args [inventorySize, fortune, speed, maximumRadius] for initialization");
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        if (args.length > 2 && args[args.length - 3] instanceof Integer fortune &&
                args[args.length - 2] instanceof Integer speed && args[args.length - 1] instanceof Integer maxRadius) {
            return new MinerLogic(this, fortune, speed, maxRadius);
        }
        throw new IllegalArgumentException(
                "MinerMachine need args [inventorySize, fortune, speed, maximumRadius] for initialization");
    }

    @Override
    public void onMachineRemoved() {
        // Remove the miner pipes below this miner
        getRecipeLogic().onRemove();
        clearInventory(exportItems.storage);
        clearInventory(chargerInventory);
    }

    @Override
    public MinerLogic getRecipeLogic() {
        return (MinerLogic) super.getRecipeLogic();
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateAutoOutputSubscription();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            if (getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.getServer().tell(new TickTask(0, this::updateAutoOutputSubscription));
            }
            updateBatterySubscription();
            exportItemSubs = exportItems.addChangedListener(this::updateAutoOutputSubscription);
            energySubs = energyContainer.addChangedListener(this::updateBatterySubscription);
            chargerInventory.setOnContentsChanged(this::updateBatterySubscription);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (exportItemSubs != null) {
            exportItemSubs.unsubscribe();
            exportItemSubs = null;
        }

        if (energySubs != null) {
            energySubs.unsubscribe();
            energySubs = null;
        }
    }

    //////////////////////////////////////
    // ********** LOGIC **********//
    //////////////////////////////////////
    protected void updateAutoOutputSubscription() {
        var outputFace = getOutputFacingItems();
        if (isAutoOutputItems() && outputFace != null && !exportItems.isEmpty() &&
                GTTransferUtils.hasAdjacentItemHandler(getLevel(), getPos(), outputFace)) {
            autoOutputSubs = subscribeServerTick(autoOutputSubs, this::autoOutput);
        } else if (autoOutputSubs != null) {
            autoOutputSubs.unsubscribe();
            autoOutputSubs = null;
        }
    }

    protected void updateBatterySubscription() {
        if (energyContainer.dischargeOrRechargeEnergyContainers(chargerInventory, 0, true)) {
            batterySubs = subscribeServerTick(batterySubs, this::chargeBattery);
        } else if (batterySubs != null) {
            batterySubs.unsubscribe();
            batterySubs = null;
        }
    }

    protected void autoOutput() {
        if (getOffsetTimer() % 5 == 0) {
            if (isAutoOutputItems() && getOutputFacingItems() != null) {
                exportItems.exportToNearby(getOutputFacingItems());
            }
        }
        updateAutoOutputSubscription();
    }

    @Override
    public void setAutoOutputItems(boolean allow) {
        this.autoOutputItems = allow;
        updateAutoOutputSubscription();
    }

    @Override
    public void setOutputFacingItems(@Nullable Direction outputFacing) {
        if (outputFacing != Direction.DOWN) {
            this.outputFacingItems = outputFacing;
            updateAutoOutputSubscription();
        }
    }

    protected void chargeBattery() {
        if (!energyContainer.dischargeOrRechargeEnergyContainers(chargerInventory, 0, false)) {
            updateBatterySubscription();
        }
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    public static BiFunction<ResourceLocation, Integer, EditableMachineUI> EDITABLE_UI_CREATOR = Util
            .memoize((path, inventorySize) -> new EditableMachineUI("misc", path, () -> {
                var template = createTemplate(inventorySize).createDefault();
                var batterySlot = createBatterySlot().createDefault();
                batterySlot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(100).top(10));
                var group = new UIElement();
                group.layout(layout -> layout.minWidth(172).alignItems(AlignItems.CENTER)
                        .justifyContent(AlignContent.CENTER).paddingAll(4));
                group.addChildren(template, batterySlot);
                return group;
            }, (template, machine) -> {
                if (machine instanceof MinerMachine minerMachine) {
                    createTemplate(inventorySize).setupUI(template, minerMachine);
                    createEnergyBar().setupUI(template, minerMachine);
                    createBatterySlot().setupUI(template, minerMachine);
                }
            }));

    protected static EditableUI<UIElement, MinerMachine> createTemplate(int inventorySize) {
        return new EditableUI<>("miner", UIElement.class, () -> {
            int rowSize = (int) Math.sqrt(inventorySize);
            int height = Math.max(rowSize * 18, 80);
            var group = new UIElement();
            group.layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(3));

            var slots = new UIElement();
            for (int y = 0; y < rowSize; y++) {
                var row = new UIElement();
                row.layout(layout -> layout.flexDirection(FlexDirection.ROW));
                for (int x = 0; x < rowSize; x++) {
                    int index = y * rowSize + x;
                    var slot = new ItemSlot();
                    slot.setId("slot_" + index);
                    row.addChild(slot);
                }
                slots.addChild(row);
            }

            var componentPanel = new ComponentPanel(list -> {});
            componentPanel.setMaxWidthLimit(110);
            componentPanel.setId("component_panel");

            var scroller = UITemplate.displayScreen(GuiTextures.DISPLAY);
            scroller.layout(layout -> layout.widthPercent(100).heightPercent(100));
            scroller.addScrollViewChild(componentPanel);

            var container = new UIElement();
            container.layout(layout -> layout.width(117).height(height).paddingAll(4));
            container.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));
            container.addChild(scroller);
            group.addChildren(container, slots);
            return group;
        }, (group, machine) -> {
            WidgetUtils.widgetByIdForEach(group, "^slot_[0-9]+$", ItemSlot.class, slot -> {
                var index = WidgetUtils.widgetIdIndex(slot);
                if (index >= 0 && index < machine.exportItems.getSlots()) {
                    slot.bind(new ItemHandlerSlot(machine.exportItems, index).setCanPlace(stack -> false));
                }
            });
            WidgetUtils.widgetByIdForEach(group, "^component_panel$", ComponentPanel.class,
                    panel -> panel.textSupplier(machine::addDisplayText));
        });
    }

    protected static EditableUI<ItemSlot, MinerMachine> createBatterySlot() {
        return new EditableUI<>("battery_slot", ItemSlot.class, () -> {
            var slot = new ItemSlot();
            slot.slotStyle(style -> style.slotOverlay(GuiTextures.CHARGER_OVERLAY));
            return slot;
        }, (slot, machine) -> {
            slot.bind(machine.chargerInventory, 0);
            var tooltips = LangHandler.getMultiLang("gtceu.gui.charger_slot.tooltip",
                    GTValues.VNF[machine.getTier()], GTValues.VNF[machine.getTier()]).toArray(Component[]::new);
            slot.style(style -> style.tooltips(tooltips));
        });
    }

    private void addDisplayText(@NotNull List<Component> textList) {
        int workingArea = IMiner.getWorkingArea(getRecipeLogic().getCurrentRadius());
        textList.add(recipeLogic.getCustomProgressLine());
        textList.add(Component.translatable("gtceu.machine.miner.startx", getRecipeLogic().getX()).append(" ")
                .append(Component.translatable("gtceu.machine.miner.minex", getRecipeLogic().getMineX())));
        textList.add(Component.translatable("gtceu.machine.miner.starty", getRecipeLogic().getY()).append(" ")
                .append(Component.translatable("gtceu.machine.miner.miney", getRecipeLogic().getMineY())));
        textList.add(Component.translatable("gtceu.machine.miner.startz", getRecipeLogic().getZ()).append(" ")
                .append(Component.translatable("gtceu.machine.miner.minez", getRecipeLogic().getMineZ())));
        textList.add(Component.translatable("gtceu.universal.tooltip.working_area", workingArea, workingArea));
        if (getRecipeLogic().isDone())
            textList.add(Component.translatable("gtceu.multiblock.large_miner.done")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)));
        else if (getRecipeLogic().isWorking())
            textList.add(Component.translatable("gtceu.multiblock.large_miner.working")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
        else if (!this.isWorkingEnabled())
            textList.add(Component.translatable("gtceu.multiblock.work_paused"));
        if (getRecipeLogic().isInventoryFull())
            textList.add(Component.translatable("gtceu.multiblock.large_miner.invfull")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        if (!drainInput(true))
            textList.add(Component.translatable("gtceu.multiblock.large_miner.needspower")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
    }

    @Override
    public boolean drainInput(boolean simulate) {
        long resultEnergy = energyContainer.getEnergyStored() - energyPerTick;
        if (resultEnergy >= 0L && resultEnergy <= energyContainer.getEnergyCapacity()) {
            if (!simulate)
                energyContainer.removeEnergy(energyPerTick);
            return true;
        }
        return false;
    }

    //////////////////////////////////////
    // ******* Interaction *******//
    //////////////////////////////////////
    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        if (isRemote()) return InteractionResult.SUCCESS;

        if (!this.isActive()) {
            int currentRadius = getRecipeLogic().getCurrentRadius();
            if (currentRadius == 1)
                getRecipeLogic().setCurrentRadius(getRecipeLogic().getMaximumRadius());
            else if (playerIn.isShiftKeyDown())
                getRecipeLogic().setCurrentRadius(Math.max(1, Math.round(currentRadius / 2.0f)));
            else
                getRecipeLogic().setCurrentRadius(Math.max(1, currentRadius - 1));

            getRecipeLogic().resetArea(true);

            int workingArea = IMiner.getWorkingArea(getRecipeLogic().getCurrentRadius());
            playerIn.sendSystemMessage(
                    Component.translatable("gtceu.universal.tooltip.working_area", workingArea, workingArea));
        } else {
            playerIn.sendSystemMessage(Component.translatable("gtceu.multiblock.large_miner.errorradius"));
        }
        return InteractionResult.SUCCESS;
    }

    @NotNull
    @Override
    public List<Component> getDataInfo(PortableScannerBehavior.DisplayMode mode) {
        if (mode == PortableScannerBehavior.DisplayMode.SHOW_ALL ||
                mode == PortableScannerBehavior.DisplayMode.SHOW_MACHINE_INFO) {
            int workingArea = IMiner.getWorkingArea(getRecipeLogic().getCurrentRadius());
            return Collections.singletonList(
                    Component.translatable("gtceu.universal.tooltip.working_area", workingArea, workingArea));
        }
        return new ArrayList<>();
    }
}
