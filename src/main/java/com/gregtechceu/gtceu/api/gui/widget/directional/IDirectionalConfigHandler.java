package com.gregtechceu.gtceu.api.gui.widget.directional;

import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.utils.data.BlockPosFace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

public interface IDirectionalConfigHandler {

    /**
     * Returns the buttons to display inside the side selector. Built on both sides.
     */
    @Nullable
    UIElement getSideSelectorWidget(FancyMachineUIWidget machineUI);

    /**
     * Called on both sides whenever a side is selected in the side selector GUI
     */
    void onSideSelected(BlockPos pos, Direction side);

    /**
     * Determines which side of the screen the UI element should be placed on.
     */
    ScreenSide getScreenSide();

    enum ScreenSide {
        LEFT,
        RIGHT,
    }

    /**
     * Runs on the server when the selected side is clicked in the scene.
     */
    default void handleClick(int button, Direction direction) {}

    @OnlyIn(Dist.CLIENT)
    default void renderOverlay(Scene scene, BlockPosFace blockPosFace) {}

    default void addAdditionalUIElements(UIElement parent) {}
}
