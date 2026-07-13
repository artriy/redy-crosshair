package dev.redycrosshair.mixin;

import dev.redycrosshair.RedyCrosshairConfig;
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
        boolean redyActive = redycrosshair$canHitTarget();
        Function<ResourceLocation, RenderType> selectedRenderType = RedyCrosshairConfig.shouldDisableBlending(redyActive)
            ? RenderType::guiTextured
            : renderType;
        if (redyActive) {
            graphics.blitSprite(selectedRenderType, sprite, x, y, width, height, RedyCrosshairConfig.argb());
        } else {
            graphics.blitSprite(selectedRenderType, sprite, x, y, width, height);
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
