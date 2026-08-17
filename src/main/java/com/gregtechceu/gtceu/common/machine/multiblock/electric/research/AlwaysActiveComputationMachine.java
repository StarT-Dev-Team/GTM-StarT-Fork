package com.gregtechceu.gtceu.common.machine.multiblock.electric.research;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AlwaysActiveComputationMachine extends WorkableElectricMultiblockMachine {

    private IMaintenanceMachine maintenance;
    private IEnergyContainer energyContainer;

    @Getter
    private int energyUsage = 0;

    @Nullable
    protected TickableSubscription tickSubs;

    public AlwaysActiveComputationMachine(IMachineBlockEntity holder) {
        super(holder);
        this.energyContainer = new EnergyContainerList(new ArrayList<>());
    }

    protected abstract int calculateEnergyUsage();

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        List<IEnergyContainer> energyContainers = new ArrayList<>();
        Long2ObjectMap<IO> ioMap = getMultiblockState().getMatchContext().getOrCreate("ioMap",
                Long2ObjectMaps::emptyMap);
        for (IMultiPart part : getParts()) {
            IO io = ioMap.getOrDefault(part.self().getPos().asLong(), IO.BOTH);
            if (part instanceof IMaintenanceMachine maintenanceMachine) {
                this.maintenance = maintenanceMachine;
            }
            if (io == IO.NONE || io == IO.OUT) continue;
            var handlerLists = part.getRecipeHandlers();
            for (var handlerList : handlerLists) {
                if (!handlerList.isValid(io)) continue;
                handlerList.getCapability(EURecipeCapability.CAP).stream()
                        .filter(IEnergyContainer.class::isInstance)
                        .map(IEnergyContainer.class::cast)
                        .forEach(energyContainers::add);
            }
        }
        this.energyContainer = new EnergyContainerList(energyContainers);
        this.energyUsage = calculateEnergyUsage();

        if (this.maintenance == null) {
            onStructureInvalid();
            return;
        }

        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateTickSubscription));
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.energyContainer = new EnergyContainerList(new ArrayList<>());
        this.energyUsage = 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.isFormed() && getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateTickSubscription));
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tickSubs != null) {
            tickSubs.unsubscribe();
            tickSubs = null;
        }
    }

    protected void updateTickSubscription() {
        if (isFormed) {
            tickSubs = subscribeServerTick(tickSubs, this::tick);
        } else if (tickSubs != null) {
            tickSubs.unsubscribe();
            tickSubs = null;
        }
    }

    public void tick() {
        int energyToConsume = this.getEnergyUsage();
        boolean hasMaintenance = ConfigHolder.INSTANCE.machines.enableMaintenance && this.maintenance != null;
        if (hasMaintenance) {
            // 10% more energy per maintenance problem
            energyToConsume += maintenance.getNumMaintenanceProblems() * energyToConsume / 10;
        }

        if (getRecipeLogic().isWaiting() && energyContainer.getInputPerSec() > 19L * energyToConsume) {
            getRecipeLogic().setStatus(RecipeLogic.Status.IDLE);
        }

        if (this.energyContainer.getEnergyStored() >= energyToConsume) {
            if (!getRecipeLogic().isWaiting()) {
                long consumed = this.energyContainer.removeEnergy(energyToConsume);
                if (consumed == energyToConsume) {
                    getRecipeLogic().setStatus(RecipeLogic.Status.WORKING);
                } else {
                    getRecipeLogic()
                            .setWaiting(Component.translatable("gtceu.recipe_logic.insufficient_in")
                                    .append(": ").append(EURecipeCapability.CAP.getName()));
                }
            }
        } else {
            getRecipeLogic().setWaiting(Component.translatable("gtceu.recipe_logic.insufficient_in").append(": ")
                    .append(EURecipeCapability.CAP.getName()));
        }
        updateTickSubscription();
    }
}
