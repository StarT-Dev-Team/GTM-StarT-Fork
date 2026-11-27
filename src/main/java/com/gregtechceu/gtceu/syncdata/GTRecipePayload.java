package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import com.lowdragmc.lowdraglib2.compat.network.StreamCodec;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraftforge.server.ServerLifecycleHooks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import org.jetbrains.annotations.Nullable;

public final class GTRecipePayload {

    public static final Codec<GTRecipe> CODEC = Codec.PASSTHROUGH.flatXmap(
            dynamic -> {
                var recipe = read(dynamic.convert(NbtOps.INSTANCE).getValue());
                return recipe == null ? DataResult.error(() -> "Unknown GT recipe") : DataResult.success(recipe);
            },
            recipe -> DataResult.success(new Dynamic<>(NbtOps.INSTANCE, write(recipe))));

    public static final StreamCodec<FriendlyByteBuf, GTRecipe> STREAM_CODEC = StreamCodec.of(
            GTRecipePayload::writeBuf, GTRecipePayload::readBuf);

    private GTRecipePayload() {}

    private static RecipeManager getRecipeManager() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && Thread.currentThread() == server.getRunningThread()) {
            return server.getRecipeManager();
        } else {
            return Client.getRecipeManager();
        }
    }

    public static Tag write(GTRecipe recipe) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", recipe.id.toString());
        tag.put("recipe",
                GTRecipeSerializer.CODEC.encodeStart(NbtOps.INSTANCE, recipe).result().orElse(new CompoundTag()));
        tag.putInt("parallels", recipe.parallels);
        tag.put("parallelsByType", GTRecipeSerializer.PARALLELS_BY_TYPE_CODEC
                .encodeStart(NbtOps.INSTANCE, recipe.parallelsByType).result().orElse(new CompoundTag()));
        tag.putInt("ocLevel", recipe.ocLevel);
        tag.putInt("baseOcLevel", recipe.baseOcLevel);
        return tag;
    }

    @Nullable
    public static GTRecipe read(Tag tag) {
        RecipeManager recipeManager = getRecipeManager();
        GTRecipe payload = null;
        if (tag instanceof CompoundTag compoundTag) {
            payload = GTRecipeSerializer.CODEC.parse(NbtOps.INSTANCE, compoundTag.get("recipe")).result().orElse(null);
            if (payload != null) {
                payload.id = new ResourceLocation(compoundTag.getString("id"));
                payload.parallels = compoundTag.contains("parallels") ? compoundTag.getInt("parallels") : 1;
                payload.ocLevel = compoundTag.getInt("ocLevel");
                payload.baseOcLevel = compoundTag.getInt("baseOcLevel");
                if (compoundTag.contains("parallelsByType")) {
                    var finalPayload = payload;
                    GTRecipeSerializer.PARALLELS_BY_TYPE_CODEC
                            .parse(NbtOps.INSTANCE, compoundTag.get("parallelsByType")).result()
                            .ifPresent(parallelsByType -> finalPayload.parallelsByType = parallelsByType);
                }
            }
        } else if (tag instanceof StringTag stringTag) {
            var recipe = recipeManager.byKey(new ResourceLocation(stringTag.getAsString())).orElse(null);
            if (recipe instanceof GTRecipe gtRecipe) {
                payload = gtRecipe;
            } else if (recipe instanceof SmeltingRecipe smeltingRecipe) {
                payload = GTRecipeTypes.FURNACE_RECIPES.toGTrecipe(new ResourceLocation(stringTag.getAsString()),
                        smeltingRecipe);
            }
        } else if (tag instanceof ByteArrayTag byteArray) {
            ByteBuf copiedDataBuffer = Unpooled.copiedBuffer(byteArray.getAsByteArray());
            FriendlyByteBuf buf = new FriendlyByteBuf(copiedDataBuffer);
            payload = (GTRecipe) recipeManager.byKey(buf.readResourceLocation()).orElse(null);
            buf.release();
        }
        return payload;
    }

    private static void writeBuf(FriendlyByteBuf buf, GTRecipe recipe) {
        buf.writeResourceLocation(recipe.id);
        GTRecipeSerializer.SERIALIZER.toNetwork(buf, recipe);
        buf.writeInt(recipe.parallels);
        buf.writeInt(recipe.ocLevel);
        buf.writeInt(recipe.baseOcLevel);
        buf.writeMap(recipe.parallelsByType,
                (buf1, type) -> buf1.writeUtf(GTRegistries.PARALLEL_TYPES.getKey(type)),
                FriendlyByteBuf::writeInt);
    }

    private static GTRecipe readBuf(FriendlyByteBuf buf) {
        var id = buf.readResourceLocation();
        GTRecipe payload;
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
        } else {
            RecipeManager recipeManager = getRecipeManager();
            payload = (GTRecipe) recipeManager.byKey(id).orElse(null);
        }
        return payload;
    }

    static class Client {

        static RecipeManager getRecipeManager() {
            return Minecraft.getInstance().getConnection().getRecipeManager();
        }
    }
}
