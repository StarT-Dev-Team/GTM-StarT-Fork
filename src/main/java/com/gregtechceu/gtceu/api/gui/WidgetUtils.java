package com.gregtechceu.gtceu.api.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class WidgetUtils {

    public static List<UIElement> getWidgetsById(UIElement group, String regex) {
        var pattern = Pattern.compile(regex);
        return group.selfAndAllChildren().filter(element -> pattern.matcher(element.getId()).matches()).toList();
    }

    @Nullable
    public static UIElement getFirstWidgetById(UIElement group, String regex) {
        var pattern = Pattern.compile(regex);
        return group.selfAndAllChildren().filter(element -> pattern.matcher(element.getId()).matches())
                .findFirst().orElse(null);
    }

    public static void widgetByIdForEach(UIElement group, String regex, Consumer<UIElement> consumer) {
        getWidgetsById(group, regex).forEach(consumer);
    }

    public static <T> void widgetByIdForEach(UIElement group, String regex, Class<T> clazz, Consumer<T> consumer) {
        for (var element : getWidgetsById(group, regex)) {
            if (clazz.isInstance(element)) {
                consumer.accept(clazz.cast(element));
            }
        }
    }

    public static int widgetIdIndex(UIElement element) {
        var id = element.getId();
        if (id == null || id.isEmpty()) return -1;
        var split = id.split("_");
        if (split.length == 0) return -1;
        var end = split[split.length - 1];
        try {
            return Integer.parseInt(end);
        } catch (Exception e) {
            return -1;
        }
    }

    public static int getInventoryHeight(boolean includeHotbar) {
        return 64 + (includeHotbar ? 22 : 0);
    }
}
