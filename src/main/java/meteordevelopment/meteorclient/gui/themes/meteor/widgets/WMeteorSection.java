/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.themes.meteor.NeverloseUi;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static meteordevelopment.meteorclient.gui.themes.meteor.ModernWidgetStyle.isModuleDetails;

public class WMeteorSection extends WSection {
    private static final Color DARK_SURFACE = new Color(18, 20, 24, 244);
    private static final Color LIGHT_SURFACE = new Color(250, 251, 254, 248);
    private static final Color DARK_OUTLINE = new Color(126, 140, 166, 42);
    private static final Color LIGHT_OUTLINE = new Color(71, 79, 96, 34);

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

        double titleSpace = theme.scale(20);
        NeverloseUi.group(renderer, theme, x, y + titleSpace, width, Math.max(0, height - titleSpace));
    }

    protected class WMeteorHeader extends WHeader {
        private WTriangle triangle;

        public WMeteorHeader(String title) {
            super(title);
        }

        @Override
        public void init() {
            if (isModuleDetails()) {
                add(theme.label(title.toUpperCase(), false).color(NeverloseUi.muted(theme))).padLeft(10);
                if (headerWidget != null) add(headerWidget).right().padRight(12);
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
