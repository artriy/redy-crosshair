package dev.redycrosshair.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.redycrosshair.RedyCrosshairConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
abstract class GuiMixin {
    @Shadow @Final private Minecraft minecraft;

    @Redirect(
        method = "renderCrosshair",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",
            ordinal = 0
        ),
        require = 0
    )
    private void redycrosshair$tintAttackableTarget(
        GuiGraphics graphics,
        ResourceLocation sprite,
        int x,
        int y,
        int width,
        int height
    ) {
        boolean redyActive = redycrosshair$canHitTarget();
        boolean disableBlending = RedyCrosshairConfig.shouldDisableBlending(redyActive);
        if (redyActive || disableBlending) {
            graphics.flush();
        }
        if (disableBlending) {
            RenderSystem.defaultBlendFunc();
        }
        if (redyActive) {
            int color = RedyCrosshairConfig.rgb();
            RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
        }
        graphics.blitSprite(sprite, x, y, width, height);
        if (redyActive || disableBlending) {
            graphics.flush();
        }
        if (redyActive) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        if (disableBlending) {
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            );
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
