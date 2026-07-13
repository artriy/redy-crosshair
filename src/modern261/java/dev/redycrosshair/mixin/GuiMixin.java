package dev.redycrosshair.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.redycrosshair.RedyCrosshairConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
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
        boolean redyActive = redycrosshair$canHitTarget();
        RenderPipeline selectedPipeline = RedyCrosshairConfig.shouldDisableBlending(redyActive)
            ? RenderPipelines.GUI_TEXTURED
            : pipeline;
        if (redyActive && RedyCrosshairConfig.useIndicatorStyle()) {
            graphics.blitSprite(selectedPipeline, sprite, x, y, width, height);
            int indicatorX = x + (width - 15) / 2;
            int indicatorY = y + (height - 15) / 2;
            if (RedyCrosshairConfig.indicatorCustomColor()) {
                graphics.blitSprite(selectedPipeline, REDYCROSSHAIR_INDICATOR, indicatorX, indicatorY, 15, 15, RedyCrosshairConfig.argb());
            } else {
                graphics.blitSprite(selectedPipeline, REDYCROSSHAIR_INDICATOR, indicatorX, indicatorY, 15, 15);
            }
        } else if (redyActive) {
            graphics.blitSprite(selectedPipeline, sprite, x, y, width, height, RedyCrosshairConfig.argb());
        } else {
            graphics.blitSprite(selectedPipeline, sprite, x, y, width, height);
        }
    }

    private boolean redycrosshair$canHitTarget() {
        Entity target = this.minecraft.crosshairPickEntity;
        return target != null
            && this.minecraft.player != null
            && !this.minecraft.player.isSpectator()
            && target.isAlive()
            && target.isAttackable()
            && !target.skipAttackInteraction(this.minecraft.player);
    }
}
