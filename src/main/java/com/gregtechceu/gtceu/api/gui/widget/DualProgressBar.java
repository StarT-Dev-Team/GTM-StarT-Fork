package com.gregtechceu.gtceu.api.gui.widget;

import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;

import org.jetbrains.annotations.Nullable;

public class DualProgressBar extends BindableUIElement<Float> {

    private final ProgressBar first;
    private final ProgressBar second;
    private final double splitPoint;
    private float progress;

    public DualProgressBar(ProgressBar first, ProgressBar second, double splitPoint) {
        this.first = first;
        this.second = second;
        this.splitPoint = splitPoint;
        addChildren(first, second);
    }

    public ProgressBar getFirst() {
        return first;
    }

    public ProgressBar getSecond() {
        return second;
    }

    @Override
    public Float getValue() {
        return progress;
    }

    @Override
    public BindableUIElement<Float> setValue(@Nullable Float value, boolean notify) {
        progress = value == null ? 0 : value;
        first.setProgress(progress >= splitPoint ? 1f : (float) (progress / splitPoint));
        second.setProgress(progress >= splitPoint ? (float) ((progress - splitPoint) / (1 - splitPoint)) : 0f);
        if (notify) notifyListeners();
        return this;
    }
}
