package dev.redycrosshair;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class RedyCrosshairIconButton {
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("redycrosshair", "icon/options");

    private RedyCrosshairIconButton() {
    }

    public static SpriteIconButton create(int x, int y, Button.OnPress onPress) {
        Component label = Component.translatable("redycrosshair.options");
        SpriteIconButton button = SpriteIconButton.builder(label, onPress, true)
            .width(20)
            .sprite(ICON, 15, 15)
            .build();
        button.setX(x);
        button.setY(y);
        button.setTooltip(Tooltip.create(label));
        return button;
    }
}
