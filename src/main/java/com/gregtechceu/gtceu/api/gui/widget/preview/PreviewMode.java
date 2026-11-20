package com.gregtechceu.gtceu.api.gui.widget.preview;

public enum PreviewMode {

    FORMED("FRM", "gtceu.multiblock.preview.mode.formed"),
    UNFORMED("UNF", "gtceu.multiblock.preview.mode.unformed"),
    ACTIVE("ACT", "gtceu.multiblock.preview.mode.active");

    public final String label;
    public final String tooltipKey;

    PreviewMode(String label, String tooltipKey) {
        this.label = label;
        this.tooltipKey = tooltipKey;
    }

    public PreviewMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public PreviewMode previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }
}
