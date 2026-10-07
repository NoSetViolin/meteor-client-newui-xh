/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import com.mojang.blaze3d.platform.MacosUtil;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.themes.meteor.NeverloseUi;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorView;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.*;

/** The module browser, laid out on the original Neverlose 830 x 580 design grid. */
public class ModulesScreen extends TabScreen {
    private static String lastNavigationTitle = "Combat";
    private WNeverlosePanel panel;

    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        if (theme instanceof MeteorGuiTheme meteorTheme) meteorTheme.applyModernPalette();
        panel = add(new WNeverlosePanel()).widget();
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent value) {
        if (locked) return false;
        boolean control = MacosUtil.IS_MACOS ? value.modifiers() == GLFW_MOD_SUPER : value.modifiers() == GLFW_MOD_CONTROL;
        if (control && value.key() == GLFW_KEY_F && panel != null) {
            panel.focusSearch();
            return true;
        }
        return super.keyPressed(value);
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Modules.get());
    }

    private final class WNeverlosePanel extends WContainer {
        private final List<NavigationItem> navigation = new ArrayList<>();
        private final List<WNavigationTab> tabs = new ArrayList<>();
        private NavigationItem selected;
        private NavigationItem selectedBeforeSearch;
        private WTextBox search;
        private WToolbarButton save;
        private WModeButton mode;
        private WView moduleView;
        private boolean searchActive;
        private double savedX, savedY, dragOffsetX, dragOffsetY;
        private boolean positionInitialized, dragging;

        @Override
        public void init() {
            buildNavigation();
            save = add(new WToolbarButton("Save", Systems::save)).widget();
            search = add(theme.textBox("", "Search modules")).widget();
            search.action = this::onSearchChanged;
            mode = add(new WModeButton()).widget();

            for (NavigationItem item : navigation) tabs.add(add(new WNavigationTab(item)).widget());

            moduleView = add(new WModuleView()).widget();
            moduleView.scrollOnlyWhenMouseOver = true;
            moduleView.hasScrollBar = true;
            moduleView.spacing = 0;
            moduleView.maxHeight = s(420);
            refreshModules();
            ModulesScreen.this.taskAfterRender = this::refreshModules;
        }

        private void buildNavigation() {
            String[] definitions = { "Combat", "Player", "Movement", "Render", "World", "Misc" };

            for (String definition : definitions) {
                for (Category category : Modules.loopCategories()) {
                    if (category.name.equalsIgnoreCase(definition)
                        && Modules.get().getGroup(category).stream().anyMatch(this::isVisible)) {
                        navigation.add(new NavigationItem(category.name, category, false));
                        break;
                    }
                }
            }

            navigation.add(new NavigationItem("All Modules", null, false));
            navigation.add(new NavigationItem("Favorites", null, true));
            selected = findNavigation(lastNavigationTitle);
            if (selected == null) selected = navigation.getFirst();
            lastNavigationTitle = selected.title;
        }

        private boolean isVisible(Module module) {
            return !Config.get().hiddenModules.get().contains(module);
        }

        private NavigationItem findNavigation(String title) {
            for (NavigationItem item : navigation) if (item.title.equalsIgnoreCase(title)) return item;
            return null;
        }

        private boolean isAllModules(NavigationItem item) {
            return item.category == null && !item.favorite;
        }

        private void select(NavigationItem item) {
            if (searchActive && !isAllModules(item)) {
                search.set("");
                searchActive = false;
                selectedBeforeSearch = null;
            }
            if (selected == item) return;
            selected = item;
            lastNavigationTitle = item.title;
            refreshModules();
        }

        private void onSearchChanged() {
            boolean hasFilter = !search.get().trim().isEmpty();
            if (hasFilter && !searchActive) {
                searchActive = true;
                selectedBeforeSearch = selected;
                NavigationItem all = findNavigation("All Modules");
                if (all != null) selected = all;
            } else if (!hasFilter && searchActive) {
                searchActive = false;
                if (selectedBeforeSearch != null) selected = selectedBeforeSearch;
                selectedBeforeSearch = null;
            }
            refreshModules();
        }

        private List<Module> filteredModules() {
            String filter = search == null ? "" : search.get().trim().toLowerCase(Locale.ROOT);
            boolean globalSearch = !filter.isEmpty();
            List<Module> modules = new ArrayList<>();

            for (Module module : Modules.get().getAll()) {
                if (!isVisible(module)) continue;
                if (!globalSearch && selected.favorite && !module.favorite) continue;
                if (!globalSearch && selected.category != null && !selected.category.equals(module.category)) continue;
                if (!filter.isEmpty() && !module.title.toLowerCase(Locale.ROOT).contains(filter)
                    && !module.description.toLowerCase(Locale.ROOT).contains(filter)) continue;
                modules.add(module);
            }
            modules.sort(Comparator.comparing(module -> module.title, String.CASE_INSENSITIVE_ORDER));
            return modules;
        }

        private void refreshModules() {
            if (moduleView == null) return;
            moduleView.clear();
            moduleView.add(new WGroupGrid(filteredModules())).expandX();
            invalidate();
        }

        private void focusSearch() {
            search.setFocused(true);
            search.setCursorMax();
        }

        private double s(double value) {
            return NeverloseUi.s(theme, value);
        }

        @Override
        protected void onCalculateSize() {
            width = s(NeverloseUi.DESIGN_WIDTH);
            height = s(NeverloseUi.DESIGN_HEIGHT);
        }

        @Override
        protected void onCalculateWidgetPositions() {
            if (!positionInitialized) {
                savedX = getWindowWidth() / 2.0 - width / 2.0;
                savedY = getWindowHeight() / 2.0 - height / 2.0;
                positionInitialized = true;
            }

            savedX = Mth.clamp(savedX, 0, Math.max(0, getWindowWidth() - width));
            savedY = Mth.clamp(savedY, 0, Math.max(0, getWindowHeight() - height));
            x = savedX;
            y = savedY;

            save.x = x + s(656);
            save.y = y + s(29);
            save.width = s(72);
            save.height = s(34);
            search.x = x + s(434);
            search.y = y + s(29);
            search.width = s(208);
            search.height = s(34);
            mode.x = x + s(740);
            mode.y = y + s(29);
            mode.width = s(66);
            mode.height = s(34);

            double tabY = y + s(114);
            int libraryIndex = 0;
            for (int i = 0; i < tabs.size(); i++) {
                WNavigationTab tab = tabs.get(i);
                if (tab.item.category == null) tabY = y + s(422 + libraryIndex++ * 42);
                tab.x = x + s(12);
                tab.y = tabY;
                tab.width = s(168);
                tab.height = s(36);
                tabY += s(42);
            }

            moduleView.x = x + s(216);
            moduleView.y = y + s(112);
            moduleView.width = s(594);
            moduleView.maxHeight = s(420);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            NeverloseUi.window(renderer, theme, x, y, width, height);
            renderer.roundedLeftQuad(x, y, s(192), height, s(NeverloseUi.WINDOW_RADIUS), NeverloseUi.sidebarColor(theme));
            renderer.quad(x + s(192), y, s(1), height, NeverloseUi.separator(theme));
            renderer.quad(x + s(216), y + s(94), width - s(240), s(1), NeverloseUi.separator(theme));
            renderer.quad(x + s(20), y + s(518), s(152), s(1), NeverloseUi.separator(theme));

            NeverloseUi.brand(renderer, theme, x, y);
            renderer.textScaled("CATEGORIES", x + s(24), y + s(96), NeverloseUi.muted(theme), 0.68);
            renderer.textScaled("LIBRARY", x + s(24), y + s(401), NeverloseUi.muted(theme), 0.68);

            NeverloseUi.profile(renderer, theme, x, y, "Local profile");

            renderer.textScaled(NeverloseUi.fit(theme, selected.title, s(206), 1.5), x + s(216), y + s(24), NeverloseUi.text(theme), 1.5);
            List<Module> visible = filteredModules();
            long active = visible.stream().filter(Module::isActive).count();
            renderer.textScaled(visible.size() + " modules  /  " + active + " enabled", x + s(216), y + s(58), NeverloseUi.muted(theme), 0.78);
            renderer.textScaled("Left click to toggle", x + s(216), y + s(550), NeverloseUi.muted(theme), 0.72);
            String hint = "Right click to configure";
            renderer.textScaled(hint, x + s(806) - theme.textWidth(hint) * 0.72, y + s(550), NeverloseUi.muted(theme), 0.72);
        }

        @Override
        public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
            if (click.button() == GLFW_MOUSE_BUTTON_LEFT && click.x() >= x && click.x() <= x + width
                && click.y() >= y && click.y() <= y + s(94)) {
                dragging = true;
                dragOffsetX = click.x() - x;
                dragOffsetY = click.y() - y;
                return true;
            }
            return false;
        }

        @Override
        public boolean onMouseReleased(MouseButtonEvent click) {
            if (dragging && click.button() == GLFW_MOUSE_BUTTON_LEFT) {
                dragging = false;
                return true;
            }
            return false;
        }

        @Override
        public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
            if (!dragging) return;
            double nextX = Mth.clamp(mouseX - dragOffsetX, 0, Math.max(0, getWindowWidth() - width));
            double nextY = Mth.clamp(mouseY - dragOffsetY, 0, Math.max(0, getWindowHeight() - height));
            move(nextX - x, nextY - y);
            savedX = x;
            savedY = y;
        }

        private final class WToolbarButton extends WPressable {
            private final String label;
            private final Runnable action;

            private WToolbarButton(String label, Runnable action) {
                this.label = label;
                this.action = action;
            }

            @Override
            protected void onCalculateSize() {
                width = s(100);
                height = s(30);
            }

            @Override
            protected void onPressed(int button) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) action.run();
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                Color fill = pressed ? NeverloseUi.buttonActive(theme)
                    : mouseOver ? NeverloseUi.buttonHover(theme) : NeverloseUi.button(theme);
                NeverloseUi.control(renderer, theme, x, y, width, height, fill, mouseOver);
                renderer.text(label, x + width / 2 - theme.textWidth(label) / 2,
                    y + height / 2 - theme.textHeight() / 2,
                    mouseOver ? NeverloseUi.text(theme) : NeverloseUi.muted(theme), false);
            }
        }

        private final class WNavigationTab extends WPressable {
            private final NavigationItem item;
            private double selection;
            private double hover;

            private WNavigationTab(NavigationItem item) {
                this.item = item;
                selection = selected == item ? 1 : 0;
            }

            @Override
            protected void onCalculateSize() {
                width = s(170);
                height = s(31);
            }

            @Override
            protected void onPressed(int button) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) select(item);
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                boolean active = selected == item;
                selection = NeverloseUi.animate(theme, selection, active ? 1 : 0, delta);
                hover = NeverloseUi.animate(theme, hover, mouseOver ? 1 : 0, delta);
                if (selection > 0 || hover > 0) {
                    Color fill = NeverloseUi.mix(NeverloseUi.buttonHover(theme), NeverloseUi.ACCENT_SOFT, selection);
                    fill.a = (int) (fill.a * Math.max(selection, hover));
                    renderer.roundedGradient(x, y, width, height, s(8), fill,
                        new Color(fill.r, fill.g, fill.b, fill.a / 2));
                }
                NeverloseUi.icon(renderer, item.title, x + s(12), y + height / 2 - s(8), s(16),
                    NeverloseUi.mix(NeverloseUi.muted(theme), NeverloseUi.ACCENT, selection));
                renderer.text(item.title, x + s(38), y + height / 2 - theme.textHeight() / 2,
                    NeverloseUi.mix(NeverloseUi.muted(theme), NeverloseUi.text(theme), selection), false);
            }
        }

        private final class WModeButton extends WPressable {
            @Override
            protected void onCalculateSize() {
                width = s(80);
                height = s(30);
                tooltip = "Switch light and dark mode";
            }

            @Override
            protected void onPressed(int button) {
                if (button != GLFW_MOUSE_BUTTON_LEFT || !(theme instanceof MeteorGuiTheme meteorTheme)) return;
                meteorTheme.modernLightMode.set(!meteorTheme.modernLightMode.get());
                meteorTheme.applyModernPalette();
                invalidate();
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                Color fill = pressed ? NeverloseUi.buttonActive(theme)
                    : mouseOver ? NeverloseUi.buttonHover(theme) : NeverloseUi.button(theme);
                NeverloseUi.control(renderer, theme, x, y, width, height, fill, mouseOver);
                String label = NeverloseUi.isLight(theme) ? "Light" : "Dark";
                renderer.text(label, x + width / 2 - theme.textWidth(label) / 2,
                    y + height / 2 - theme.textHeight() / 2, NeverloseUi.text(theme), false);
            }
        }

        private final class WGroupGrid extends WContainer {
            private final List<Module> modules;
            private WModuleGroup left, right;

            private WGroupGrid(List<Module> modules) {
                this.modules = modules;
            }

            @Override
            public void init() {
                int split = (modules.size() + 1) / 2;
                left = add(new WModuleGroup("MODULES", new ArrayList<>(modules.subList(0, split)))).widget();
                right = add(new WModuleGroup("MORE MODULES", new ArrayList<>(modules.subList(split, modules.size())))).widget();
            }

            @Override
            protected void onCalculateSize() {
                width = s(582);
                height = Math.max(left.height, right.height);
            }

            @Override
            protected void onCalculateWidgetPositions() {
                left.x = x;
                left.y = y;
                left.width = s(282);
                right.x = x + s(300);
                right.y = y;
                right.width = s(282);
            }
        }

        private final class WModuleView extends WMeteorView {
            @Override
            public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                boolean result = super.render(renderer, mouseX, mouseY, delta);
                if (canScroll) {
                    Color base = NeverloseUi.windowColor(theme);
                    Color clear = new Color(base.r, base.g, base.b, 0);
                    Color opaque = new Color(base.r, base.g, base.b, 255);
                    renderer.roundedGradient(x, y + height - s(24), width - s(8), s(24), 0, clear, opaque);
                    if (handleY() > y + s(1)) renderer.roundedGradient(x, y, width - s(8), s(16), 0, opaque, clear);
                }
                return result;
            }
        }

        private final class WModuleGroup extends WContainer {
            private final String title;
            private final List<Module> modules;

            private WModuleGroup(String title, List<Module> modules) {
                this.title = title;
                this.modules = modules;
            }

            @Override
            public void init() {
                for (Module module : modules) add(new WModuleRow(module));
            }

            @Override
            protected void onCalculateSize() {
                width = s(282);
                height = s(Math.max(110, 48 + modules.size() * 52));
            }

            @Override
            protected void onCalculateWidgetPositions() {
                double rowY = y + s(38);
                for (Cell<?> cell : cells) {
                    cell.widget().x = x + s(16);
                    cell.widget().y = rowY;
                    cell.widget().width = width - s(32);
                    cell.widget().height = s(52);
                    rowY += s(52);
                }
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                NeverloseUi.group(renderer, theme, x, y, width, height);
                renderer.textScaled(title, x + s(16), y + s(15), NeverloseUi.muted(theme), 0.7);
                if (modules.isEmpty()) renderer.textScaled("No modules found", x + s(16), y + s(54), NeverloseUi.muted(theme), 0.85);
            }
        }

        private final class WModuleRow extends WPressable {
            private final Module module;
            private double anim;
            private double hover;

            private WModuleRow(Module module) {
                this.module = module;
                tooltip = module.description;
                anim = module.isActive() ? 1 : 0;
            }

            @Override
            protected void onCalculateSize() {
                width = s(266);
                height = s(31);
            }

            @Override
            protected void onPressed(int button) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
                else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.gui.setScreen(theme.moduleScreen(module));
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                anim = NeverloseUi.animate(theme, anim, module.isActive() ? 1 : 0, delta);
                hover = NeverloseUi.animate(theme, hover, mouseOver ? 1 : 0, delta);
                if (hover > 0) {
                    Color fill = NeverloseUi.buttonHover(theme);
                    renderer.roundedQuad(x - s(8), y + s(2), width + s(16), height - s(4), s(7),
                        new Color(fill.r, fill.g, fill.b, (int) (fill.a * hover)));
                }
                renderer.text(NeverloseUi.fit(theme, module.title, width - s(46), 1), x, y + s(9), NeverloseUi.text(theme), false);
                renderer.textScaled(NeverloseUi.fit(theme, module.description, width - s(46), 0.75),
                    x, y + s(30), NeverloseUi.muted(theme), 0.75);

                double switchWidth = s(30), switchHeight = s(17);
                double switchX = x + width - switchWidth;
                double switchY = y + height / 2 - switchHeight / 2;
                if (NeverloseUi.effects(theme) && anim > 0) renderer.roundedShadow(
                    switchX, switchY, switchWidth, switchHeight, switchHeight / 2, s(3),
                    new Color(NeverloseUi.ACCENT.r, NeverloseUi.ACCENT.g, NeverloseUi.ACCENT.b, (int) (36 * anim)));
                renderer.roundedQuad(switchX, switchY, switchWidth, switchHeight, switchHeight / 2,
                    NeverloseUi.frameActive(theme));
                renderer.roundedQuad(switchX, switchY, switchWidth, switchHeight, switchHeight / 2,
                    new Color(NeverloseUi.ACCENT.r, NeverloseUi.ACCENT.g, NeverloseUi.ACCENT.b, (int) (255 * anim)));
                renderer.roundedQuad(switchX + s(1) + s(13) * anim, switchY + s(1.5), s(14), s(14), s(7),
                    NeverloseUi.isLight(theme) ? Color.WHITE
                        : module.isActive() ? NeverloseUi.text(theme) : NeverloseUi.muted(theme));
            }
        }
    }

    private record NavigationItem(String title, Category category, boolean favorite) {}
}
