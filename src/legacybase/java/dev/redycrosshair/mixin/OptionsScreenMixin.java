package dev.redycrosshair.mixin;

import dev.redycrosshair.RedyCrosshairScreen;
import dev.redycrosshair.RedyCrosshairIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void redycrosshair$addOptionsButton(CallbackInfo callbackInfo) {
        addRenderableWidget(RedyCrosshairIconButton.create(
            this.width - 26,
            6,
            button -> this.minecraft.setScreen(new RedyCrosshairScreen(this))
        ));
    }
}
