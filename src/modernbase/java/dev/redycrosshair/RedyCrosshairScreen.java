package dev.redycrosshair;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class RedyCrosshairScreen extends Screen {
    private final Screen parent;
    private int color;
    private boolean disableBlending;
    private boolean disableBlendingOnlyWhileRedy;
    private boolean updatingFields;
    private ColorWheelWidget wheel;
    private EditBox hexField;
    private EditBox redField;
    private EditBox greenField;
    private EditBox blueField;
    private Button disableBlendingOnlyWhileRedyButton;

    public RedyCrosshairScreen(Screen parent) {
        super(Component.translatable("redycrosshair.title"));
        this.parent = parent;
        this.color = RedyCrosshairConfig.rgb();
        this.disableBlending = RedyCrosshairConfig.disableBlending();
        this.disableBlendingOnlyWhileRedy = RedyCrosshairConfig.disableBlendingOnlyWhileRedy();
    }

    @Override
    protected void init() {
        int wheelX = this.width / 2 - 137;
        int wheelY = 38;
        int rightX = this.width / 2 + 10;

        this.wheel = addRenderableWidget(new ColorWheelWidget(wheelX, wheelY, 108, this.color, this::setColorFromWheel));
        this.hexField = addRenderableWidget(new EditBox(this.font, rightX, 49, 120, 20, Component.translatable("redycrosshair.hex")));
        this.redField = addRenderableWidget(new EditBox(this.font, rightX, 88, 36, 20, Component.translatable("redycrosshair.red")));
        this.greenField = addRenderableWidget(new EditBox(this.font, rightX + 42, 88, 36, 20, Component.translatable("redycrosshair.green")));
        this.blueField = addRenderableWidget(new EditBox(this.font, rightX + 84, 88, 36, 20, Component.translatable("redycrosshair.blue")));

        this.hexField.setMaxLength(7);
        this.redField.setMaxLength(3);
        this.greenField.setMaxLength(3);
        this.blueField.setMaxLength(3);
        this.hexField.setResponder(this::onHexChanged);
        this.redField.setResponder(ignored -> onRgbChanged());
        this.greenField.setResponder(ignored -> onRgbChanged());
        this.blueField.setResponder(ignored -> onRgbChanged());
        syncAllFields();

        addRenderableWidget(Button.builder(toggleLabel("redycrosshair.disable_blending", this.disableBlending), this::toggleDisableBlending)
            .bounds(this.width / 2 - 145, this.height - 68, 290, 18).build());
        this.disableBlendingOnlyWhileRedyButton = addRenderableWidget(Button.builder(
            toggleLabel("redycrosshair.disable_blending_only_while_redy", this.disableBlendingOnlyWhileRedy),
            this::toggleDisableBlendingOnlyWhileRedy
        ).bounds(this.width / 2 - 135, this.height - 47, 280, 18).build());
        this.disableBlendingOnlyWhileRedyButton.active = this.disableBlending;

        int buttonY = this.height - 24;
        addRenderableWidget(Button.builder(Component.translatable("redycrosshair.reset"), button -> setColor(RedyCrosshairConfig.DEFAULT_RGB))
            .bounds(this.width / 2 - 154, buttonY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("redycrosshair.save"), button -> saveAndClose())
            .bounds(this.width / 2 - 50, buttonY, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
            .bounds(this.width / 2 + 54, buttonY, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int rightX = this.width / 2 + 10;
        graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        graphics.text(this.font, Component.translatable("redycrosshair.hex"), rightX, 36, 0xFFA0A0A0);
        graphics.text(this.font, Component.translatable("redycrosshair.red"), rightX, 75, 0xFFFF8080);
        graphics.text(this.font, Component.translatable("redycrosshair.green"), rightX + 42, 75, 0xFF80FF80);
        graphics.text(this.font, Component.translatable("redycrosshair.blue"), rightX + 84, 75, 0xFF8080FF);
        graphics.text(this.font, Component.translatable("redycrosshair.preview"), rightX, 115, 0xFFA0A0A0);
        graphics.fill(rightX, 128, rightX + 120, 158, 0xFF000000 | this.color);
        graphics.outline(rightX - 1, 127, 122, 32, 0xFFFFFFFF);
        graphics.centeredText(this.font, String.format(Locale.ROOT, "#%06X", this.color), rightX + 60, 139, contrastColor(this.color));
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
        RedyCrosshairConfig.setRgb(this.color);
        RedyCrosshairConfig.setDisableBlending(this.disableBlending);
        RedyCrosshairConfig.setDisableBlendingOnlyWhileRedy(this.disableBlendingOnlyWhileRedy);
        RedyCrosshairConfig.save();
        onClose();
    }

    private void toggleDisableBlending(Button button) {
        this.disableBlending = !this.disableBlending;
        button.setMessage(toggleLabel("redycrosshair.disable_blending", this.disableBlending));
        this.disableBlendingOnlyWhileRedyButton.active = this.disableBlending;
    }

    private void toggleDisableBlendingOnlyWhileRedy(Button button) {
        this.disableBlendingOnlyWhileRedy = !this.disableBlendingOnlyWhileRedy;
        button.setMessage(toggleLabel("redycrosshair.disable_blending_only_while_redy", this.disableBlendingOnlyWhileRedy));
    }

    private static Component toggleLabel(String key, boolean value) {
        return Component.translatable(key, Component.translatable(value ? "options.on" : "options.off"));
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
