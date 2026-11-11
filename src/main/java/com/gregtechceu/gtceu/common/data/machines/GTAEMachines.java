package com.gregtechceu.gtceu.common.data.machines;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.integration.ae2.machine.*;
import com.gregtechceu.gtceu.integration.ae2.slot.PortAEInventory;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMappings;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

import static com.gregtechceu.gtceu.api.GTValues.EV;
import static com.gregtechceu.gtceu.api.GTValues.LuV;
import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

@SuppressWarnings("unused")
public class GTAEMachines {

    public static final MachineDefinition[] ITEM_IMPORT_BUSES_ME = registerTieredMEPart("me_input_bus", "ME Input Bus",
            GTMachineUtils.ME_INPUT_TIERS, MEInputBusPartMachine::new, PartAbility.IMPORT_ITEMS, "me_input_bus",
            "gtceu.universal.tooltip.item_storage_capacity",
            Component.translatable("gtceu.machine.item_bus.import.tooltip"),
            Component.translatable("gtceu.machine.me.item_import.tooltip"));
    public static final MachineDefinition ITEM_IMPORT_BUS_ME = ITEM_IMPORT_BUSES_ME[GTMachineUtils.ME_INPUT_TIERS[0]];

    public static final MachineDefinition[] FLUID_IMPORT_HATCHES_ME = registerTieredMEPart("me_input_hatch",
            "ME Input Hatch", GTMachineUtils.ME_INPUT_TIERS, MEInputHatchPartMachine::new, PartAbility.IMPORT_FLUIDS,
            "me_input_hatch", "gtceu.machine.me.fluid_slots.tooltip",
            Component.translatable("gtceu.machine.fluid_hatch.import.tooltip"),
            Component.translatable("gtceu.machine.me.fluid_import.tooltip"));
    public static final MachineDefinition FLUID_IMPORT_HATCH_ME = FLUID_IMPORT_HATCHES_ME[GTMachineUtils.ME_INPUT_TIERS[0]];

    public static final MachineDefinition[] STOCKING_IMPORT_BUSES_ME = registerTieredMEPart("me_stocking_input_bus",
            "ME Stocking Input Bus", GTMachineUtils.ME_STOCKING_TIERS, MEStockingBusPartMachine::new,
            PartAbility.IMPORT_ITEMS, "me_stocking_input_bus", "gtceu.universal.tooltip.item_storage_capacity",
            Component.translatable("gtceu.machine.item_bus.import.tooltip"),
            Component.translatable("gtceu.machine.me.stocking_item.tooltip.0"),
            Component.translatable("gtceu.machine.me.stocking_item.tooltip.1"));
    public static final MachineDefinition STOCKING_IMPORT_BUS_ME = STOCKING_IMPORT_BUSES_ME[GTMachineUtils.ME_STOCKING_TIERS[0]];

    public static final MachineDefinition[] STOCKING_IMPORT_HATCHES_ME = registerTieredMEPart("me_stocking_input_hatch",
            "ME Stocking Input Hatch", GTMachineUtils.ME_STOCKING_TIERS, MEStockingHatchPartMachine::new,
            PartAbility.IMPORT_FLUIDS, "me_stocking_input_hatch", "gtceu.machine.me.fluid_slots.tooltip",
            Component.translatable("gtceu.machine.fluid_hatch.import.tooltip"),
            Component.translatable("gtceu.machine.me.stocking_fluid.tooltip.0"),
            Component.translatable("gtceu.machine.me.stocking_fluid.tooltip.1"));
    public static final MachineDefinition STOCKING_IMPORT_HATCH_ME = STOCKING_IMPORT_HATCHES_ME[GTMachineUtils.ME_STOCKING_TIERS[0]];

