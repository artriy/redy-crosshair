package dev.redycrosshair;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

public final class RedyCrosshairPreview {
    private static final int BOX_SIZE = 36;
    private static final int SPRITE_SIZE = 15;
    private static final ResourceLocation CROSSHAIR = ResourceLocation.fromNamespaceAndPath(
        "minecraft",
        "hud/crosshair"
    );
    private static final ResourceLocation INDICATOR = ResourceLocation.fromNamespaceAndPath(
        "redycrosshair",
        "crosshair_indicator"
    );

    private RedyCrosshairPreview() {
    }

    public static void render(GuiGraphics graphics, int boxX, int boxY, RedyCrosshairPreviewState state) {
        drawBox(graphics, boxX, boxY);
        graphics.nextStratum();

        boolean enabled = state.enabled();
        boolean indicatorActive = enabled && state.indicatorActive();
        boolean tintBase = enabled && state.tintBase();
        boolean customColorActive = indicatorActive && state.customColorActive();
        RenderPipeline pipeline = enabled && state.disableBlendingActive()
            ? RenderPipelines.GUI_TEXTURED
            : RenderPipelines.CROSSHAIR;
        int spriteX = boxX + (BOX_SIZE - SPRITE_SIZE) / 2;
        int spriteY = boxY + (BOX_SIZE - SPRITE_SIZE) / 2;

        if (tintBase) {
            graphics.blitSprite(pipeline, CROSSHAIR, spriteX, spriteY, SPRITE_SIZE, SPRITE_SIZE, state.argb());
        } else {
            graphics.blitSprite(pipeline, CROSSHAIR, spriteX, spriteY, SPRITE_SIZE, SPRITE_SIZE);
        }

        if (indicatorActive) {
            if (customColorActive) {
                graphics.blitSprite(pipeline, INDICATOR, spriteX, spriteY, SPRITE_SIZE, SPRITE_SIZE, state.argb());
            } else {
                graphics.blitSprite(pipeline, INDICATOR, spriteX, spriteY, SPRITE_SIZE, SPRITE_SIZE);
            }
        }
    }

    private static void drawBox(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + BOX_SIZE, y + BOX_SIZE, 0xFF303030);
        graphics.fill(x, y, x + BOX_SIZE / 2, y + BOX_SIZE / 2, 0xFF686868);
        graphics.fill(x + BOX_SIZE / 2, y + BOX_SIZE / 2, x + BOX_SIZE, y + BOX_SIZE, 0xFF686868);
        drawOutline(graphics, x, y, BOX_SIZE, BOX_SIZE, 0xFFC0C0C0);
    }

    private static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
