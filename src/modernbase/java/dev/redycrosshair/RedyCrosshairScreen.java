package dev.redycrosshair;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RedyCrosshairScreen extends Screen {
    private final Screen parent;
    private final List<AbstractWidget> optionWidgets = new ArrayList<>();
    private boolean redyCrosshairEnabled;
    private int color;
    private boolean useIndicatorStyle;
    private boolean indicatorCustomColor;
    private boolean indicatorCornersOnly;
    private boolean disableBlending;
    private boolean disableBlendingOnlyWhileRedy;
    private boolean updatingFields;
    private ColorWheelWidget wheel;
    private EditBox hexField;
    private EditBox redField;
    private EditBox greenField;
    private EditBox blueField;
    private Button indicatorCustomColorButton;
    private Button indicatorCornersOnlyButton;
    private Button disableBlendingOnlyWhileRedyButton;

    public RedyCrosshairScreen(Screen parent) {
        super(Component.translatable("redycrosshair.title"));
        this.parent = parent;
        this.redyCrosshairEnabled = RedyCrosshairConfig.enabled();
        this.color = RedyCrosshairConfig.rgb();
        this.useIndicatorStyle = RedyCrosshairConfig.useIndicatorStyle();
        this.indicatorCustomColor = RedyCrosshairConfig.indicatorCustomColor();
        this.indicatorCornersOnly = RedyCrosshairConfig.indicatorCornersOnly();
        this.disableBlending = RedyCrosshairConfig.disableBlending();
        this.disableBlendingOnlyWhileRedy = RedyCrosshairConfig.disableBlendingOnlyWhileRedy();
    }

    @Override
    protected void init() {
        this.optionWidgets.clear();
        int wheelX = this.width / 2 - 137;
        int wheelY = 38;
        int rightX = this.width / 2 + 10;

        this.wheel = addOptionWidget(new ColorWheelWidget(wheelX, wheelY, 108, this.color, this::setColorFromWheel));
        this.hexField = addOptionWidget(new EditBox(this.font, rightX, 49, 120, 20, Component.translatable("redycrosshair.hex")));
        this.redField = addOptionWidget(new EditBox(this.font, rightX, 88, 36, 20, Component.translatable("redycrosshair.red")));
        this.greenField = addOptionWidget(new EditBox(this.font, rightX + 42, 88, 36, 20, Component.translatable("redycrosshair.green")));
        this.blueField = addOptionWidget(new EditBox(this.font, rightX + 84, 88, 36, 20, Component.translatable("redycrosshair.blue")));

        this.hexField.setMaxLength(7);
        this.redField.setMaxLength(3);
        this.greenField.setMaxLength(3);
        this.blueField.setMaxLength(3);
        this.hexField.setResponder(this::onHexChanged);
        this.redField.setResponder(ignored -> onRgbChanged());
        this.greenField.setResponder(ignored -> onRgbChanged());
        this.blueField.setResponder(ignored -> onRgbChanged());
        syncAllFields();

        int indicatorStyleY = this.height - 89;
        int optionRowOneY = this.height - 68;
        int optionRowTwoY = this.height - 47;
        addOptionWidget(Button.builder(toggleLabel("redycrosshair.indicator_style", this.useIndicatorStyle), this::toggleIndicatorStyle)
            .bounds(this.width / 2 - 145, indicatorStyleY, 142, 18).build());
        this.indicatorCustomColorButton = addOptionWidget(Button.builder(
            toggleLabel("redycrosshair.indicator_custom_color", this.indicatorCustomColor),
            this::toggleIndicatorCustomColor
        ).bounds(this.width / 2 - 135, optionRowOneY, 132, 18).build());
        this.indicatorCornersOnlyButton = addOptionWidget(Button.builder(
            toggleLabel("redycrosshair.indicator_corners_only", this.indicatorCornersOnly),
            this::toggleIndicatorCornersOnly
        ).bounds(this.width / 2 - 125, optionRowTwoY, 122, 18).build());
        addRenderableWidget(Button.builder(toggleLabel("redycrosshair.enabled", this.redyCrosshairEnabled), this::toggleEnabled)
            .bounds(this.width / 2 + 3, indicatorStyleY, 142, 18).build());
        addOptionWidget(Button.builder(toggleLabel("redycrosshair.disable_blending", this.disableBlending), this::toggleDisableBlending)
            .bounds(this.width / 2 + 3, optionRowOneY, 142, 18).build());
        this.disableBlendingOnlyWhileRedyButton = addOptionWidget(Button.builder(
            toggleLabel("redycrosshair.disable_blending_only_while_redy", this.disableBlendingOnlyWhileRedy),
            this::toggleDisableBlendingOnlyWhileRedy
        ).bounds(this.width / 2 + 13, optionRowTwoY, 132, 18).build());

        int buttonY = this.height - 24;
        addOptionWidget(Button.builder(Component.translatable("redycrosshair.reset"), button -> setColor(RedyCrosshairConfig.DEFAULT_RGB))
            .bounds(this.width / 2 - 154, buttonY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("redycrosshair.save"), button -> saveAndClose())
            .bounds(this.width / 2 - 50, buttonY, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
            .bounds(this.width / 2 + 54, buttonY, 100, 20).build());
        updateOptionStates();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int rightX = this.width / 2 + 10;
        graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        if (this.redyCrosshairEnabled) {
            graphics.text(this.font, Component.translatable("redycrosshair.hex"), rightX, 36, 0xFFA0A0A0);
            graphics.text(this.font, Component.translatable("redycrosshair.red"), rightX, 75, 0xFFFF8080);
            graphics.text(this.font, Component.translatable("redycrosshair.green"), rightX + 42, 75, 0xFF80FF80);
            graphics.text(this.font, Component.translatable("redycrosshair.blue"), rightX + 84, 75, 0xFF8080FF);
            graphics.text(this.font, Component.translatable("redycrosshair.preview"), rightX, 115, 0xFFA0A0A0);
            graphics.fill(rightX, 128, rightX + 120, 158, 0xFF000000 | this.color);
            graphics.outline(rightX - 1, 127, 122, 32, 0xFFFFFFFF);
            graphics.centeredText(this.font, String.format(Locale.ROOT, "#%06X", this.color), rightX + 60, 139, contrastColor(this.color));
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void onHexChanged(String text) {
        if (this.updatingFields) {
            return;
        }
        Integer parsed = RedyCrosshairConfig.parseHex(text);
        if (parsed != null) {
            this.color = parsed;
            this.wheel.setRgb(this.color);
            syncRgbFields();
        }
    }

    private void onRgbChanged() {
        if (this.updatingFields) {
            return;
        }
        Integer red = parseChannel(this.redField.getValue());
        Integer green = parseChannel(this.greenField.getValue());
        Integer blue = parseChannel(this.blueField.getValue());
        if (red != null && green != null && blue != null) {
            this.color = (red << 16) | (green << 8) | blue;
            this.wheel.setRgb(this.color);
            syncHexField();
        }
    }

    private void setColorFromWheel(int rgb) {
        this.color = rgb & 0xFFFFFF;
        syncAllFields();
    }

    private void setColor(int rgb) {
        this.color = rgb & 0xFFFFFF;
        this.wheel.setRgb(this.color);
        syncAllFields();
    }

    private void syncAllFields() {
        this.updatingFields = true;
        this.hexField.setValue(String.format(Locale.ROOT, "#%06X", this.color));
        this.redField.setValue(Integer.toString((this.color >> 16) & 0xFF));
        this.greenField.setValue(Integer.toString((this.color >> 8) & 0xFF));
        this.blueField.setValue(Integer.toString(this.color & 0xFF));
        this.updatingFields = false;
    }

    private void syncHexField() {
        this.updatingFields = true;
        this.hexField.setValue(String.format(Locale.ROOT, "#%06X", this.color));
        this.updatingFields = false;
    }

    private void syncRgbFields() {
        this.updatingFields = true;
        this.redField.setValue(Integer.toString((this.color >> 16) & 0xFF));
        this.greenField.setValue(Integer.toString((this.color >> 8) & 0xFF));
        this.blueField.setValue(Integer.toString(this.color & 0xFF));
        this.updatingFields = false;
    }

    private void saveAndClose() {
        RedyCrosshairConfig.setEnabled(this.redyCrosshairEnabled);
        RedyCrosshairConfig.setRgb(this.color);
        RedyCrosshairConfig.setUseIndicatorStyle(this.useIndicatorStyle);
        RedyCrosshairConfig.setIndicatorCustomColor(this.indicatorCustomColor);
        RedyCrosshairConfig.setIndicatorCornersOnly(this.indicatorCornersOnly);
        RedyCrosshairConfig.setDisableBlending(this.disableBlending);
        RedyCrosshairConfig.setDisableBlendingOnlyWhileRedy(this.disableBlendingOnlyWhileRedy);
        RedyCrosshairConfig.save();
        onClose();
    }

    private void toggleEnabled(Button button) {
        this.redyCrosshairEnabled = !this.redyCrosshairEnabled;
        button.setMessage(toggleLabel("redycrosshair.enabled", this.redyCrosshairEnabled));
        updateOptionStates();
    }

    private void toggleIndicatorStyle(Button button) {
        this.useIndicatorStyle = !this.useIndicatorStyle;
        button.setMessage(toggleLabel("redycrosshair.indicator_style", this.useIndicatorStyle));
        updateOptionStates();
    }

    private void toggleIndicatorCustomColor(Button button) {
        this.indicatorCustomColor = !this.indicatorCustomColor;
        button.setMessage(toggleLabel("redycrosshair.indicator_custom_color", this.indicatorCustomColor));
        updateOptionStates();
    }

    private void toggleIndicatorCornersOnly(Button button) {
        this.indicatorCornersOnly = !this.indicatorCornersOnly;
        button.setMessage(toggleLabel("redycrosshair.indicator_corners_only", this.indicatorCornersOnly));
    }

    private void updateOptionStates() {
        for (AbstractWidget widget : this.optionWidgets) {
            widget.visible = this.redyCrosshairEnabled;
        }
        boolean colorActive = this.redyCrosshairEnabled && (!this.useIndicatorStyle || this.indicatorCustomColor);
        this.wheel.active = colorActive;
        this.hexField.active = colorActive;
        this.redField.active = colorActive;
        this.greenField.active = colorActive;
        this.blueField.active = colorActive;
        this.indicatorCustomColorButton.active = this.redyCrosshairEnabled && this.useIndicatorStyle;
        this.indicatorCornersOnlyButton.active = this.redyCrosshairEnabled && this.useIndicatorStyle && this.indicatorCustomColor;
        this.disableBlendingOnlyWhileRedyButton.active = this.redyCrosshairEnabled && this.disableBlending;
    }

    private void toggleDisableBlending(Button button) {
        this.disableBlending = !this.disableBlending;
        button.setMessage(toggleLabel("redycrosshair.disable_blending", this.disableBlending));
        updateOptionStates();
    }

    private void toggleDisableBlendingOnlyWhileRedy(Button button) {
        this.disableBlendingOnlyWhileRedy = !this.disableBlendingOnlyWhileRedy;
        button.setMessage(toggleLabel("redycrosshair.disable_blending_only_while_redy", this.disableBlendingOnlyWhileRedy));
    }

    private static Component toggleLabel(String key, boolean value) {
        return Component.translatable(key, Component.translatable(value ? "options.on" : "options.off"));
    }

    private <T extends AbstractWidget> T addOptionWidget(T widget) {
        this.optionWidgets.add(widget);
        return addRenderableWidget(widget);
    }

    @Override
    public void onClose() {
        ScreenBridge.set(this.minecraft, this.parent);
    }

    private static Integer parseChannel(String text) {
        try {
            int value = Integer.parseInt(text);
            return value >= 0 && value <= 255 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int contrastColor(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return red * 299 + green * 587 + blue * 114 > 140000 ? 0xFF000000 : 0xFFFFFFFF;
    }
}
