package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import lombok.Getter;

import java.util.List;
import java.util.function.BiConsumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CoilWorkableElectricMultiblockMachine extends WorkableElectricMultiblockMachine {

    public static final BiConsumer<IMultiController, List<Component>> BLAST_DISPLAY = (controller, components) -> {
        if (controller instanceof CoilWorkableElectricMultiblockMachine coilMachine && controller.isFormed()) {
            int machineTemp = GTRecipeModifiers.ebfMachineTemp(coilMachine.getCoilType(), coilMachine.getTier());

            components.add(Component.translatable("gtceu.multiblock.blast_furnace.max_temperature",
                    Component.literal(FormattingUtil.formatNumbers(machineTemp) + "K")
                            .withStyle(ChatFormatting.RED)));
        }
    };

    public static final BiConsumer<IMultiController, List<Component>> SMELTER_DISPLAY = (controller, components) -> {
        if (controller instanceof CoilWorkableElectricMultiblockMachine coilMachine && controller.isFormed()) {
            components.add(Component.translatable("gtceu.multiblock.multi_furnace.heating_coil_level",
                    coilMachine.getCoilType().getLevel()));
            components.add(Component.translatable("gtceu.multiblock.multi_furnace.heating_coil_discount",
                    coilMachine.getCoilType().getEnergyDiscount()));
        }
    };

    public static final BiConsumer<IMultiController, List<Component>> CRACKER_DISPLAY = (controller, components) -> {
        if (controller instanceof CoilWorkableElectricMultiblockMachine coilMachine && controller.isFormed()) {
            double energy = GTRecipeModifiers.crackerEUtMultiplier(coilMachine.getCoilTier()) * 100.0;

            components.add(Component.translatable("gtceu.multiblock.cracking_unit.energy",
                    FormattingUtil.DECIMAL_FORMAT_0F.format(energy)));
        }
    };

    public static final BiConsumer<IMultiController, List<Component>> PYROLYSE_DISPLAY = (controller, components) -> {
        if (controller instanceof CoilWorkableElectricMultiblockMachine coilMachine && controller.isFormed()) {
            components.add(Component.translatable("gtceu.multiblock.pyrolyse_oven.speed",
                    Math.round(100 / GTRecipeModifiers.pyrolyseDurationMultiplier(coilMachine.getCoilTier()))));
        }
    };

    public static final BiConsumer<IMultiController, List<Component>> CHEMICAL_DISPLAY = (controller, components) -> {
        if (controller instanceof CoilWorkableElectricMultiblockMachine coilMachine && controller.isFormed()) {
            int coilTier = coilMachine.getCoilTier();

            components.add(Component.translatable("gtceu.multiblock.chemical_reactor.speed",
                    Math.round(100 / GTRecipeModifiers.chemicalReactorDurationMultiplier(coilTier))));
            components.add(Component.translatable("gtceu.multiblock.chemical_reactor.energy",
                    Math.round(GTRecipeModifiers.chemicalReactorEUtMultiplier(coilTier) * 100)));
        }
    };

    @Getter
    private ICoilType coilType = CoilBlock.CoilType.CUPRONICKEL;

    public CoilWorkableElectricMultiblockMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    //////////////////////////////////////
    // *** Multiblock LifeCycle ***//
    //////////////////////////////////////
    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        var type = getMultiblockState().getMatchContext().get("CoilType");
        if (type instanceof ICoilType coil) {
            this.coilType = coil;
        }
    }

    public int getCoilTier() {
        return coilType.getTier();
    }
}
