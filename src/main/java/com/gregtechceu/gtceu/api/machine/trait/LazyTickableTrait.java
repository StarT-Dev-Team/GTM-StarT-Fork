package com.gregtechceu.gtceu.api.machine.trait;

import com.gregtechceu.gtceu.api.machine.TickableSubscription;

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

    public LazyTickableTrait(Runnable callback, int rate) {
        super();
        this.callback = callback;
        this.tickRate = rate;
        this.tickCounter = tickRate;
    }

    @Override
    public void onMachineLoad() {
        assert machine != null;
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