    public final static MachineDefinition ITEM_EXPORT_BUS_ME = REGISTRATE
            .machine("me_output_bus", MEOutputBusPartMachine::new)
            .langValue("ME Output Bus")
            .tier(EV)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.EXPORT_ITEMS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_output_bus"))
            .tooltips(
                    Component.translatable("gtceu.machine.item_bus.export.tooltip"),
                    Component.translatable("gtceu.machine.me.item_export.tooltip"),
                    Component.translatable("gtceu.machine.me.export.tooltip"),
                    Component.empty(),
                    Component.translatable("gtceu.machine.me.expose_sides.tooltip"),
                    Component.empty(),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .register();

    public final static MachineDefinition FLUID_EXPORT_HATCH_ME = REGISTRATE
            .machine("me_output_hatch", MEOutputHatchPartMachine::new)
            .langValue("ME Output Hatch")
            .tier(EV)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.EXPORT_FLUIDS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_output_hatch"))
            .tooltips(
                    Component.translatable("gtceu.machine.fluid_hatch.export.tooltip"),
                    Component.translatable("gtceu.machine.me.fluid_export.tooltip"),
                    Component.translatable("gtceu.machine.me.export.tooltip"),
                    Component.empty(),
                    Component.translatable("gtceu.machine.me.expose_sides.tooltip"),
                    Component.empty(),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .register();

    public static final MachineDefinition[] ME_PATTERN_BUFFERS = registerPatternBuffers();
    public static final MachineDefinition ME_PATTERN_BUFFER = ME_PATTERN_BUFFERS[GTMachineUtils.ME_PATTERN_BUFFER_TIERS[0]];

    public static final MachineDefinition ME_PATTERN_BUFFER_PROXY = REGISTRATE
            .machine("me_pattern_buffer_proxy", MEPatternBufferProxyPartMachine::new)
            .tier(LuV)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS)
            .rotationState(RotationState.ALL)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_buffer_hatch_proxy"))
            .langValue("ME Pattern Buffer Proxy")
            .tooltips(
                    Component.translatable("block.gtceu.pattern_buffer_proxy.desc.0"),
                    Component.empty(),
                    Component.translatable("gtceu.machine.me.expose_sides.tooltip"),
                    Component.empty(),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .register();

    public static final MachineDefinition[] INPUT_PORTS_ME = registerInputPorts();
    public static final MachineDefinition INPUT_PORT_ME = INPUT_PORTS_ME[GTMachineUtils.ME_INPUT_TIERS[0]];

    public static final MachineDefinition[] STOCKING_INPUT_PORTS_ME = registerStockingInputPorts();
    public static final MachineDefinition STOCKING_INPUT_PORT_ME = STOCKING_INPUT_PORTS_ME[GTMachineUtils.ME_STOCKING_TIERS[0]];

    public static final MachineDefinition OUTPUT_PORT_ME = REGISTRATE
            .machine("me_output_port", holder -> new MEOutputPortPartMachine(holder, EV))
            .langValue("ME Output Port")
            .tier(EV)
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.EXPORT_ITEMS, PartAbility.EXPORT_FLUIDS)
            .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_output_port"))
            .tooltips(
                    Component.translatable("gtceu.machine.dual_hatch.export.tooltip"),
                    Component.translatable("gtceu.machine.me.item_export.tooltip"),
                    Component.translatable("gtceu.machine.me.fluid_export.tooltip"),
                    Component.translatable("gtceu.machine.me.export.tooltip"),
                    Component.translatable("gtceu.machine.me.expose_sides.tooltip"),
                    Component.translatable("gtceu.part_sharing.enabled"))
            .register();

    private static MachineDefinition[] registerTieredMEPart(String id, String name, int[] tiers,
                                                            BiFunction<IMachineBlockEntity, Integer, MetaMachine> factory,
                                                            PartAbility ability, String overlay, String slotsKey,
                                                            Component... tooltips) {
        var definitions = new MachineDefinition[GTValues.TIER_COUNT];

        for (int tier : tiers) {
            var lines = new ArrayList<>(List.of(tooltips));

            lines.add(Component.translatable(slotsKey, PortAEInventory.slotsForTier(tier, tiers[0])));
            lines.add(Component.empty());
            lines.add(Component.translatable("gtceu.machine.me.copy_settings.tooltip",
                    SyncedKeyMappings.VANILLA_SNEAK.getBracketedKeybind()));
            lines.add(Component.translatable("gtceu.machine.me.paste_settings.tooltip"));
            lines.add(Component.translatable("gtceu.machine.me.expose_sides.tooltip"));
            lines.add(Component.empty());
            lines.add(Component.translatable("gtceu.part_sharing.enabled"));

            definitions[tier] = REGISTRATE
                    .machine(tier == tiers[0] ? id : id + "_" + GTValues.VN[tier].toLowerCase(Locale.ROOT),
                            holder -> factory.apply(holder, tier))
                    .langValue(GTValues.VNF[tier] + " " + name)
                    .tier(tier)
                    .rotationState(RotationState.ALL)
                    .abilities(ability)
                    .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/" + overlay))
                    .tooltips(lines.toArray(Component[]::new))
                    .register();
        }

        return definitions;
    }

    private static MachineDefinition[] registerPatternBuffers() {
        var definitions = new MachineDefinition[GTValues.TIER_COUNT];

        for (int tier : GTMachineUtils.ME_PATTERN_BUFFER_TIERS) {
            int slots = MEPatternBufferPartMachine.patternCountForTier(tier);

            definitions[tier] = REGISTRATE
                    .machine(
                            tier == GTMachineUtils.ME_PATTERN_BUFFER_TIERS[0] ? "me_pattern_buffer" :
                                    "me_pattern_buffer_" + GTValues.VN[tier].toLowerCase(Locale.ROOT),
                            holder -> new MEPatternBufferPartMachine(holder, tier))
                    .tier(tier)
                    .rotationState(RotationState.ALL)
                    .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS)
                    .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_buffer_hatch"))
                    .langValue(GTValues.VNF[tier] + " ME Pattern Buffer")
                    .tooltips(
                            Component.translatable("block.gtceu.pattern_buffer.desc.0"),
                            Component.translatable("block.gtceu.pattern_buffer.desc.1"),
                            Component.translatable("gtceu.machine.me.pattern_slots.tooltip", slots),
                            Component.empty(),
                            Component.translatable("gtceu.machine.me.expose_sides.tooltip"),
                            Component.empty(),
                            Component.translatable("gtceu.part_sharing.enabled"))
                    .register();
        }

        return definitions;
    }

