package com.gregtechceu.gtceu.common.machine.steam;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.ProgressTextureBar;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.steam.SteamBoilerMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jetbrains.annotations.NotNull;

public class SteamSolarBoiler extends SteamBoilerMachine {

    public SteamSolarBoiler(IMachineBlockEntity holder, boolean isHighPressure, Object... args) {
        super(holder, isHighPressure, args);
    }

    @Override
    public @NotNull Direction getFrontFacing() {
        return Direction.UP;
    }

    @Override
    protected long getBaseSteamOutput() {
        return isHighPressure ? ConfigHolder.INSTANCE.machines.smallBoilers.hpSolarBoilerBaseOutput :
                ConfigHolder.INSTANCE.machines.smallBoilers.solarBoilerBaseOutput;
    }

    @Override
    protected void updateSteamSubscription() {
        if (temperatureSubs == null) {
            temperatureSubs = subscribeServerTick(temperatureSubs, this::updateCurrentTemperature);
        }
    }

    @Override
    protected void updateCurrentTemperature() {
        if (GTUtil.canSeeSunClearly(getLevel(), getPos())) {
            recipeLogic.setStatus(RecipeLogic.Status.WORKING);
        } else {
            recipeLogic.setStatus(RecipeLogic.Status.IDLE);
        }
        super.updateCurrentTemperature();
    }

    @Override
    protected int getCooldownInterval() {
        return isHighPressure ? 50 : 45;
    }

    @Override
    protected int getCoolDownRate() {
        return 3;
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var modularUI = super.createUI(entityPlayer);
        var bar = ProgressTextureBar.of(GuiTextures.PROGRESS_BAR_SOLAR_STEAM.get(isHighPressure),
                FillDirection.LEFT_TO_RIGHT, 20, 20);
        bar.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(114).top(44));
        bar.bindDataSource(SupplierDataSource.of(
                () -> GTUtil.canSeeSunClearly(getLevel(), getPos()) ? 1.0f : 0.0f));
        modularUI.ui.rootElement.getChildren().get(0).addChild(bar);
        return modularUI;
    }

    @Override
    protected void randomDisplayTick(RandomSource random, float x, float y, float z) {}
}
