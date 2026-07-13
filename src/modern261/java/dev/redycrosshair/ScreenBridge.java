package dev.redycrosshair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenBridge {
    private ScreenBridge() {
    }

    public static void set(Minecraft minecraft, Screen screen) {
        minecraft.setScreen(screen);
    }
}
