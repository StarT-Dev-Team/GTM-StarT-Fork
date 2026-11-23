package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.IntInput;
import com.gregtechceu.gtceu.api.gui.widget.TextToggleButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeComputationProviderMachine extends MetaMachine
                                                implements IUIMachine, IOpticalComputationProvider {

    @Persisted
    private int maxCWUt;
    private int lastRequestedCWUt;
    private int requestedCWUPerSec;
    @Persisted
    @Getter
    private boolean active;
    @Nullable
    private TickableSubscription computationSubs;

    public CreativeComputationProviderMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateComputationSubscription();
    }

    protected void updateComputationSubscription() {
        if (active) {
            this.computationSubs = subscribeServerTick(this::updateComputationTick);
        } else if (computationSubs != null) {
            computationSubs.unsubscribe();
            this.computationSubs = null;
            this.lastRequestedCWUt = 0;
            this.requestedCWUPerSec = 0;
        }
    }

    protected void updateComputationTick() {
        if (getOffsetTimer() % 20 == 0) {
            this.lastRequestedCWUt = requestedCWUPerSec / 20;
            this.requestedCWUPerSec = 0;
        }
    }

    @Override
    public int requestCWUt(
                           int cwut, boolean simulate, @NotNull Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        int requestedCWUt = active ? Math.min(cwut, maxCWUt) : 0;
        if (!simulate) {
            this.requestedCWUPerSec += requestedCWUt;
        }
        return requestedCWUt;
    }

    @Override
    public int getMaxCWUt(@NotNull Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return active ? maxCWUt : 0;
    }

    @Override
    public boolean canBridge(@NotNull Collection<IOpticalComputationProvider> seen) {
        seen.add(this);
        return true;
    }

    public void setActive(boolean active) {
        this.active = active;
        updateComputationSubscription();
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = UITemplate.panel();
        root.layout(layout -> layout.width(140).height(95).paddingAll(0));

        var title = new Label();
        title.setText("CWUt");
        title.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(7));

        var input = new IntInput(() -> maxCWUt, value -> maxCWUt = value);
        input.setMin(0);
        input.setMax(Integer.MAX_VALUE);
        input.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(9).top(20).width(122).height(16));

        var averageLabel = new Label();
        averageLabel.setText(Component.translatable("gtceu.creative.computation.average"));
        averageLabel.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(42));

        var requested = new Label();
        requested.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal(String.valueOf(lastRequestedCWUt))).build());
        requested.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(7).top(54));

        var activity = new TextToggleButton("gtceu.creative.activity.on", "gtceu.creative.activity.off",
                this::isActive, this::setActive);
        activity.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(9).top(66).width(122)
                .height(20));

        root.addChildren(title, input, averageLabel, requested, activity);
        return UITemplate.modularUI(root, entityPlayer);
    }
}
