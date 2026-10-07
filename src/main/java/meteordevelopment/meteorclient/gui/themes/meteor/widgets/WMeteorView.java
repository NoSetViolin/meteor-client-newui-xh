/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;

public class WMeteorView extends WView implements MeteorWidget {
    @Override
    protected double handleWidth() {
        return meteordevelopment.meteorclient.gui.themes.meteor.ModernWidgetStyle.isModernScreen()
            ? theme.scale(3) : super.handleWidth();
    }
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (canScroll && hasScrollBar) {
            renderer.roundedQuad(handleX(), handleY(), handleWidth(), handleHeight(), handleWidth() / 2,
                theme().scrollbarColor.get(focused, handleMouseOver));
        }
    }
}
