package com.gregtechceu.gtceu.api.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.feature.*;
import com.gregtechceu.gtceu.api.machine.trait.*;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class WorkableTieredMachine extends TieredEnergyMachine implements IRecipeLogicMachine,
                                            IMachineLife, IMufflableMachine, IOverclockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(WorkableTieredMachine.class,
            TieredEnergyMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Persisted
    @DescSynced
    public final RecipeLogic recipeLogic;
    @Getter
    public final GTRecipeType[] recipeTypes;
    @Getter
    @Setter
    @Persisted
    public int activeRecipeType;
    @Getter
    public final Int2IntFunction tankScalingFunction;
    @Nullable
    @Getter
    @Setter
    private ICleanroomProvider cleanroom;
    @Persisted
    public final NotifiableItemStackHandler importItems;
    @Persisted
    public final NotifiableItemStackHandler exportItems;
    @Persisted
    public final NotifiableFluidTank importFluids;
    @Persisted
    public final NotifiableFluidTank exportFluids;
    @Persisted
    public final NotifiableComputationContainer importComputation;
    @Persisted
    public final NotifiableComputationContainer exportComputation;
    @Getter
    protected final Map<IO, List<RecipeHandlerList>> capabilitiesProxy;
    @Getter
    protected final Map<IO, Map<RecipeCapability<?>, List<IRecipeHandler<?>>>> capabilitiesFlat;
    @Persisted
    @Getter
    protected int overclockTier;
    protected final List<ISubscription> traitSubscriptions;
    @Persisted
    @DescSynced
    @Getter
    @Setter
    protected boolean isMuffled;
    protected boolean previouslyMuffled = true;

    public WorkableTieredMachine(IMachineBlockEntity holder, int tier, Int2IntFunction tankScalingFunction,
                                 Object... args) {
        super(holder, tier, args);
        this.overclockTier = getMaxOverclockTier();
        this.recipeTypes = getDefinition().getRecipeTypes();
        this.activeRecipeType = 0;
        this.tankScalingFunction = tankScalingFunction;
        this.capabilitiesProxy = new EnumMap<>(IO.class);
        this.capabilitiesFlat = new EnumMap<>(IO.class);
        this.traitSubscriptions = new ArrayList<>();
        this.recipeLogic = attachTrait(createRecipeLogic(args));
        this.importItems = attachTrait(createImportItemHandler(args));
        this.exportItems = attachTrait(createExportItemHandler(args));
        this.importFluids = attachTrait(createImportFluidHandler(args));
        this.exportFluids = attachTrait(createExportFluidHandler(args));
        this.importComputation = attachTrait(createImportComputationContainer(args));
        this.exportComputation = attachTrait(createExportComputationContainer(args));
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected NotifiableEnergyContainer createEnergyContainer(Object... args) {
        long tierVoltage = GTValues.V[getTier()];
        if (isEnergyEmitter()) {
            return RecipeAmperageEnergyContainer.makeEmitterContainer(tierVoltage * 64L,
                    tierVoltage, getMaxInputOutputAmperage());
        } else {
            return RecipeAmperageEnergyContainer.makeReceiverContainer(tierVoltage * 64L,
                    tierVoltage, getMaxInputOutputAmperage());
        }
    }

    protected NotifiableItemStackHandler createImportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(getRecipeType().getMaxInputs(ItemRecipeCapability.CAP), IO.IN);
    }

    protected NotifiableItemStackHandler createExportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(getRecipeType().getMaxOutputs(ItemRecipeCapability.CAP), IO.OUT);
    }

    protected NotifiableFluidTank createImportFluidHandler(Object... args) {
        return new NotifiableFluidTank(getRecipeType().getMaxInputs(FluidRecipeCapability.CAP),
                this.tankScalingFunction.applyAsInt(this.getTier()), IO.IN);
    }

    protected NotifiableFluidTank createExportFluidHandler(Object... args) {
        return new NotifiableFluidTank(getRecipeType().getMaxOutputs(FluidRecipeCapability.CAP),
                this.tankScalingFunction.applyAsInt(this.getTier()), IO.OUT);
    }

    protected NotifiableComputationContainer createImportComputationContainer(Object... args) {
        boolean transmitter = true;
        if (args.length > 0 && args[args.length - 1] instanceof Boolean isTransmitter) {
            transmitter = isTransmitter;
        }
        return new NotifiableComputationContainer(IO.IN, transmitter);
    }

    protected NotifiableComputationContainer createExportComputationContainer(Object... args) {
        return new NotifiableComputationContainer(IO.OUT, false);
    }

    protected RecipeLogic createRecipeLogic(Object... args) {
        return new RecipeLogic();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // attach self traits
        Map<IO, List<IRecipeHandler<?>>> ioTraits = new EnumMap<>(IO.class);

        for (MachineTrait trait : getAllTraits()) {
            if (trait instanceof IRecipeHandlerTrait<?> handlerTrait) {
                ioTraits.computeIfAbsent(handlerTrait.getHandlerIO(), i -> new ArrayList<>()).add(handlerTrait);
            }
        }

        for (var entry : ioTraits.entrySet()) {
            var handlerList = RecipeHandlerList.of(entry.getKey(), entry.getValue());
            this.addHandlerList(handlerList);
            traitSubscriptions.add(handlerList.subscribe(recipeLogic::updateTickSubscription));
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        traitSubscriptions.forEach(ISubscription::unsubscribe);
        traitSubscriptions.clear();
        capabilitiesProxy.clear();
        capabilitiesFlat.clear();
        recipeLogic.inValid();
    }

    //////////////////////////////////////
    // ********** MISC ***********//
    //////////////////////////////////////

    @Override
    public void onMachineRemoved() {
        clearInventory(importItems.storage);
        clearInventory(exportItems.storage);
    }

    //////////////////////////////////////
    // ******** OVERCLOCK *********//
    //////////////////////////////////////

    @Override
    public int getMaxOverclockTier() {
        return GTUtil.getTierByVoltage(Math.max(energyContainer.getInputVoltage(), energyContainer.getOutputVoltage()));
    }

    @Override
    public int getMinOverclockTier() {
        return 0;
    }

    @Override
    public void setOverclockTier(int tier) {
        if (!isRemote() && tier >= getMinOverclockTier() && tier <= getMaxOverclockTier()) {
            this.overclockTier = tier;
            this.recipeLogic.markLastRecipeDirty();
        }
    }

    @Override
    public long getOverclockVoltage() {
        return Math.min(GTValues.V[getOverclockTier()],
                Math.max(energyContainer.getInputVoltage(), energyContainer.getOutputVoltage()));
    }

    //////////////////////////////////////
    // ****** RECIPE LOGIC *******//
    //////////////////////////////////////

    @Override
    public void clientTick() {
        super.clientTick();
        if (previouslyMuffled != isMuffled) {
            previouslyMuffled = isMuffled;

            if (recipeLogic != null)
                recipeLogic.updateSound();
        }
    }

    @Override
    public boolean keepSubscribing() {
        return false;
    }

    @NotNull
    public GTRecipeType getRecipeType() {
        return recipeTypes[activeRecipeType];
    }

    /**
     * Sets a recipe type of the machine.
     * FOR INTERNAL / TESTING USE ONLY!
     * NOT SUPPORTED FOR PRODUCTION USE!
     *
     * @param newType The new recipe type
     */
    @ApiStatus.Internal
    @VisibleForTesting
    public void setRecipeType(GTRecipeType newType) {
        recipeTypes[activeRecipeType] = newType;
    }
}
