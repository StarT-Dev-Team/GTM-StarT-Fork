package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import com.lowdragmc.lowdraglib.syncdata.payload.ObjectTypedPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraftforge.server.ServerLifecycleHooks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import org.jetbrains.annotations.Nullable;

public class GTRecipePayload extends ObjectTypedPayload<GTRecipe> {

    private static RecipeManager getRecipeManager() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && Thread.currentThread() == server.getRunningThread()) {
            return server.getRecipeManager();
        } else {
            return Client.getRecipeManager();
        }
    }

    @Nullable
    @Override
    public Tag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", payload.id.toString());
        tag.put("recipe",
                GTRecipeSerializer.CODEC.encodeStart(NbtOps.INSTANCE, payload).result().orElse(new CompoundTag()));
        tag.putInt("parallels", payload.parallels);
        tag.put("parallelsByType", GTRecipeSerializer.PARALLELS_BY_TYPE_CODEC
                .encodeStart(NbtOps.INSTANCE, payload.parallelsByType).result().orElse(new CompoundTag()));
        tag.putInt("ocLevel", payload.ocLevel);
        tag.putInt("baseOcLevel", payload.baseOcLevel);
        return tag;
    }

    @Override
    public void deserializeNBT(Tag tag) {
        RecipeManager recipeManager = getRecipeManager();
        if (tag instanceof CompoundTag compoundTag) {
            payload = GTRecipeSerializer.CODEC.parse(NbtOps.INSTANCE, compoundTag.get("recipe")).result().orElse(null);
            if (payload != null) {
                payload.id = new ResourceLocation(compoundTag.getString("id"));
                payload.parallels = compoundTag.contains("parallels") ? compoundTag.getInt("parallels") : 1;
                payload.ocLevel = compoundTag.getInt("ocLevel");
                payload.baseOcLevel = compoundTag.getInt("baseOcLevel");
                if (compoundTag.contains("parallelsByType")) {
                    GTRecipeSerializer.PARALLELS_BY_TYPE_CODEC
                            .parse(NbtOps.INSTANCE, compoundTag.get("parallelsByType")).result()
                            .ifPresent(parallelsByType -> payload.parallelsByType = parallelsByType);
                }
            }
        } else if (tag instanceof StringTag stringTag) { // Backwards Compatibility
            var recipe = recipeManager.byKey(new ResourceLocation(stringTag.getAsString())).orElse(null);
            if (recipe instanceof GTRecipe gtRecipe) {
                payload = gtRecipe;
            } else if (recipe instanceof SmeltingRecipe smeltingRecipe) {
                payload = GTRecipeTypes.FURNACE_RECIPES.toGTrecipe(new ResourceLocation(stringTag.getAsString()),
                        smeltingRecipe);
            } else {
                payload = null;
            }
        } else if (tag instanceof ByteArrayTag byteArray) { // Backwards Compatibility
            ByteBuf copiedDataBuffer = Unpooled.copiedBuffer(byteArray.getAsByteArray());
            FriendlyByteBuf buf = new FriendlyByteBuf(copiedDataBuffer);
            payload = (GTRecipe) recipeManager.byKey(buf.readResourceLocation()).orElse(null);
            buf.release();
        }
    }

    @Override
    public void writePayload(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.payload.id);
        GTRecipeSerializer.SERIALIZER.toNetwork(buf, this.payload);
        buf.writeInt(this.payload.parallels);
        buf.writeInt(this.payload.ocLevel);
        buf.writeInt(this.payload.baseOcLevel);
        buf.writeMap(this.payload.parallelsByType,
                (buf1, type) -> buf1.writeUtf(GTRegistries.PARALLEL_TYPES.getKey(type)),
                FriendlyByteBuf::writeInt);
    }

    @Override
    public void readPayload(FriendlyByteBuf buf) {
        var id = buf.readResourceLocation();
        if (buf.isReadable()) {
            payload = GTRecipeSerializer.SERIALIZER.fromNetwork(id, buf);
            if (buf.isReadable()) {
                payload.parallels = buf.readInt();
                payload.ocLevel = buf.readInt();
                payload.baseOcLevel = buf.readInt();
            }
            if (buf.isReadable()) {
                payload.parallelsByType = new Reference2IntArrayMap<>(buf.readMap(
                        (buf1) -> GTRegistries.PARALLEL_TYPES.get(buf1.readUtf()),
                        FriendlyByteBuf::readInt));
            }
        } else { // Backwards Compatibility
            RecipeManager recipeManager = getRecipeManager();
            payload = (GTRecipe) recipeManager.byKey(id).orElse(null);
        }
    }

    static class Client {

        static RecipeManager getRecipeManager() {
            return Minecraft.getInstance().getConnection().getRecipeManager();
        }
    }
}
