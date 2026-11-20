package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.core.Direction;

public class PreviewState {

    private static final int[] GRADUATION_STEPS = { 0, 3, 5, 10 };

    private static boolean defaultShowGrid = true;
    private static boolean defaultTurntable = false;
    private static int defaultGraduationIndex = 0;
    private static PreviewMode defaultMode = PreviewMode.FORMED;

    public Direction frontFacing;
    public Direction baseHorizontalFacing;
    public Direction upwardsFacing;
    public boolean flipped;

    public int index;
    public int layer = -1;

    public boolean showGrid = defaultShowGrid;
    public boolean turntable = defaultTurntable;
    public PreviewMode mode = defaultMode;
    private int graduationIndex = defaultGraduationIndex;

    public PreviewState(MultiblockMachineDefinition definition) {
        resetOrientation(definition);
    }

    public void resetOrientation(MultiblockMachineDefinition definition) {
        this.frontFacing = definition.getRotationState().defaultDirection;
        this.baseHorizontalFacing = frontFacing.getAxis() == Direction.Axis.Y ? Direction.NORTH : frontFacing;
        this.upwardsFacing = Direction.NORTH;
        this.flipped = false;
    }

    public boolean isDefaultOrientation(MultiblockMachineDefinition definition) {
        return frontFacing == definition.getRotationState().defaultDirection &&
                upwardsFacing == Direction.NORTH &&
                !flipped;
    }

    public int graduationStep() {
        return GRADUATION_STEPS[graduationIndex];
    }

    public void cycleGraduation(boolean reverse) {
        int count = GRADUATION_STEPS.length;
        graduationIndex = (graduationIndex + (reverse ? count - 1 : 1)) % count;
        defaultGraduationIndex = graduationIndex;
    }

    public void toggleGrid() {
        showGrid = !showGrid;
        defaultShowGrid = showGrid;
    }

    public void setTurntable(boolean active) {
        turntable = active;
        defaultTurntable = active;
    }

    public void cycleMode(boolean reverse) {
        mode = reverse ? mode.previous() : mode.next();
        defaultMode = mode;
    }
}
