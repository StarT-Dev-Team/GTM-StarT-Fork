package com.gregtechceu.gtceu.api.machine.trait;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.ILazyTickableMachine;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

public class LazyTickableTrait extends MachineTrait {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(LazyTickableTrait.class);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    private final Runnable callback;

    private TickableSubscription subscription;
    private final int tickRate;
    private int tickCounter;

    public LazyTickableTrait(ILazyTickableMachine machine, int rate) {
        this(machine.self(), machine::lazyTick, rate);
    }

    public LazyTickableTrait(MetaMachine machine, Runnable callback, int rate) {
        super(machine);
        this.callback = callback;
        this.tickRate = rate;
        this.tickCounter = tickRate;
    }

    @Override
    public void onMachineLoad() {
        subscription = machine.subscribeServerTick(subscription, this::lazyTick);
    }

    @Override
    public void onMachineUnLoad() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    private void lazyTick() {
        if (tickCounter-- <= 0) {
            tickCounter = tickRate;
            callback.run();
        }
    }
}
