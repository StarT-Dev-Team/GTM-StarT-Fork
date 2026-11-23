package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.ILaserContainer;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.gui.widget.LongInput;
import com.gregtechceu.gtceu.api.gui.widget.TextToggleButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import dev.vfyjxf.taffy.style.TaffyPosition;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeEnergyContainerMachine extends TieredMachine implements ILaserContainer, IUIMachine {

    @Persisted
    private long voltage = 0;
    @Persisted
    private int amps = 1;
    @Persisted
    private int setTier = 0;
    @Persisted
    private boolean active = false;
    @Persisted
    private boolean source = true;
    @Persisted
    private long energyIOPerSec = 0;
    private long lastAverageEnergyIOPerTick = 0;
    private long ampsReceived = 0;
    private boolean doExplosion = false;

    public CreativeEnergyContainerMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.MAX);
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    @Override
    public void onLoad() {
        super.onLoad();
        subscribeServerTick(this::updateEnergyTick);
    }

    //////////////////////////////////////
    // ********** MISC ***********//
    //////////////////////////////////////

    protected void updateEnergyTick() {
        if (getOffsetTimer() % 20 == 0) {
            this.setIOSpeed(energyIOPerSec / 20);
            energyIOPerSec = 0;
            if (doExplosion) {
                getLevel().explode(null, getPos().getX() + 0.5, getPos().getY() + 0.5, getPos().getZ() + 0.5,
                        1, Level.ExplosionInteraction.NONE);
                doExplosion = false;
            }
        }
        ampsReceived = 0;
        if (!active || !source || voltage <= 0 || amps <= 0) return;
        int ampsUsed = 0;
        for (var facing : GTUtil.DIRECTIONS) {
            var opposite = facing.getOpposite();
            IEnergyContainer container = GTCapabilityHelper.getEnergyContainer(getLevel(), getPos().relative(facing),
                    opposite);
            // Try to get laser capability
            if (container == null)
                container = GTCapabilityHelper.getLaser(getLevel(), getPos().relative(facing), opposite);

            if (container != null && container.inputsEnergy(opposite) && container.getEnergyCanBeInserted() > 0) {
                ampsUsed += container.acceptEnergyFromNetwork(opposite, voltage, amps - ampsUsed);
                if (ampsUsed >= amps) {
                    break;
                }
            }
        }
        energyIOPerSec += ampsUsed * voltage;
    }

    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
        if (source || !active || ampsReceived >= amps) {
            return 0;
        }
        if (voltage > this.voltage) {
            if (doExplosion)
                return 0;
            doExplosion = true;
            return Math.min(amperage, getInputAmperage() - ampsReceived);
        }
        long amperesAccepted = Math.min(amperage, getInputAmperage() - ampsReceived);
        if (amperesAccepted > 0) {
            ampsReceived += amperesAccepted;
            energyIOPerSec += amperesAccepted * voltage;
            return amperesAccepted;
        }
        return 0;
    }

    @Override
    public boolean inputsEnergy(Direction side) {
        return !source;
    }

    @Override
    public boolean outputsEnergy(Direction side) {
        return source;
    }

    @Override
    public long changeEnergy(long differenceAmount) {
        if (source || !active) {
            return 0;
        }
        energyIOPerSec += differenceAmount;
        return differenceAmount;
    }

    @Override
    public long getEnergyStored() {
        return 69;
    }

    @Override
    public long getEnergyCapacity() {
        return 420;
    }

    @Override
    public long getInputAmperage() {
        return source ? 0 : amps;
    }

    @Override
    public long getInputVoltage() {
        return source ? 0 : voltage;
    }

    @Override
    public long getOutputVoltage() {
        return source ? voltage : 0;
    }

    @Override
    public long getOutputAmperage() {
        return source ? amps : 0;
    }

    public void setIOSpeed(long energyIOPerSec) {
        if (this.lastAverageEnergyIOPerTick != energyIOPerSec) {
            this.lastAverageEnergyIOPerTick = energyIOPerSec;
        }
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = UITemplate.panel();
        root.layout(layout -> layout.width(176).height(166).paddingAll(0));

        var voltageLabel = new Label();
        voltageLabel.setText(Component.translatable("gtceu.creative.energy.voltage"));
        voltageLabel.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(32));

        var voltageInput = new LongInput(() -> voltage, value -> {
            voltage = value;
            setTier = GTUtil.getOCTierByVoltage(voltage);
        });
        voltageInput.setMin(0L);
        voltageInput.setMax(Long.MAX_VALUE);
        voltageInput.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(9).top(47).width(152)
                .height(16));

        var amperageLabel = new Label();
        amperageLabel.setText(Component.translatable("gtceu.creative.energy.amperage"));
        amperageLabel.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(74));

        var ampsInput = new IntInput(() -> amps, value -> amps = value);
        ampsInput.setMin(0);
        ampsInput.setMax(Integer.MAX_VALUE);
        ampsInput.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(9).top(89).width(152)
                .height(16));

        var average = new Label();
        average.bind(DataBindingBuilder.componentS2C(() -> Component.literal(
                "Average Energy I/O per tick: " + this.lastAverageEnergyIOPerTick)).build());
        average.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(110));

        var activity = new TextToggleButton("gtceu.creative.activity.on", "gtceu.creative.activity.off",
                () -> active, value -> active = value);
        activity.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(139).width(77)
                .height(20));

        var sourceToggle = new TextToggleButton("gtceu.creative.energy.source", "gtceu.creative.energy.sink",
                () -> source, value -> {
                    source = value;
                    if (source) {
                        voltage = 0;
                        amps = 0;
                        setTier = 0;
                    } else {
                        voltage = GTValues.V[14];
                        amps = Integer.MAX_VALUE;
                        setTier = 14;
                    }
                });
        sourceToggle.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(85).top(139).width(77)
                .height(20));

        var tierSelector = new Selector<String>();
        tierSelector.setCandidates(Arrays.stream(GTValues.VNF).toList());
        tierSelector.bind(DataBindingBuilder.string(() -> GTValues.VNF[setTier], tier -> {
            setTier = ArrayUtils.indexOf(GTValues.VNF, tier);
            voltage = GTValues.VEX[setTier];
        }).build());
        tierSelector.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(7).width(65)
                .height(20));

        root.addChildren(voltageLabel, voltageInput, amperageLabel, ampsInput, average, activity,
                sourceToggle, tierSelector);
        return UITemplate.modularUI(root, entityPlayer);
    }
}
