/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.NeverloseUi;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.function.Supplier;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class ModuleScreen extends WidgetScreen {
    private static final Color ACCENT = NeverloseUi.ACCENT;
    private static final Color DARK_MICA = new Color(0, 0, 0, 255);
    private static final Color DARK_HEADER = new Color(0, 0, 0, 118);
    private static final Color DARK_TEXT = new Color(242, 244, 252);
    private static final Color DARK_MUTED = new Color(149, 158, 183);
    private static final Color DARK_BUTTON = new Color(18, 18, 18, 255);
    private static final Color LIGHT_MICA = new Color(238, 241, 248, 255);
    private static final Color LIGHT_HEADER = new Color(250, 251, 254, 112);
    private static final Color LIGHT_TEXT = new Color(28, 31, 42);
    private static final Color LIGHT_MUTED = new Color(92, 99, 119);
    private static final Color LIGHT_BUTTON = new Color(255, 255, 255, 255);
    private static final Color DARK_ACCENT_SOFT = new Color(0, 245, 255, 42);
    private static final Color LIGHT_ACCENT_SOFT = new Color(0, 155, 170, 30);

    private final Module module;
    private WModulePanel panel;
    private WKeybind keybind;

    public ModuleScreen(GuiTheme theme, Module module) {
        super(theme, module.title);
        this.module = module;
    }

    @Override
    public void initWidgets() {
        if (theme instanceof MeteorGuiTheme meteorTheme) meteorTheme.applyModernPalette();
        panel = add(new WModulePanel()).widget();
    }

    @Override
    public void tick() {
        super.tick();
        if (panel != null && panel.visibilityChanged()) panel.refreshContent();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !Modules.get().isBinding();
    }

    @EventHandler
    private void onModuleBindChanged(ModuleBindChangedEvent event) {
        if (keybind != null) keybind.reset();
    }

    @Override
    public boolean toClipboard() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", module.name);
        CompoundTag settingsTag = module.settings.toTag();
        if (!settingsTag.isEmpty()) tag.put("settings", settingsTag);
        return NbtUtils.toClipboard(tag);
    }

    @Override
    public boolean fromClipboard() {
        CompoundTag tag = NbtUtils.fromClipboard();
        if (tag == null || !tag.getStringOr("name", "").equals(module.name)) return false;

        Optional<CompoundTag> settings = tag.getCompound("settings");
        if (settings.isPresent()) module.settings.fromTag(settings.get());
        else module.settings.reset();

        if (parent instanceof WidgetScreen widgetScreen) widgetScreen.reload();
        if (panel != null) panel.refreshContent();
        return true;
    }

    private Color text() { return NeverloseUi.text(theme); }
    private Color muted() { return NeverloseUi.muted(theme); }
    private Color buttonColor() { return NeverloseUi.button(theme); }
    private Color accentSoft() { return NeverloseUi.ACCENT_SOFT; }

    public double materialSettingLabelWidth() {
        return 112;
    }

    private final class WModulePanel extends WContainer {
        private WView view;
        private WBackButton back;
        private WIconButton favorite;
        private WIconButton mode;
        private WActiveButton active;
        private boolean twoColumns;

        private double plannedWidth;
        private double plannedHeight;
        private double savedX;
        private double savedY;
        private boolean positionInitialized;
        private boolean dragging;
        private double dragOffsetX;
        private double dragOffsetY;

        @Override
        public void init() {
            back = add(new WBackButton()).widget();
            favorite = add(new WIconButton(() -> module.favorite ? "★" : "☆", "Toggle favorite",
                () -> module.favorite = !module.favorite, () -> module.favorite)).widget();
            mode = add(new WIconButton(() -> NeverloseUi.isLight(theme) ? "☀" : "☾", "Switch light and dark mode",
                this::toggleMode, () -> false)).widget();
            active = add(new WActiveButton()).widget();
            view = add(theme.view()).widget();
            view.scrollOnlyWhenMouseOver = true;
            view.hasScrollBar = true;
            view.spacing = 0;
            refreshContent();
        }

        private void toggleMode() {
            if (!(theme instanceof MeteorGuiTheme meteorTheme)) return;
            meteorTheme.modernLightMode.set(!meteorTheme.modernLightMode.get());
            meteorTheme.applyModernPalette();
            refreshContent();
        }

        private void refreshContent() {
            if (view == null) return;
            view.clear();

            WWidget custom = module.getWidget(theme);
            twoColumns = true;

            WHorizontalList columns = theme.horizontalList();
            columns.spacing = 15;
            WVerticalList left = theme.verticalList();
            WVerticalList right = theme.verticalList();
            left.spacing = 14;
            right.spacing = 14;
            columns.add(left).expandX();
            columns.add(right).expandX();

            int leftWeight = 0;
            int rightWeight = 0;
            for (SettingGroup group : module.settings.groups) {
                int visibleSettings = 0;
                for (Setting<?> setting : group) {
                    if (setting.isVisible()) visibleSettings++;
                }
                if (visibleSettings == 0) continue;

                Settings singleGroup = new Settings();
                singleGroup.groups.add(group);
                int groupWeight = visibleSettings + 1;

                boolean addLeft = leftWeight <= rightWeight;
                WVerticalList target = addLeft ? left : right;
                target.add(theme.settings(singleGroup)).expandX();
                if (addLeft) leftWeight += groupWeight;
                else rightWeight += groupWeight;
            }

            boolean moduleOnLeft = leftWeight <= rightWeight;
            WVerticalList moduleColumn = moduleOnLeft ? left : right;
            WSection moduleSection = moduleColumn.add(theme.section("Module", true)).expandX().widget();
            WTable moduleTable = moduleSection.add(theme.table()).expandX().widget();
            moduleTable.horizontalSpacing = 10;
            moduleTable.verticalSpacing = 6;

            moduleTable.add(theme.label("Bind", false)).minWidth(materialSettingLabelWidth()).centerY().padVertical(6);
            keybind = moduleTable.add(theme.keybind(module.keybind)).expandX().widget();
            keybind.actionOnSet = () -> Modules.get().setModuleToBind(module);
            WButton reset = moduleTable.add(theme.button(GuiRenderer.RESET)).centerY().widget();
            reset.action = keybind::resetBind;
            moduleTable.row();

            moduleTable.add(theme.label("Toggle on release", false)).minWidth(materialSettingLabelWidth()).centerY().padVertical(6);
            WCheckbox releaseToggle = moduleTable.add(theme.checkbox(module.toggleOnBindRelease)).expandCellX().right().widget();
            releaseToggle.action = () -> module.toggleOnBindRelease = releaseToggle.checked;
            moduleTable.add(theme.label("")).minWidth(30);
            moduleTable.row();

            moduleTable.add(theme.label("Chat feedback", false)).minWidth(materialSettingLabelWidth()).centerY().padVertical(6);
            WCheckbox feedbackToggle = moduleTable.add(theme.checkbox(module.chatFeedback)).expandCellX().right().widget();
            feedbackToggle.action = () -> module.chatFeedback = feedbackToggle.checked;
            moduleTable.add(theme.label("")).minWidth(30);
            moduleTable.row();

            moduleTable.add(theme.label("Configuration", false)).minWidth(materialSettingLabelWidth()).centerY().padVertical(6);
            moduleTable.row();
            WHorizontalList sharing = moduleTable.add(theme.horizontalList()).widget();
            sharing.spacing = 8;
            WButton copy = sharing.add(theme.button("Copy")).widget();
            copy.action = ModuleScreen.this::toClipboard;
            WButton paste = sharing.add(theme.button("Paste")).widget();
            paste.action = ModuleScreen.this::fromClipboard;
            if (moduleOnLeft) leftWeight += 6;
            else rightWeight += 6;

            if (custom != null) {
                WVerticalList customColumn = leftWeight <= rightWeight ? left : right;
                customColumn.add(custom).expandX();
            }

            columns.calculateSize();
            if (columns.width > theme.scale(594)) {
                // Keep controls at their measured width rather than overlapping reset buttons.
                twoColumns = false;
                WVerticalList stacked = theme.verticalList();
                stacked.spacing = 14;
                stacked.add(left).expandX();
                stacked.add(right).expandX();
                view.add(stacked).expandX();
            } else {
                view.add(columns).expandX();
            }
            invalidate();
        }

        private int moduleContentWeight(boolean hasCustomWidget) {
            int weight = 6; // Permanent Module card: header plus five rows.

            for (SettingGroup group : module.settings.groups) {
                int visibleSettings = 0;
                for (Setting<?> setting : group) {
                    if (setting.isVisible()) visibleSettings++;
                }

                if (visibleSettings > 0) weight += visibleSettings + 1;
            }

            if (hasCustomWidget) weight += 4;
            return weight;
        }

        private int singleColumnCapacity() {
            double availableHeight = getWindowHeight() * 0.52;
            return Math.max(9, (int) Math.floor(availableHeight / theme.scale(42)));
        }

        private boolean visibilityChanged() {
            for (SettingGroup group : module.settings.groups) {
                for (Setting<?> setting : group) {
                    if (setting.isVisible() != setting.lastWasVisible) return true;
                }
            }
            return false;
        }

        @Override
        public void calculateSize() {
            plannedWidth = theme.scale(NeverloseUi.DESIGN_WIDTH);
            plannedHeight = theme.scale(NeverloseUi.DESIGN_HEIGHT);
            if (view != null) view.maxHeight = theme.scale(420);
            super.calculateSize();
            width = plannedWidth;
            height = plannedHeight;
        }

        @Override
        protected void onCalculateSize() {
            width = plannedWidth;
            height = plannedHeight;
        }

        @Override
        protected void onCalculateWidgetPositions() {
            if (!positionInitialized) {
                savedX = getWindowWidth() / 2.0 - width / 2.0 - width * 0.10;
                savedY = getWindowHeight() / 2.0 - height / 2.0 - height * 0.10;
                positionInitialized = true;
            }
            savedX = Mth.clamp(savedX, 0, Math.max(0, getWindowWidth() - width));
            savedY = Mth.clamp(savedY, 0, Math.max(0, getWindowHeight() - height));
            x = savedX;
            y = savedY;

            back.x = x + theme.scale(12);
            back.y = y + theme.scale(114);
            back.width = theme.scale(168);
            back.height = theme.scale(36);
            active.x = x + theme.scale(604);
            active.y = y + theme.scale(29);
            favorite.x = x + theme.scale(765);
            favorite.y = y + theme.scale(29);
            mode.x = x + theme.scale(717);
            mode.y = y + theme.scale(29);
            view.x = x + theme.scale(216);
            view.y = y + theme.scale(112);
            view.width = theme.scale(594);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            NeverloseUi.window(renderer, theme, x, y, width, height);
            renderer.roundedLeftQuad(x, y, theme.scale(192), height, theme.scale(NeverloseUi.WINDOW_RADIUS), NeverloseUi.sidebarColor(theme));
            renderer.quad(x + theme.scale(192), y, theme.scale(1), height, NeverloseUi.separator(theme));
            renderer.quad(x + theme.scale(216), y + theme.scale(94), width - theme.scale(240),
                theme.scale(1), NeverloseUi.separator(theme));
            renderer.quad(x + theme.scale(20), y + theme.scale(518), theme.scale(152), theme.scale(1), NeverloseUi.separator(theme));
            NeverloseUi.brand(renderer, theme, x, y);
            NeverloseUi.profile(renderer, theme, x, y, "Editing settings");

            renderer.textScaled(NeverloseUi.fit(theme, module.title, theme.scale(370), 1.5),
                x + theme.scale(216), y + theme.scale(24), text(), 1.5);
            renderer.textScaled(NeverloseUi.fit(theme, module.description, theme.scale(488), 0.78),
                x + theme.scale(216), y + theme.scale(58), muted(), 0.78);
        }

        private void renderRounded(GuiRenderer renderer, double rx, double ry, double rw, double rh, double radius, Color color) {
            renderer.roundedQuad(rx, ry, rw, rh, radius, color);
        }

        private void renderRoundedTop(GuiRenderer renderer, double rx, double ry, double rw, double rh, double radius, Color color) {
            renderer.roundedTopQuad(rx, ry, rw, rh, radius, color);
        }

        @Override
        public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
            if (click.button() == GLFW_MOUSE_BUTTON_LEFT && click.x() >= x && click.x() <= x + width
                && click.y() >= y && click.y() <= y + theme.scale(70)) {
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

    }

    private final class WBackButton extends WPressable {
        @Override
        protected void onCalculateSize() {
            width = theme.scale(170);
            height = theme.scale(31);
        }

        @Override
        protected void onPressed(int button) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) mc.gui.setScreen(ModuleScreen.this.parent);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (mouseOver) renderer.roundedQuad(x, y, width, height, theme.scale(5), NeverloseUi.buttonHover(theme));
            double cy = y + height / 2;
            renderer.line(x + theme.scale(18), cy - theme.scale(5), x + theme.scale(13), cy, ACCENT);
            renderer.line(x + theme.scale(13), cy, x + theme.scale(18), cy + theme.scale(5), ACCENT);
            String label = "Back to modules";
            renderer.text(label, x + theme.scale(35), y + height / 2 - theme.textHeight() / 2,
                mouseOver ? text() : muted(), false);
        }
    }

    private final class WIconButton extends WPressable {
        private final Supplier<String> label;
        private final String hint;
        private final Runnable action;
        private final Supplier<Boolean> highlighted;

        private WIconButton(Supplier<String> label, String hint, Runnable action, Supplier<Boolean> highlighted) {
            this.label = label;
            this.hint = hint;
            this.action = action;
            this.highlighted = highlighted;
            tooltip = hint;
        }

        @Override
        protected void onCalculateSize() {
            width = theme.scale(40);
            height = theme.scale(30);
        }

        @Override
        protected void onPressed(int button) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) action.run();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderControl(renderer, this, highlighted.get() ? accentSoft() : buttonColor());
            if (hint.contains("favorite")) NeverloseUi.icon(renderer, "Favorites", x + width / 2 - theme.scale(8),
                y + height / 2 - theme.scale(8), theme.scale(16), highlighted.get() ? ACCENT : muted());
            else renderer.textScaled(NeverloseUi.isLight(theme) ? "Light" : "Dark", x + width / 2
                - theme.textWidth(NeverloseUi.isLight(theme) ? "Light" : "Dark") * 0.75 / 2,
                y + height / 2 - theme.textHeight() * 0.75 / 2, text(), 0.75);
        }
    }

    private final class WActiveButton extends WPressable {
        @Override
        protected void onCalculateSize() {
            width = theme.scale(100);
            height = theme.scale(30);
        }

        @Override
        protected void onPressed(int button) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderControl(renderer, this, module.isActive() ? ACCENT : buttonColor());
            String label = module.isActive() ? "Enabled" : "Disabled";
            renderer.text(label, x + width / 2 - theme.textWidth(label) / 2,
                y + height / 2 - theme.textHeight() / 2, module.isActive() ? Color.WHITE : text(), false);
        }
    }

    private void renderControl(GuiRenderer renderer, WWidget widget, Color color) {
        NeverloseUi.control(renderer, theme, widget.x, widget.y, widget.width, widget.height, color, widget.mouseOver);
    }
}
