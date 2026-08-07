package dev.redycrosshair.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.redycrosshair.RedyCrosshairConfig;
import dev.redycrosshair.RedyCrosshairTargeting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Hud.class)
abstract class GuiMixin {
    private static final Identifier REDYCROSSHAIR_INDICATOR = Identifier.fromNamespaceAndPath(
        "redycrosshair",
        "crosshair_indicator"
    );

    @Shadow @Final private Minecraft minecraft;

    @Redirect(
        method = "extractCrosshair",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
            ordinal = 0
        ),
        require = 0
    )
    private void redycrosshair$tintAttackableTarget(
        GuiGraphicsExtractor graphics,
        RenderPipeline pipeline,
        Identifier sprite,
        int x,
        int y,
        int width,
        int height
    ) {
        Entity target = RedyCrosshairTargeting.attackableTarget(this.minecraft);
        boolean redyActive = target != null;
        boolean criticalHit = redyActive
            && RedyCrosshairConfig.critColorEnabled()
            && !this.minecraft.player.getMainHandItem().has(DataComponents.PIERCING_WEAPON)
            && !this.minecraft.player.cannotAttackWithItem(this.minecraft.player.getMainHandItem(), 0)
            && RedyCrosshairTargeting.canCriticalHit(this.minecraft, target);
        int targetColor = RedyCrosshairConfig.targetArgb(criticalHit);
        RenderPipeline selectedPipeline = RedyCrosshairConfig.shouldDisableBlending(redyActive)
            ? RenderPipelines.GUI_TEXTURED
            : pipeline;
        if (redyActive && RedyCrosshairConfig.useIndicatorStyle()) {
            boolean customColor = RedyCrosshairConfig.indicatorCustomColor() || criticalHit;
            if (customColor && !RedyCrosshairConfig.indicatorCornersOnly()) {
                graphics.blitSprite(selectedPipeline, sprite, x, y, width, height, targetColor);
            } else {
                graphics.blitSprite(selectedPipeline, sprite, x, y, width, height);
            }
            int indicatorX = x + (width - 15) / 2;
            int indicatorY = y + (height - 15) / 2;
            if (customColor) {
                graphics.blitSprite(selectedPipeline, REDYCROSSHAIR_INDICATOR, indicatorX, indicatorY, 15, 15, targetColor);
            } else {
                graphics.blitSprite(selectedPipeline, REDYCROSSHAIR_INDICATOR, indicatorX, indicatorY, 15, 15);
            }
        } else if (redyActive) {
            graphics.blitSprite(selectedPipeline, sprite, x, y, width, height, targetColor);
        } else {
            graphics.blitSprite(selectedPipeline, sprite, x, y, width, height);
        }
    }

}
