package dev.redycrosshair.mixin;

import dev.redycrosshair.RedyCrosshairConfig;
import dev.redycrosshair.RedyCrosshairTargeting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(Gui.class)
abstract class GuiMixin {
    private static final ResourceLocation REDYCROSSHAIR_INDICATOR = ResourceLocation.fromNamespaceAndPath(
        "redycrosshair",
        "crosshair_indicator"
    );

    @Shadow @Final private Minecraft minecraft;

    @Redirect(
        method = "renderCrosshair",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V",
            ordinal = 0
        ),
        require = 0
    )
    private void redycrosshair$tintAttackableTarget(
        GuiGraphics graphics,
        Function<ResourceLocation, RenderType> renderType,
        ResourceLocation sprite,
        int x,
        int y,
        int width,
        int height
    ) {
        Entity target = RedyCrosshairTargeting.attackableTarget(this.minecraft);
        boolean redyActive = target != null;
        boolean criticalHit = redyActive
            && RedyCrosshairConfig.critColorEnabled()
            && RedyCrosshairTargeting.canCriticalHit(this.minecraft, target);
        int targetColor = RedyCrosshairConfig.targetArgb(criticalHit);
        Function<ResourceLocation, RenderType> selectedRenderType = RedyCrosshairConfig.shouldDisableBlending(redyActive)
            ? RenderType::guiTextured
            : renderType;
        if (redyActive && RedyCrosshairConfig.useIndicatorStyle()) {
            boolean customColor = RedyCrosshairConfig.indicatorCustomColor() || criticalHit;
            if (customColor && !RedyCrosshairConfig.indicatorCornersOnly()) {
                graphics.blitSprite(selectedRenderType, sprite, x, y, width, height, targetColor);
            } else {
                graphics.blitSprite(selectedRenderType, sprite, x, y, width, height);
            }
            int indicatorX = x + (width - 15) / 2;
            int indicatorY = y + (height - 15) / 2;
            if (customColor) {
                graphics.blitSprite(selectedRenderType, REDYCROSSHAIR_INDICATOR, indicatorX, indicatorY, 15, 15, targetColor);
            } else {
                graphics.blitSprite(selectedRenderType, REDYCROSSHAIR_INDICATOR, indicatorX, indicatorY, 15, 15);
            }
        } else if (redyActive) {
            graphics.blitSprite(selectedRenderType, sprite, x, y, width, height, targetColor);
        } else {
            graphics.blitSprite(selectedRenderType, sprite, x, y, width, height);
        }
    }

}
