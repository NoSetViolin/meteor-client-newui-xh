/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static meteordevelopment.meteorclient.gui.themes.meteor.ModernWidgetStyle.isModuleDetails;

public class WMeteorSection extends WSection {
    private static final Color DARK_SURFACE = new Color(18, 20, 24, 244);
    private static final Color LIGHT_SURFACE = new Color(250, 251, 254, 248);

    public WMeteorSection(String title, boolean expanded, WWidget headerWidget) {
        super(title, expanded, headerWidget);
    }

    @Override
    protected WHeader createHeader() {
        return new WMeteorHeader(title);
    }

    @Override
    public <T extends WWidget> Cell<T> add(T widget) {
        Cell<T> cell = super.add(widget);
        if (isModuleDetails()) cell.padHorizontal(10).padBottom(7);
        return cell;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!isModuleDetails()) return;

        MeteorGuiTheme meteorTheme = (MeteorGuiTheme) theme;
        Color surface = meteorTheme.modernLightMode.get() ? LIGHT_SURFACE : DARK_SURFACE;
        renderer.roundedQuad(x, y, width, height, theme.scale(12), surface);
    }

    protected class WMeteorHeader extends WHeader {
        private WTriangle triangle;

        public WMeteorHeader(String title) {
            super(title);
        }

        @Override
        public void init() {
            if (isModuleDetails()) {
                add(theme.label(title, true).color(((MeteorGuiTheme) theme).accentColor.get()))
                    .padLeft(12).padTop(10).padBottom(8);
                add(theme.horizontalSeparator()).expandX().centerY().padRight(12);
                if (headerWidget != null) add(headerWidget).padRight(12);
                return;
            } else {
                add(theme.horizontalSeparator(title)).expandX();
            }

            if (headerWidget != null) add(headerWidget);

            triangle = new WHeaderTriangle();
            triangle.theme = theme;
            triangle.action = this::onClick;

            add(triangle);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (triangle != null) triangle.rotation = (1 - animProgress) * -90;
        }

        @Override
        protected void onClick() {
            if (!isModuleDetails()) super.onClick();
        }
    }

    protected static class WHeaderTriangle extends WTriangle implements MeteorWidget {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.rotatedQuad(x, y, width, height, rotation, GuiRenderer.TRIANGLE, theme().textColor.get());
        }
    }
}
