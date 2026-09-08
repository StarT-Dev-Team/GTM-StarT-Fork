package com.gregtechceu.gtceu.integration.kjs.builders.machine;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleMachine;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.common.registry.GTRegistration;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class KJSWrappingMachineBuilder extends MachineBuilder<MachineDefinition> {

    public KJSWrappingMachineBuilder(ResourceLocation id, Function<IMachineBlockEntity, MetaMachine> metaMachine) {
        super(GTRegistration.REGISTRATE, id.getPath(),
                MachineDefinition::new,
                metaMachine,
                MetaMachineBlock::new,
                MetaMachineItem::new, MetaMachineBlockEntity::new);
    }

    public static KJSWrappingMachineBuilder createKJSPrimitiveSingleblock(ResourceLocation id) {
        return new KJSWrappingMachineBuilder(id, SimpleMachine::new);
    }
}
