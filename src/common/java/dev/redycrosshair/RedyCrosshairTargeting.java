package dev.redycrosshair;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class RedyCrosshairTargeting {
    private RedyCrosshairTargeting() {
    }

    public static Entity attackableTarget(Minecraft minecraft) {
        if (!RedyCrosshairConfig.enabled() || minecraft.player == null || minecraft.player.isSpectator()) {
            return null;
        }
        Entity target = minecraft.crosshairPickEntity;
        return target != null
            && target.isAlive()
            && target.isAttackable()
            && !target.skipAttackInteraction(minecraft.player)
            ? target
            : null;
    }

    public static boolean canCriticalHit(Minecraft minecraft, Entity target) {
        if (minecraft.player == null || !(target instanceof LivingEntity)) {
            return false;
        }
        var player = minecraft.player;
        return player.getAttackStrengthScale(0.5F) > 0.9F
            && player.fallDistance > 0.0
            && !player.onGround()
            && !player.onClimbable()
            && !player.isInWater()
            && !player.hasEffect(MobEffects.BLINDNESS)
            && !player.isPassenger()
            && !player.isSprinting();
    }
}
