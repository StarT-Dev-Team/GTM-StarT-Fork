package com.gregtechceu.gtceu.common.machine.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.*;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RequireRerender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraftforge.energy.IEnergyStorage;

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ChargerMachine extends TieredEnergyMachine implements IControllable, IFancyUIMachine, IMachineLife {

    public static final long AMPS_PER_ITEM = 4L;

    public enum State implements StringRepresentable {

        IDLE("idle"),
        RUNNING("running"),
        FINISHED("finished");

        @Getter
        private final String serializedName;

        State(String name) {
            this.serializedName = name;
        }
    }

    public static final EnumProperty<ChargerMachine.State> STATE_PROPERTY = GTMachineModelProperties.CHARGER_STATE;

    @Persisted
    @Getter
    @Setter
    private boolean isWorkingEnabled;
    @Getter
    private final int inventorySize;
    @Getter
    @Persisted
    protected final CustomItemStackHandler chargerInventory;

    @Getter
    @DescSynced
    @RequireRerender
    private State state;

    public ChargerMachine(IMachineBlockEntity holder, int tier, int inventorySize, Object... args) {
        super(holder, tier, inventorySize);
        this.isWorkingEnabled = true;
        this.inventorySize = inventorySize;
        this.chargerInventory = createChargerInventory(args);
        this.state = State.IDLE;
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    @Override
    protected NotifiableEnergyContainer createEnergyContainer(Object... args) {
        return new EnergyBatteryTrait((int) args[0]);
    }

    protected CustomItemStackHandler createChargerInventory(Object... args) {
        var handler = new CustomItemStackHandler(this.inventorySize) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        handler.setFilter(item -> GTCapabilityHelper.getElectricItem(item) != null ||
                (ConfigHolder.INSTANCE.compat.energy.nativeEUToFE &&
                        GTCapabilityHelper.getForgeEnergyItem(item) != null));
        return handler;
    }

    @Override
    public int tintColor(int index) {
        if (index == 2) {
            return GTValues.VC[getTier()];
        }
        return super.tintColor(index);
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(chargerInventory);
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////

    @Override
    public UIElement createUIWidget() {
        int rowSize = (int) Math.sqrt(inventorySize);
        int colSize = rowSize;
        if (inventorySize == 8) {
            rowSize = 4;
            colSize = 2;
        }
        var template = new UIElement();
        template.layout(layout -> layout.paddingAll(4));
        template.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));
        int index = 0;
        for (int y = 0; y < colSize; y++) {
            var row = new UIElement();
            row.layout(layout -> layout.flexDirection(FlexDirection.ROW));
            for (int x = 0; x < rowSize; x++) {
                var slot = new ItemSlot().bind(chargerInventory, index++);
                slot.slotStyle(style -> style.slotOverlay(GuiTextures.CHARGER_OVERLAY));
                row.addChild(slot);
            }
            template.addChild(row);
        }

        var editableUI = createEnergyBar();
        var energyBar = editableUI.createDefault();

        var group = new UIElement();
        group.layout(layout -> layout.minWidth(172).flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER).gapAll(4).paddingAll(4));
        group.addChildren(energyBar, template);
        editableUI.setupUI(group, this);
        return group;
    }

    //////////////////////////////////////
    // ****** Charger Logic ******//
    //////////////////////////////////////

    private List<Object> getNonFullElectricItem() {
        List<Object> electricItems = new ArrayList<>();
        for (int i = 0; i < chargerInventory.getSlots(); i++) {
            var electricItemStack = chargerInventory.getStackInSlot(i);
            var electricItem = GTCapabilityHelper.getElectricItem(electricItemStack);
            if (electricItem != null) {
                if (electricItem.getCharge() < electricItem.getMaxCharge()) {
                    electricItems.add(electricItem);
                }
            } else if (ConfigHolder.INSTANCE.compat.energy.nativeEUToFE) {
                var energyStorage = GTCapabilityHelper.getForgeEnergyItem(electricItemStack);
                if (energyStorage != null) {
                    if (energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
                        electricItems.add(energyStorage);
                    }
                }
            }
        }
        return electricItems;
    }

    private void changeState(State newState) {
        if (state != newState) {
            state = newState;
            setRenderState(getRenderState().setValue(GTMachineModelProperties.CHARGER_STATE, newState));
        }
    }

    protected class EnergyBatteryTrait extends NotifiableEnergyContainer {

        protected EnergyBatteryTrait(int inventorySize) {
            super(ChargerMachine.this, GTValues.V[tier] * inventorySize * 32L, GTValues.V[tier],
                    inventorySize * AMPS_PER_ITEM, 0L, 0L);
            this.setSideInputCondition(side -> isWorkingEnabled());
            this.setSideOutputCondition(side -> false);
        }

        @Override
        public long acceptEnergyFromNetwork(@Nullable Direction side, long voltage, long amperage) {
            var latestTimeStamp = getMachine().getOffsetTimer();
            if (lastTimeStamp < latestTimeStamp) {
                amps = 0;
                lastTimeStamp = latestTimeStamp;
            }
            if (amperage <= 0 || voltage <= 0) {
                changeState(State.IDLE);
                return 0;
            }

            var electricItems = getNonFullElectricItem();
            var maxAmps = electricItems.size() * AMPS_PER_ITEM - amps;
            var usedAmps = Math.min(maxAmps, amperage);
            if (maxAmps <= 0) {
                return 0;
            }

            if (side == null || inputsEnergy(side)) {
                if (voltage > getInputVoltage()) {
                    doExplosion(GTUtil.getExplosionPower(voltage));
                    return usedAmps;
                }

                // Prioritizes as many packets as available from the buffer
                long internalAmps = Math.min(maxAmps, Math.max(0, getInternalStorage() / voltage));

                usedAmps = Math.min(usedAmps, maxAmps - internalAmps);
                amps += usedAmps;

                long energy = (usedAmps + internalAmps) * voltage;
                long distributed = energy / electricItems.size();

                boolean changed = false;
                for (var electricItem : electricItems) {
                    long charged = 0;
                    if (electricItem instanceof IElectricItem item) {
                        charged = item.charge(Math.min(distributed, GTValues.V[item.getTier()] * AMPS_PER_ITEM),
                                getTier(), true, false);
                    } else if (electricItem instanceof IEnergyStorage energyStorage) {
                        charged = FeCompat.insertEu(energyStorage,
                                Math.min(distributed, GTValues.V[getTier()] * AMPS_PER_ITEM), false);
                    }
                    if (charged > 0) {
                        changed = true;
                    }
                    energy -= charged;
                    energyInputPerSec += charged;
                }

                if (changed) {
                    ChargerMachine.this.markDirty();
                    changeState(State.RUNNING);
                }

                // Remove energy used and then transfer overflow energy into the internal buffer
                setEnergyStored(getInternalStorage() - internalAmps * voltage + energy);
                return usedAmps;
            }
            return 0;
        }

        @Override
        public long getEnergyCapacity() {
            long energyCapacity = 0L;
            for (int i = 0; i < chargerInventory.getSlots(); i++) {
                var electricItemStack = chargerInventory.getStackInSlot(i);
                var electricItem = GTCapabilityHelper.getElectricItem(electricItemStack);
                if (electricItem != null) {
                    energyCapacity += electricItem.getMaxCharge();
                } else if (ConfigHolder.INSTANCE.compat.energy.nativeEUToFE) {
                    var energyStorage = GTCapabilityHelper.getForgeEnergyItem(electricItemStack);
                    if (energyStorage != null) {
                        energyCapacity += FeCompat.toEu(energyStorage.getMaxEnergyStored(),
                                FeCompat.ratio(false));
                    }
                }
            }

            if (energyCapacity == 0) {
                changeState(State.IDLE);
            }

            return energyCapacity;
        }

        @Override
        public long getEnergyStored() {
            long energyStored = 0L;
            for (int i = 0; i < chargerInventory.getSlots(); i++) {
                var electricItemStack = chargerInventory.getStackInSlot(i);
                var electricItem = GTCapabilityHelper.getElectricItem(electricItemStack);
                if (electricItem != null) {
                    energyStored += electricItem.getCharge();
                } else if (ConfigHolder.INSTANCE.compat.energy.nativeEUToFE) {
                    var energyStorage = GTCapabilityHelper.getForgeEnergyItem(electricItemStack);
                    if (energyStorage != null) {
                        energyStored += FeCompat.toEu(energyStorage.getEnergyStored(),
                                FeCompat.ratio(false));
                    }
                }
            }

            var capacity = getEnergyCapacity();

            if (capacity != 0 && capacity == energyStored) {
                changeState(State.FINISHED);
            }

            return energyStored;
        }

        private long getInternalStorage() {
            return energyStored;
        }
    }
}
