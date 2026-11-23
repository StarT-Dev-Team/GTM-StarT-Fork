package com.gregtechceu.gtceu.common.item.tool.behavior;

import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.item.tool.aoe.AoESymmetrical;
import com.gregtechceu.gtceu.api.item.tool.behavior.IToolUIBehavior;

import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static com.gregtechceu.gtceu.api.item.tool.ToolHelper.getBehaviorsTag;
import static com.gregtechceu.gtceu.api.item.tool.ToolHelper.getMaxAoEDefinition;

public class AOEConfigUIBehavior implements IToolUIBehavior {

    public static final AOEConfigUIBehavior INSTANCE = new AOEConfigUIBehavior();

    @Override
    public boolean openUI(@NotNull Player player, @NotNull InteractionHand hand) {
        return player.isShiftKeyDown() && !getMaxAoEDefinition(player.getItemInHand(hand)).isZero();
    }

    @Override
    public ModularUI createUI(Player player, HeldItemUIMenuType.HeldItemUIHolder holder) {
        Supplier<ItemStack> held = () -> player.getItemInHand(holder.hand);
        var defaultDefinition = getMaxAoEDefinition(holder.itemStack);
        var root = UITemplate.panel();
        root.layout(layout -> layout.width(120).paddingAll(6).gapAll(4));

        var columns = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW)
                .justifyContent(AlignContent.SPACE_AROUND));
        columns.addChild(column("item.gtceu.tool.aoe.columns",
                () -> Integer.toString(1 + 2 * AoESymmetrical.getColumn(getBehaviorsTag(held.get()),
                        defaultDefinition)),
                () -> AoESymmetrical.increaseColumn(getBehaviorsTag(held.get()), defaultDefinition),
                () -> AoESymmetrical.decreaseColumn(getBehaviorsTag(held.get()), defaultDefinition)));
        columns.addChild(column("item.gtceu.tool.aoe.rows",
                () -> Integer.toString(1 + 2 * AoESymmetrical.getRow(getBehaviorsTag(held.get()),
                        defaultDefinition)),
                () -> AoESymmetrical.increaseRow(getBehaviorsTag(held.get()), defaultDefinition),
                () -> AoESymmetrical.decreaseRow(getBehaviorsTag(held.get()), defaultDefinition)));
        columns.addChild(column("item.gtceu.tool.aoe.layers",
                () -> Integer.toString(1 + AoESymmetrical.getLayer(getBehaviorsTag(held.get()),
                        defaultDefinition)),
                () -> AoESymmetrical.increaseLayer(getBehaviorsTag(held.get()), defaultDefinition),
                () -> AoESymmetrical.decreaseLayer(getBehaviorsTag(held.get()), defaultDefinition)));
        root.addChild(columns);
        return UITemplate.modularUI(root, player);
    }

    private static UIElement column(String titleKey, Supplier<String> value, Runnable increase, Runnable decrease) {
        var column = new UIElement().layout(layout -> layout.alignItems(AlignItems.CENTER).gapAll(2));
        column.addChild(new Label().setText(Component.translatable(titleKey)));
        column.addChild(button("+", increase));
        column.addChild(button("-", decrease));
        var label = new Label();
        label.bindDataSource(SupplierDataSource.of(() -> (Component) Component.literal(value.get())));
        column.addChild(label);
        return column;
    }

    private static Button button(String text, Runnable action) {
        var button = new Button().setText(text, false);
        button.layout(layout -> layout.width(20).height(20));
        button.setOnServerClick(event -> action.run());
        return button;
    }
}
