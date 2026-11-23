package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.GTCEu;

import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import com.lowdragmc.lowdraglib2.syncdata.ref.ReadOnlyManagedRef;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import com.lowdragmc.lowdraglib2.utils.TagUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

public final class LegacyManagedData {

    public static final String MANAGED = "managed";
    public static final String CUSTOM = "custom";
    private static final Set<String> VANILLA_KEYS = Set.of("id", "x", "y", "z", "ForgeData", "ForgeCaps",
            "keepPacked");

    private LegacyManagedData() {}

    public static void upgradeBlockEntityTag(IManagedStorage storage, CompoundTag tag, String syncTag) {
        if (tag.contains(MANAGED, Tag.TAG_COMPOUND) || tag.contains(syncTag, Tag.TAG_COMPOUND)) return;
        try {
            tag.put(MANAGED, toManagedLayout(storage, tag));
        } catch (Exception e) {
            GTCEu.LOGGER.error("Failed to migrate legacy managed data", e);
        }
    }

    public static CompoundTag wrapFlatTag(IManagedStorage storage, CompoundTag flat) {
        if (flat.contains(MANAGED, Tag.TAG_COMPOUND)) return flat;
        var wrapped = new CompoundTag();
        wrapped.put(MANAGED, toManagedLayout(storage, flat));
        return wrapped;
    }

    public static void flattenInto(CompoundTag saved, CompoundTag out) {
        var managed = saved.getCompound(MANAGED);
        for (var key : managed.getAllKeys()) {
            if (!CUSTOM.equals(key)) {
                out.put(key, managed.get(key));
            }
        }
        out.merge(managed.getCompound(CUSTOM));
    }

    public static CompoundTag toManagedLayout(IManagedStorage storage, CompoundTag flat) {
        var managed = convertManaged(storage, flat, true);
        var custom = new CompoundTag();
        for (var key : flat.getAllKeys()) {
            if (!VANILLA_KEYS.contains(key) && !MANAGED.equals(key)) {
                var value = flat.get(key);
                if (value != null) custom.put(key, value.copy());
            }
        }
        if (!custom.isEmpty()) {
            managed.put(CUSTOM, custom);
        }
        return managed;
    }

