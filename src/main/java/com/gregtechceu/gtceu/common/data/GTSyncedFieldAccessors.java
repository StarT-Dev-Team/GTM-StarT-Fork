package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry;
import com.gregtechceu.gtceu.api.misc.virtualregistry.entries.VirtualItemStorage;
import com.gregtechceu.gtceu.api.misc.virtualregistry.entries.VirtualRedstone;
import com.gregtechceu.gtceu.api.misc.virtualregistry.entries.VirtualTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;
import com.gregtechceu.gtceu.syncdata.GTRecipePayload;
import com.gregtechceu.gtceu.syncdata.MonitorGroupPayload;

import com.lowdragmc.lowdraglib2.compat.network.ByteBufCodecs;
import com.lowdragmc.lowdraglib2.compat.network.StreamCodec;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.function.Supplier;

public class GTSyncedFieldAccessors {

    public static void init() {
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(MachineRenderState.class)
                .codec(MachineRenderState.CODEC)
                .streamCodec(StreamCodec.<FriendlyByteBuf, MachineRenderState>of(
                        (buf, state) -> buf.writeId(MachineDefinition.RENDER_STATE_REGISTRY, state),
                        buf -> buf.readById(MachineDefinition.RENDER_STATE_REGISTRY)))
                .build(), 100);

        var materialCodec = Codec.STRING.flatXmap(
                name -> {
                    var material = GTCEuAPI.materialManager.getMaterial(name);
                    return DataResult.success(material == null ? GTMaterials.NULL : material);
                },
                material -> DataResult.success(material.getResourceLocation().toString()));
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(Material.class)
                .codec(materialCodec)
                .streamCodec(ByteBufCodecs.fromCodec(materialCodec))
                .build(), 100);

        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(GTRecipe.class)
                .codec(GTRecipePayload.CODEC)
                .streamCodec(GTRecipePayload.STREAM_CODEC)
                .build(), 100);

        var recipeTypeCodec = ResourceLocation.CODEC.flatXmap(
                id -> {
                    var type = GTRegistries.RECIPE_TYPES.get(id);
                    return type == null ? DataResult.<GTRecipeType>error(() -> "Unknown recipe type " + id) :
                            DataResult.success(type);
                },
                type -> DataResult.success(type.registryName));
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(GTRecipeType.class, true)
                .codec(recipeTypeCodec)
                .streamCodec(ByteBufCodecs.fromCodec(recipeTypeCodec))
                .build(), 1000);

        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(MonitorGroup.class)
                .codec(MonitorGroupPayload.CODEC)
                .streamCodec(MonitorGroupPayload.STREAM_CODEC)
                .customMark(MonitorGroupPayload::write, (mark, group) -> mark.equals(MonitorGroupPayload.write(group)))
                .build(), 100);

        registerVirtual(VirtualTank.class, VirtualTank::new);
        registerVirtual(VirtualItemStorage.class, VirtualItemStorage::new);
        registerVirtual(VirtualRedstone.class, VirtualRedstone::new);
    }

    private static <T extends VirtualEntry> void registerVirtual(Class<T> type, Supplier<T> factory) {
        var codec = CompoundTag.CODEC.xmap(tag -> {
            var entry = factory.get();
            entry.deserializeNBT(tag);
            return entry;
        }, VirtualEntry::serializeNBT);
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(type, true)
                .codec(codec)
                .streamCodec(ByteBufCodecs.fromCodec(codec))
                .customMark(VirtualEntry::serializeNBT, (mark, entry) -> mark.equals(entry.serializeNBT()))
                .build(), 1000);
    }
}
