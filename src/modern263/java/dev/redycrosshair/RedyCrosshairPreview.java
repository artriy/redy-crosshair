package dev.redycrosshair;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class RedyCrosshairPreview {
    private static final int BOX_SIZE = 36;
    private static final int SPRITE_SIZE = 15;
    private static final Identifier CROSSHAIR = Identifier.fromNamespaceAndPath(
        "minecraft",
        "hud/crosshair"
    );
    private static final Identifier INDICATOR = Identifier.fromNamespaceAndPath(
        "redycrosshair",
        "crosshair_indicator"
    );

    private RedyCrosshairPreview() {
    }

    public static void render(
        GuiGraphicsExtractor graphics,
        int boxX,
        int boxY,
        RedyCrosshairPreviewState state
    ) {
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

    private static void drawBox(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + BOX_SIZE, y + BOX_SIZE, 0xFF303030);
        graphics.fill(x, y, x + BOX_SIZE / 2, y + BOX_SIZE / 2, 0xFF686868);
        graphics.fill(x + BOX_SIZE / 2, y + BOX_SIZE / 2, x + BOX_SIZE, y + BOX_SIZE, 0xFF686868);
        graphics.outline(x, y, BOX_SIZE, BOX_SIZE, 0xFFC0C0C0);
    }
}
