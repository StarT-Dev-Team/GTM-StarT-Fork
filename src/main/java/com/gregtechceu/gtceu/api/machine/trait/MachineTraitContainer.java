package com.gregtechceu.gtceu.api.machine.trait;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.lowdragmc.lowdraglib.syncdata.*;
import com.lowdragmc.lowdraglib.syncdata.accessor.ReadonlyAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MachineTraitContainer implements IContentChangeAware {

    private final MetaMachine machine;

    private List<MachineTrait> traits = new ObjectArrayList<>();

    private final Map<String, MachineTrait> namedTraits = new Object2ObjectOpenHashMap<>();

    private Map<Class<?>, List<MachineTrait>> traitsByClass = new Object2ObjectOpenHashMap<>();

    private final Set<String> changedNamedTraits = new HashSet<>();

    @Getter
    @Setter
    private Runnable onContentsChanged;

    public MachineTraitContainer(MetaMachine machine) {
        this.machine = machine;
    }

    public void onMachineLoad() {
        traitsByClass.clear();

        traits.sort(Comparator.comparing(MachineTrait::getPriority).reversed());
        traits = Collections.unmodifiableList(traits);

        traits.forEach(trait -> computeTraitClassMap(trait.getClass(), trait));
        traitsByClass = traitsByClass.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> {
            var traits = e.getValue();
            traits.sort(Comparator.comparing(MachineTrait::getPriority).reversed());
            return Collections.unmodifiableList(traits);
        }));

        traits.forEach(MachineTrait::onMachineLoad);
    }

    @SuppressWarnings("unchecked")
    private void computeTraitClassMap(Class<? extends IMachineTrait> cls, MachineTrait trait) {
        var list = traitsByClass.computeIfAbsent(cls, c -> new ObjectArrayList<>());
        list.add(trait);
        if (!cls.getSuperclass().equals(MachineTrait.class) && !cls.getSuperclass().equals(IMachineTrait.class)) {
            computeTraitClassMap((Class<? extends IMachineTrait>) cls.getSuperclass(), trait);
        }
        for (var inf : cls.getInterfaces()) {
            if (!inf.equals(IMachineTrait.class) && IMachineTrait.class.isAssignableFrom(inf)) {
                computeTraitClassMap((Class<? extends IMachineTrait>) inf, trait);
            }
        }
    }

    public void onMachineUnLoad() {
        traits.forEach(MachineTrait::onMachineUnLoad);
    }

    public @Unmodifiable List<MachineTrait> getAllTraits() {
        if (!machine.isLoaded())
            throw new IllegalStateException("Cannot access traits of machine before it has been loaded.");
        return traits;
    }

    @SuppressWarnings("unchecked")
    public <T extends IMachineTrait> @Unmodifiable List<T> getTraitsByType(Class<T> cls) {
        if (!machine.isLoaded())
            throw new IllegalStateException("Cannot access traits of machine before it has been loaded.");
        var traits = traitsByClass.get(cls);
        if (traits == null) return List.of();
        return (List<T>) traits;
    }

    public <T extends MachineTrait> @Nullable T getTrait(Class<T> cls) {
        var list = getTraitsByType(cls);
        if (list.isEmpty()) return null;
        return list.get(0);
    }

    @SuppressWarnings("unchecked")
    public <T extends MachineTrait> @Nullable T getNamedTrait(String name) {
        var trait = namedTraits.get(name);
        return trait == null ? null : (T) trait;
    }

    public <T extends MachineTrait> Optional<T> getTraitOptional(Class<T> cls) {
        return Optional.ofNullable(getTrait(cls));
    }

    public <T extends MachineTrait> T attachTrait(T trait) {
        attachTrait(trait, 1);
        return trait;
    }

    public <T extends MachineTrait> T attachTrait(T trait, int priority) {
        if (machine.isLoaded())
            throw new IllegalStateException("Cannot add traits to machine after it has been loaded.");
        trait.setPriority(priority);
        trait.setMachine(machine);
        traits.add(trait);
        trait.onTraitAttached();
        return trait;
    }

    public void attachNamedTrait(String name, MachineTrait trait) {
        attachNamedTrait(name, trait, 1);
    }

    public void attachNamedTrait(String name, MachineTrait trait, int priority) {
        attachTrait(trait, priority);
        if (namedTraits.containsKey(name))
            throw new IllegalStateException("Attempted to register duplicate trait save key \"" + name + "\"");
        trait.setPersistedName(name);

        Arrays.stream(trait.getFieldHolder().getFields())
                .filter(ManagedKey::isDestSync)
                .forEach(field -> trait.getSyncStorage().addSyncUpdateListener(field,
                        (_f, _v, _p) -> onTraitChange(name)));

        namedTraits.put(name, trait);
    }

    private void onTraitChange(String traitName) {
        changedNamedTraits.add(traitName);
        if (onContentsChanged != null) onContentsChanged.run();
    }

    public static class MachineTraitContainerAccessor extends ReadonlyAccessor {

        public static MachineTraitContainerAccessor INSTANCE = new MachineTraitContainerAccessor();

        private static final Class<?>[] OPERAND_TYPES = new Class<?>[] { MachineTraitContainer.class };

        public boolean hasPredicate() {
            return true;
        }

        public boolean test(Class<?> type) {
            return MachineTraitContainer.class.isAssignableFrom(type);
        }

        public Class<?>[] operandTypes() {
            return OPERAND_TYPES;
        }

        @Override
        public ITypedPayload<?> readFromReadonlyField(AccessorOp op, Object obj) {
            if (!(obj instanceof MachineTraitContainer value)) {
                throw new IllegalArgumentException("Field %s is not MachineTraitContainer".formatted(obj));
            }

            var tag = new CompoundTag();
            for (var entry : value.namedTraits.entrySet()) {
                var traitName = entry.getKey();

                if (op == AccessorOp.SYNCED && !value.changedNamedTraits.contains(traitName)) {
                    continue;
                }

                var trait = entry.getValue();

                var payload = SyncedFieldAccessors.MANAGED_ACCESSOR.readFromReadonlyField(op, trait);
                var nbt = payload.serializeNBT();
                if (nbt == null) continue;

                tag.put(traitName, nbt);
            }

            if (op == AccessorOp.SYNCED) {
                value.changedNamedTraits.clear();
            }

            return new NbtTagPayload().setPayload(tag);
        }

        @Override
        public void writeToReadonlyField(AccessorOp op, Object obj, ITypedPayload<?> rootPayload) {
            if (!(obj instanceof MachineTraitContainer value)) {
                throw new IllegalArgumentException("Field %s is not MachineTraitContainer".formatted(obj));
            }

            if (!(rootPayload instanceof NbtTagPayload rootNbtPayload &&
                    rootNbtPayload.getPayload() instanceof CompoundTag rootTag)) {
                throw new IllegalArgumentException("Payload %s is not NbtTagPayload".formatted(rootPayload));
            }

            for (var traitName : rootTag.getAllKeys()) {
                var trait = value.namedTraits.get(traitName);
                if (trait == null) {
                    throw new IllegalStateException(
                            "Reading packet data for syncable trait '%s', but no client-side syncable trait has that ID"
                                    .formatted(traitName));
                }

                var payload = NbtTagPayload.of(rootTag.get(traitName));
                SyncedFieldAccessors.MANAGED_ACCESSOR.writeToReadonlyField(op, trait, payload);
            }
        }
    }
}
