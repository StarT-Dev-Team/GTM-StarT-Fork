package com.gregtechceu.gtceu.api.capability.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.content.SerializerInteger;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;

import dev.vfyjxf.taffy.style.TaffyPosition;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;

public class CWURecipeCapability extends RecipeCapability<Integer> {

    public final static CWURecipeCapability CAP = new CWURecipeCapability();

    protected CWURecipeCapability() {
        super("cwu", 0xFFEEEE00, false, 3, SerializerInteger.INSTANCE);
    }

    @Override
    public Integer copyInner(Integer content) {
        return content;
    }

    @Override
    public Integer copyWithModifier(Integer content, ContentModifier modifier) {
        return modifier.apply(content);
    }

    @Override
    public void addXEIInfo(UIElement group, int xOffset, GTRecipe recipe, List<Content> contents, boolean perTick,
                           boolean isInput, MutableInt yOffset, int minTier, int currentTier) {
        if (perTick) {
            int cwu = contents.stream().map(Content::getContent).mapToInt(CWURecipeCapability.CAP::of).sum();
            group.addChild(infoLabel(3 - xOffset, yOffset.addAndGet(10),
                    LocalizationUtils.format("gtceu.recipe.computation_per_tick", FormattingUtil.formatNumbers(cwu))));
        }
        if (recipe.data.getBoolean("duration_is_total_cwu")) {
            double totalCWU = recipe.duration;

            if (currentTier > minTier) {
                totalCWU = totalCWU - (totalCWU * (0.025 * (currentTier - minTier)));
            }

            group.addChild(infoLabel(3 - xOffset, yOffset.addAndGet(10),
                    LocalizationUtils.format("gtceu.recipe.total_computation",
                            FormattingUtil.formatNumbers(totalCWU))));
        }
    }

    private static Label infoLabel(int x, int y, String text) {
        var label = new Label();
        label.setText(text, false);
        label.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).left(x).top(y));
        return label;
    }
}