    private static CompoundTag convertManaged(IManagedStorage storage, CompoundTag old, boolean extended) {
        var result = new CompoundTag();
        for (var ref : storage.getPersistedFields()) {
            var key = ref.getPersistedKey();
            var value = TagUtils.getTagExtended(old, key);
            if (value == null) continue;
            var converted = convertRef(ref, value);
            if (converted == null) continue;
            if (extended) {
                TagUtils.setTagExtended(result, key, converted);
            } else {
                result.put(key, converted);
            }
        }
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Nullable
    private static Tag convertRef(IRef<?> ref, Tag old) {
        if (ref instanceof ReadOnlyManagedRef<?> readOnlyRef && readOnlyRef.isReadOnlyManaged()) {
            if (!(old instanceof CompoundTag wrapper) || !wrapper.contains("uid")) {
                return old;
            }
            var uid = wrapper.get("uid");
            var payload = wrapper.get("payload");
            if (payload instanceof CompoundTag payloadTag && isTypedPayload(payloadTag)) {
                payload = payloadTag.get("d");
            }
            var readOnlyVar = (ReadOnlyManagedRef) readOnlyRef;
            var managedVar = readOnlyVar.getReadOnlyVar().getManagedVar();
            Object value = readOnlyRef.readRaw();
            if (managedVar != null && (value == null || !managedVar.serializeUid(value).equals(uid))) {
                value = managedVar.deserializeUid(uid);
                readOnlyVar.getReadOnlyVar().set(value);
            }
            var result = new CompoundTag();
            result.put("uid", uid);
            if (payload != null) {
                var converted = convert(ref.getKey().getContentType(), value, payload);
                if (converted != null) result.put("payload", converted);
            }
            return result;
        }
        return convert(ref.getKey().getContentType(), ref.readRaw(), old);
    }

    private static boolean isTypedPayload(CompoundTag tag) {
        if (!tag.contains("t", Tag.TAG_BYTE)) return false;
        for (var key : tag.getAllKeys()) {
            if (!key.equals("t") && !key.equals("d") && !key.equals("p")) return false;
        }
        return true;
    }

    @Nullable
    private static Tag convert(Type type, @Nullable Object instance, Tag old) {
        if (instance instanceof IManaged managed) {
            return old instanceof CompoundTag compound ?
                    convertManaged(managed.getSyncStorage(), compound, false) : old;
        }
        Type componentType = null;
        if (type instanceof GenericArrayType arrayType) {
            componentType = arrayType.getGenericComponentType();
        } else if (type instanceof Class<?> clazz && clazz.isArray()) {
            componentType = clazz.getComponentType();
        } else {
            var raw = ReflectionUtils.getRawType(type);
            if (raw != null && Collection.class.isAssignableFrom(raw) &&
                    type instanceof ParameterizedType parameterized &&
                    parameterized.getActualTypeArguments().length == 1) {
                componentType = parameterized.getActualTypeArguments()[0];
            }
        }
        if (componentType != null) {
            return convertList(componentType, instance, old);
        }
        var raw = type instanceof Class<?> clazz ? clazz : ReflectionUtils.getRawType(type);
        if (raw == null) return old;
        if (raw.isEnum()) {
            if (old instanceof NumericTag numeric) {
                var constants = raw.getEnumConstants();
                var ordinal = numeric.getAsInt();
                if (ordinal >= 0 && ordinal < constants.length) {
                    return StringTag.valueOf(enumName((Enum<?>) constants[ordinal]));
                }
                return null;
            }
            return old;
        }
        if (raw == UUID.class) {
            if (old instanceof IntArrayTag intArray && intArray.size() == 4) {
                return StringTag.valueOf(NbtUtils.loadUUID(intArray).toString());
            }
            return old;
        }
        if (raw == BlockPos.class) {
            if (old instanceof CompoundTag compound && compound.contains("X") && compound.contains("Y") &&
                    compound.contains("Z")) {
                var pos = NbtUtils.readBlockPos(compound);
                return new IntArrayTag(new int[] { pos.getX(), pos.getY(), pos.getZ() });
            }
            return old;
        }
        if (Component.class.isAssignableFrom(raw)) {
            if (old instanceof StringTag string) {
                Component component;
                try {
                    component = Component.Serializer.fromJson(string.getAsString());
                } catch (Exception e) {
                    component = null;
                }
                if (component == null) {
                    component = Component.literal(string.getAsString());
                }
                return ExtraCodecs.COMPONENT.encodeStart(NbtOps.INSTANCE, component).result().orElse(old);
            }
            return old;
        }
        return old;
    }

    private static Tag convertList(Type componentType, @Nullable Object instance, Tag old) {
        if (!(old instanceof ListTag list) || list.isEmpty()) return old;
        for (var element : list) {
            if (!(element instanceof CompoundTag compound) || !isTypedPayload(compound)) {
                return old;
            }
        }
        Iterator<?> instances = null;
        if (instance instanceof Collection<?> collection && collection.size() == list.size()) {
            instances = collection.iterator();
        } else if (instance != null && instance.getClass().isArray() && Array.getLength(instance) == list.size()) {
            instances = IntStream.range(0, list.size()).mapToObj(i -> Array.get(instance, i))
                    .iterator();
        }
        var result = new ListTag();
        for (var element : list) {
            var compound = (CompoundTag) element;
            var child = instances != null ? instances.next() : null;
            var payload = compound.get("p");
            var converted = payload == null ? null : convert(componentType, child, payload);
            result.addTag(result.size(),
                    converted == null ? StringTag.valueOf(LDLibExtraCodecs.NULL_STRING) : converted);
        }
        return result;
    }

    private static String enumName(Enum<?> value) {
        if (value instanceof StringRepresentable representable) {
            return representable.getSerializedName();
        }
        return value.name();
    }
}
