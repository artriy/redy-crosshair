package dev.redycrosshair;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
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
        graphics.flush();

        boolean enabled = state.enabled();
        boolean indicatorActive = enabled && state.indicatorActive();
        boolean tintBase = enabled && state.tintBase();
        boolean customColorActive = indicatorActive && state.customColorActive();
        boolean disableBlending = enabled && state.disableBlendingActive();
        int spriteX = boxX + (BOX_SIZE - SPRITE_SIZE) / 2;
        int spriteY = boxY + (BOX_SIZE - SPRITE_SIZE) / 2;

        RenderSystem.enableBlend();
        if (disableBlending) {
            RenderSystem.defaultBlendFunc();
        } else {
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            );
        }

        if (tintBase) {
            setShaderColor(state.rgb());
        }
        graphics.blitSprite(CROSSHAIR, spriteX, spriteY, SPRITE_SIZE, SPRITE_SIZE);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (indicatorActive) {
            if (customColorActive) {
                setShaderColor(state.rgb());
            }
            graphics.blitSprite(INDICATOR, spriteX, spriteY, SPRITE_SIZE, SPRITE_SIZE);
            graphics.flush();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        RenderSystem.defaultBlendFunc();
    }

    private static void setShaderColor(int rgb) {
        RenderSystem.setShaderColor(
            ((rgb >> 16) & 0xFF) / 255.0F,
            ((rgb >> 8) & 0xFF) / 255.0F,
            (rgb & 0xFF) / 255.0F,
            1.0F
        );
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
