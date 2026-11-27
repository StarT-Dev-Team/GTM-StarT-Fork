package com.gregtechceu.gtceu.api.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.editor.EditableMachineUI;
import com.gregtechceu.gtceu.api.gui.editor.EditableUI;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.gui.widget.GhostCircuitSlot;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IAutoOutputBoth;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.google.common.collect.Tables;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * All simple single machines are implemented here.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleTieredMachine extends WorkableTieredMachine
                                 implements IAutoOutputBoth, IFancyUIMachine, IHasCircuitSlot {

    @Persisted
    @DescSynced
    @RequireRerender
    protected Direction outputFacingItems;
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
    @Getter
    @Persisted
    protected final CustomItemStackHandler chargerInventory;
    @Getter
    @Persisted
    protected final NotifiableItemStackHandler circuitInventory;
    @Getter
    @Setter
    @Persisted
    @DescSynced
    protected boolean circuitPastingBlocked;
    @Nullable
    protected TickableSubscription autoOutputSubs, batterySubs;
    @Nullable
    protected ISubscription exportItemSubs, exportFluidSubs, energySubs;

    public SimpleTieredMachine(IMachineBlockEntity holder, int tier, Int2IntFunction tankScalingFunction,
                               Object... args) {
        super(holder, tier, tankScalingFunction, args);
        this.outputFacingItems = hasFrontFacing() ? getFrontFacing().getOpposite() : Direction.UP;
        this.outputFacingFluids = outputFacingItems;
        this.chargerInventory = createChargerItemHandler(args);
        this.circuitInventory = createCircuitItemHandler(args);
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    protected CustomItemStackHandler createChargerItemHandler(Object... args) {
        var handler = new CustomItemStackHandler() {

            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        handler.setFilter(item -> GTCapabilityHelper.getElectricItem(item) != null ||
                (ConfigHolder.INSTANCE.compat.energy.nativeEUToFE &&
                        GTCapabilityHelper.getForgeEnergyItem(item) != null));
        return handler;
    }

    protected NotifiableItemStackHandler createCircuitItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(IntCircuitBehaviour::isIntegratedCircuit);
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            if (getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.getServer().tell(new TickTask(0, this::updateAutoOutputSubscription));
            }
            updateBatterySubscription();
            exportItemSubs = exportItems.addChangedListener(this::updateAutoOutputSubscription);
            exportFluidSubs = exportFluids.addChangedListener(this::updateAutoOutputSubscription);
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

        if (exportFluidSubs != null) {
            exportFluidSubs.unsubscribe();
            exportFluidSubs = null;
        }

        if (energySubs != null) {
            energySubs.unsubscribe();
            energySubs = null;
        }
    }

    //////////////////////////////////////
    // ******* Auto Output *******//
    //////////////////////////////////////

    @Override
    public boolean hasAutoOutputFluid() {
        return exportFluids.getTanks() > 0;
    }

    @Override
    public boolean hasAutoOutputItem() {
        return exportItems.getSlots() > 0;
    }

    @Override
    public @Nullable Direction getOutputFacingFluids() {
        if (hasAutoOutputFluid()) {
            return outputFacingFluids;
        }
        return null;
    }

    @Override
    public @Nullable Direction getOutputFacingItems() {
        if (hasAutoOutputItem()) {
            return outputFacingItems;
        }
        return null;
    }

    @Override
    public void setAutoOutputItems(boolean allow) {
        if (hasAutoOutputItem()) {
            this.autoOutputItems = allow;
            updateAutoOutputSubscription();
        }
    }

    @Override
    public void setAutoOutputFluids(boolean allow) {
        if (hasAutoOutputFluid()) {
            this.autoOutputFluids = allow;
            updateAutoOutputSubscription();
        }
    }

    @Override
    public void setOutputFacingFluids(@Nullable Direction outputFacing) {
        if (hasAutoOutputFluid()) {
            this.outputFacingFluids = outputFacing;
            updateAutoOutputSubscription();
        }
    }

    @Override
    public void setOutputFacingItems(@Nullable Direction outputFacing) {
        if (hasAutoOutputItem()) {
            this.outputFacingItems = outputFacing;
            updateAutoOutputSubscription();
        }
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateAutoOutputSubscription();
    }

    protected void updateAutoOutputSubscription() {
        var outputFacingItems = getOutputFacingItems();
        var outputFacingFluids = getOutputFacingFluids();
        if ((isAutoOutputItems() && !exportItems.isEmpty() && outputFacingItems != null &&
                GTTransferUtils.hasAdjacentItemHandler(getLevel(), getPos(), outputFacingItems)) ||
                (isAutoOutputFluids() && !exportFluids.isEmpty() && outputFacingFluids != null &&
                        GTTransferUtils.hasAdjacentFluidHandler(getLevel(), getPos(), outputFacingFluids))) {
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

    protected void chargeBattery() {
        if (!energyContainer.dischargeOrRechargeEnergyContainers(chargerInventory, 0, false)) {
            updateBatterySubscription();
        }
    }

    protected void autoOutput() {
        if (getOffsetTimer() % 5 == 0) {
            if (isAutoOutputFluids() && getOutputFacingFluids() != null) {
                exportFluids.exportToNearby(getOutputFacingFluids());
            }
            if (isAutoOutputItems() && getOutputFacingItems() != null) {
                exportItems.exportToNearby(getOutputFacingItems());
            }
        }
        updateAutoOutputSubscription();
    }

    @Override
    public boolean isFacingValid(Direction facing) {
        if (facing == getOutputFacingItems() || facing == getOutputFacingFluids()) {
            return false;
        }
        return super.isFacingValid(facing);
    }

    //////////////////////////////////////
    // ********** MISC ***********//
    //////////////////////////////////////
    @Override
    public void onMachineRemoved() {
        super.onMachineRemoved();
        clearInventory(chargerInventory);
        if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
            clearInventory(circuitInventory.storage);
        }
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IFancyUIMachine.super.attachConfigurators(configuratorPanel);

        if (isCircuitSlotEnabled()) {
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(this));
        }
    }

    @Override
    public void attachAutoPushConfigurators(ConfiguratorPanel configuratorPanel) {
        IFancyUIMachine.super.attachAutoPushConfigurators(configuratorPanel);

        if (hasAutoOutputFluid()) {
            configuratorPanel.attachConfigurators(createAutoOutputFluidConfigurator());
        }
        if (hasAutoOutputItem()) {
            configuratorPanel.attachConfigurators(createAutoOutputItemConfigurator());
        }
    }

    private IFancyConfigurator createAutoOutputFluidConfigurator() {
        return createAutoOutputConfigurator(
                GuiTextures.IO_CONFIG_FLUID_MODES_BUTTON,
                "gtceu.gui.fluid_auto_output",
                this::isAutoOutputFluids,
                (event, nextState) -> this.setAutoOutputFluids(nextState));
    }

    private IFancyConfigurator createAutoOutputItemConfigurator() {
        return createAutoOutputConfigurator(
                GuiTextures.IO_CONFIG_ITEM_MODES_BUTTON,
                "gtceu.gui.item_auto_output",
                this::isAutoOutputItems,
                (event, nextState) -> this.setAutoOutputItems(nextState));
    }

    private IFancyConfigurator createAutoOutputConfigurator(SpriteTexture modesButtonTexture,
                                                            String tooltipBaseLangKey,
                                                            BooleanSupplier stateSupplier,
                                                            BiConsumer<UIEvent, Boolean> onToggle) {
        var toggle = new IFancyConfiguratorButton.Toggle(
                new GuiTextureGroup(
                        GuiTextures.TOGGLE_BUTTON_BACK.copy().setSprite(0, 0, 18, 18),
                        modesButtonTexture.copy().setSprite(0, 36, 36, 36)),
                new GuiTextureGroup(
                        GuiTextures.TOGGLE_BUTTON_BACK.copy().setSprite(0, 18, 18, 18),
                        modesButtonTexture.copy().setSprite(0, 72, 36, 36)),
                stateSupplier,
                onToggle);

        toggle.setTooltipsSupplier(enabled -> {
            var key = tooltipBaseLangKey + '.' + (enabled ? "enabled" : "disabled");
            return List.of(Component.translatable(key));
        });

        return toggle;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static BiFunction<ResourceLocation, GTRecipeType, EditableMachineUI> EDITABLE_UI_CREATOR = Util
            .memoize((path, recipeType) -> new EditableMachineUI("simple", path, () -> {
                var template = recipeType.getRecipeUI().createEditableUITemplate(false, false).createDefault();
                var batterySlot = createBatterySlot().createDefault();
                batterySlot.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).bottom(0).leftPercent(50)
                        .marginLeft(-9));
                var group = new UIElement();
                group.layout(l -> l.minHeight(78).alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));
                group.addChildren(template, batterySlot);
                return group;
            }, (template, machine) -> {
                if (machine instanceof SimpleTieredMachine tieredMachine) {
                    var storages = Tables.newCustomTable(new EnumMap<>(IO.class),
                            LinkedHashMap<RecipeCapability<?>, Object>::new);
                    storages.put(IO.IN, ItemRecipeCapability.CAP, tieredMachine.importItems.storage);
                    storages.put(IO.OUT, ItemRecipeCapability.CAP, tieredMachine.exportItems.storage);
                    storages.put(IO.IN, FluidRecipeCapability.CAP, tieredMachine.importFluids);
                    storages.put(IO.OUT, FluidRecipeCapability.CAP, tieredMachine.exportFluids);
                    storages.put(IO.IN, CWURecipeCapability.CAP, tieredMachine.importComputation);
                    storages.put(IO.OUT, CWURecipeCapability.CAP, tieredMachine.exportComputation);

                    tieredMachine.getRecipeType().getRecipeUI().createEditableUITemplate(false, false).setupUI(template,
                            new GTRecipeTypeUI.RecipeHolder(tieredMachine.recipeLogic::getProgressPercent,
                                    storages,
                                    new CompoundTag(),
                                    Collections.emptyList(),
                                    false, false));
                    createBatterySlot().setupUI(template, tieredMachine);
                }
            }));

    /**
     * Create a battery slot widget.
     */
    protected static EditableUI<ItemSlot, SimpleTieredMachine> createBatterySlot() {
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

    /**
     * Create a ghost circuit slot widget.
     */
    protected static EditableUI<GhostCircuitSlot, SimpleTieredMachine> createCircuitConfigurator() {
        return new EditableUI<>("circuit_configurator", GhostCircuitSlot.class, () -> {
            var slot = new GhostCircuitSlot();
            slot.slotStyle(style -> style.slotOverlay(GuiTextures.INT_CIRCUIT_OVERLAY));
            return slot;
        }, (slot, machine) -> {
            slot.setCircuitInventory(machine.circuitInventory.storage);
            slot.setCircuitMachine(machine);
            var tooltips = LangHandler.getMultiLang("gtceu.gui.configurator_slot.tooltip").toArray(Component[]::new);
            slot.style(style -> style.tooltips(tooltips));
        });
    }

    // Method provided to override
    protected IGuiTexture getCircuitSlotOverlay() {
        return GuiTextures.INT_CIRCUIT_OVERLAY;
    }

    //////////////////////////////////////
    // ******* Rendering ********//
    //////////////////////////////////////
    @Override
    public @Nullable SpriteTexture sideTips(Player player, BlockPos pos, BlockState state, Set<GTToolType> toolTypes,
                                            Direction side) {
        if (toolTypes.contains(GTToolType.WRENCH)) {
            if (!player.isShiftKeyDown()) {
                if (!hasFrontFacing() || side != getFrontFacing()) {
                    return GuiTextures.TOOL_IO_FACING_ROTATION;
                }
            }
        }
        if (toolTypes.contains(GTToolType.SCREWDRIVER)) {
            if (side == getOutputFacingItems() || side == getOutputFacingFluids()) {
                return GuiTextures.TOOL_ALLOW_INPUT;
            }
        }
        return super.sideTips(player, pos, state, toolTypes, side);
    }
}
