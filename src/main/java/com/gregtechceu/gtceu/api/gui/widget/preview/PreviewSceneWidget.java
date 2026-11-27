package com.gregtechceu.gtceu.api.gui.widget.preview;

import com.gregtechceu.gtceu.api.gui.widget.PatternSceneWidget;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PreviewSceneWidget extends PatternSceneWidget {

    public interface Host {

        boolean hasSelection();

        boolean isOverVariantUi(double mouseX, double mouseY);

        void clearBlockSelection();

        void stepLayer(int delta);
    }

    private final Host host;

    public PreviewSceneWidget(Level world, Host host) {
        super(world);
        this.host = host;
    }

    @Override
    protected void onMouseUp(UIEvent event) {
        super.onMouseUp(event);
        if ((event.button == 0 || event.button == 1) && host.hasSelection() && isHover() &&
                !host.isOverVariantUi(event.x, event.y)) {
            var hit = renderer == null ? null : renderer.getLastTraceResult();
            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                host.clearBlockSelection();
            }
        }
    }

    @Override
    protected void onMouseWheel(UIEvent event) {
        if (event.target == this && (UIElement.isShiftDown() || UIElement.isCtrlDown())) {
            host.stepLayer(event.deltaY > 0 ? 1 : -1);
            event.stopPropagation();
            return;
        }
        super.onMouseWheel(event);
    }
}
