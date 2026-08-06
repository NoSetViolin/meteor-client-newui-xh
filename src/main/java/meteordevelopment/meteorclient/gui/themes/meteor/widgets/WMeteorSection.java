/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;

import static meteordevelopment.meteorclient.gui.themes.meteor.ModernWidgetStyle.isModuleDetails;

public class WMeteorSection extends WSection {
    public WMeteorSection(String title, boolean expanded, WWidget headerWidget) {
        super(title, expanded, headerWidget);
    }

    @Override
    protected WHeader createHeader() {
        return new WMeteorHeader(title);
    }

    protected class WMeteorHeader extends WHeader {
        private WTriangle triangle;

        public WMeteorHeader(String title) {
            super(title);
        }

        @Override
        public void init() {
            if (isModuleDetails()) {
                add(theme.label(title, false).color(((MeteorGuiTheme) theme).accentColor.get())).padLeft(2);
                add(theme.horizontalSeparator()).expandX().centerY();
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
            triangle.rotation = (1 - animProgress) * -90;
        }
    }

    protected static class WHeaderTriangle extends WTriangle implements MeteorWidget {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.rotatedQuad(x, y, width, height, rotation, GuiRenderer.TRIANGLE, theme().textColor.get());
        }
    }
}