    private static MachineDefinition[] registerInputPorts() {
        var definitions = new MachineDefinition[GTValues.TIER_COUNT];

        for (int tier : GTMachineUtils.ME_INPUT_TIERS) {
            int slots = PortAEInventory.slotsForTier(tier, GTMachineUtils.ME_INPUT_TIERS[0]);

            definitions[tier] = REGISTRATE
                    .machine("me_input_port_" + GTValues.VN[tier].toLowerCase(Locale.ROOT),
                            holder -> new MEInputPortPartMachine(holder, tier))
                    .langValue(GTValues.VNF[tier] + " ME Input Port")
                    .tier(tier)
                    .rotationState(RotationState.ALL)
                    .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS)
                    .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_input_port"))
                    .tooltips(
                            Component.translatable("gtceu.machine.dual_hatch.import.tooltip"),
                            Component.translatable("gtceu.machine.me.item_import.tooltip"),
                            Component.translatable("gtceu.machine.me.fluid_import.tooltip"),
                            Component.translatable("gtceu.machine.me.port_slots.tooltip", slots),
                            Component.empty(),
                            Component.translatable("gtceu.machine.me.copy_settings.tooltip",
                                    SyncedKeyMappings.VANILLA_SNEAK.getBracketedKeybind()),
                            Component.translatable("gtceu.machine.me.paste_settings.tooltip"),
                            Component.translatable("gtceu.machine.me.expose_sides.tooltip"),
                            Component.empty(),
                            Component.translatable("gtceu.part_sharing.enabled"))
                    .register();
        }

        return definitions;
    }

    private static MachineDefinition[] registerStockingInputPorts() {
        var definitions = new MachineDefinition[GTValues.TIER_COUNT];

        for (int tier : GTMachineUtils.ME_STOCKING_TIERS) {
            int slots = PortAEInventory.slotsForTier(tier, GTMachineUtils.ME_STOCKING_TIERS[0]);

            definitions[tier] = REGISTRATE
                    .machine("me_stocking_input_port_" + GTValues.VN[tier].toLowerCase(Locale.ROOT),
                            holder -> new MEStockingInputPortPartMachine(holder, tier))
                    .langValue(GTValues.VNF[tier] + " ME Stocking Input Port")
                    .tier(tier)
                    .rotationState(RotationState.ALL)
                    .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS)
                    .colorOverlayTieredHullModel(GTCEu.id("block/overlay/appeng/me_stocking_input_port"))
                    .tooltips(
                            Component.translatable("gtceu.machine.dual_hatch.import.tooltip"),
                            Component.translatable("gtceu.machine.me.stocking_port.tooltip.0"),
                            Component.translatable("gtceu.machine.me.stocking_port.tooltip.1"),
                            Component.translatable("gtceu.machine.me.port_slots.tooltip", slots),
                            Component.empty(),
                            Component.translatable("gtceu.machine.me.copy_settings.tooltip",
                                    SyncedKeyMappings.VANILLA_SNEAK.getBracketedKeybind()),
                            Component.translatable("gtceu.machine.me.paste_settings.tooltip"),
                            Component.translatable("gtceu.machine.me.expose_sides.tooltip"),
                            Component.empty(),
                            Component.translatable("gtceu.part_sharing.enabled"))
                    .register();
        }

        return definitions;
    }

    public static void init() {}
}
