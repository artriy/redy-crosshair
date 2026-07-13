package dev.redycrosshair;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

public final class ColorWheelWidget extends AbstractWidget {
    private static final int SAMPLE_SIZE = 2;
    private static final int VALUE_GAP = 7;
    private static final int VALUE_HEIGHT = 12;

    private final int diameter;
    private final IntConsumer onColorChanged;
    private float hue;
    private float saturation;
    private float value;

    public ColorWheelWidget(int x, int y, int diameter, int initialRgb, IntConsumer onColorChanged) {
        super(x, y, diameter, diameter + VALUE_GAP + VALUE_HEIGHT, Component.translatable("redycrosshair.color_wheel"));
        this.diameter = diameter;
        this.onColorChanged = onColorChanged;
        setRgb(initialRgb);
    }

    public void setRgb(int rgb) {
        float[] hsv = ColorMath.rgbToHsv(rgb);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int radius = this.diameter / 2;
        float center = (this.diameter - 1) / 2.0F;
        for (int py = 0; py < this.diameter; py += SAMPLE_SIZE) {
            for (int px = 0; px < this.diameter; px += SAMPLE_SIZE) {
                float dx = px + SAMPLE_SIZE * 0.5F - center;
                float dy = py + SAMPLE_SIZE * 0.5F - center;
                float distance = (float)Math.sqrt(dx * dx + dy * dy);
                if (distance <= radius) {
                    float cellHue = (float)(Math.atan2(dy, dx) / (Math.PI * 2.0));
                    if (cellHue < 0.0F) {
                        cellHue += 1.0F;
                    }
                    float cellSaturation = Math.min(1.0F, distance / radius);
                    int color = 0xFF000000 | ColorMath.hsvToRgb(cellHue, cellSaturation, this.value);
                    graphics.fill(getX() + px, getY() + py, getX() + px + SAMPLE_SIZE, getY() + py + SAMPLE_SIZE, color);
                }
            }
        }

        double angle = this.hue * Math.PI * 2.0;
        float selectionRadius = this.saturation * radius;
        int selectionX = Math.round(getX() + center + (float)Math.cos(angle) * selectionRadius);
        int selectionY = Math.round(getY() + center + (float)Math.sin(angle) * selectionRadius);
        drawOutline(graphics, selectionX - 3, selectionY - 3, 7, 7, 0xFF000000);
        drawOutline(graphics, selectionX - 2, selectionY - 2, 5, 5, 0xFFFFFFFF);

        int valueY = getY() + this.diameter + VALUE_GAP;
        for (int px = 0; px < this.diameter; px += SAMPLE_SIZE) {
            float cellValue = px / (float)(this.diameter - 1);
            int color = 0xFF000000 | ColorMath.hsvToRgb(this.hue, this.saturation, cellValue);
            graphics.fill(getX() + px, valueY, getX() + px + SAMPLE_SIZE, valueY + VALUE_HEIGHT, color);
        }
        int valueX = getX() + Math.round(this.value * (this.diameter - 1));
        graphics.fill(valueX - 1, valueY - 2, valueX + 2, valueY + VALUE_HEIGHT + 2, 0xFFFFFFFF);
        drawOutline(graphics, getX() - 1, valueY - 1, this.diameter + 2, VALUE_HEIGHT + 2, 0xFF606060);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        updateFromMouse(event.x(), event.y());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        updateFromMouse(event.x(), event.y());
    }

    private void updateFromMouse(double mouseX, double mouseY) {
        int valueY = getY() + this.diameter + VALUE_GAP;
        if (mouseY >= valueY && mouseY <= valueY + VALUE_HEIGHT) {
            this.value = clamp((float)((mouseX - getX()) / (this.diameter - 1)));
            notifyChanged();
            return;
        }

        float radius = this.diameter / 2.0F;
        float centerX = getX() + (this.diameter - 1) / 2.0F;
        float centerY = getY() + (this.diameter - 1) / 2.0F;
        float dx = (float)mouseX - centerX;
        float dy = (float)mouseY - centerY;
        float distance = (float)Math.sqrt(dx * dx + dy * dy);
        if (distance <= radius + 2.0F) {
            this.hue = (float)(Math.atan2(dy, dx) / (Math.PI * 2.0));
            if (this.hue < 0.0F) {
                this.hue += 1.0F;
            }
            this.saturation = clamp(distance / radius);
            notifyChanged();
        }
    }

    private void notifyChanged() {
        this.onColorChanged.accept(ColorMath.hsvToRgb(this.hue, this.saturation, this.value));
    }

    private static float clamp(float input) {
        return Math.max(0.0F, Math.min(1.0F, input));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }

    private static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
