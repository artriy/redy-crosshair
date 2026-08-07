package dev.redycrosshair.smoke;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class SmokeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SmokeChecks.Context context = SmokeChecks.verify("net.minecraft.client.gui.Gui", "1.21.1");
        openPreviewAndExit(context);
    }

    private static void openPreviewAndExit(SmokeChecks.Context context) {
        Thread previewSmoke = new Thread(() -> {
            try {
                Minecraft client = Minecraft.getInstance();
                long deadline = System.nanoTime() + 60_000_000_000L;
                while ((client.screen == null
                    || !client.screen.getClass().getName().equals("net.minecraft.client.gui.screens.TitleScreen"))
                    && System.nanoTime() < deadline) {
                    Thread.sleep(50L);
                }
                Screen parent = client.screen;
                if (parent == null) {
                    throw new IllegalStateException("Minecraft never opened its title screen");
                }
                ConfigScreenFactory<?> factory = context.configFactory();
                client.execute(() -> client.setScreen(factory.create(parent)));
                while ((client.screen == null
                    || !client.screen.getClass().getName().equals("dev.redycrosshair.RedyCrosshairScreen"))
                    && System.nanoTime() < deadline) {
                    Thread.sleep(50L);
                }
                Screen preview = client.screen;
                if (preview == null || !preview.getClass().getName().equals("dev.redycrosshair.RedyCrosshairScreen")) {
                    throw new IllegalStateException("Redy Crosshair settings screen never opened");
                }
                Thread.sleep(2_000L);
                if (client.screen != preview) {
                    throw new IllegalStateException("Redy Crosshair settings screen closed while rendering its preview");
                }
                System.out.println(
                    "REDY_CROSSHAIR_SMOKE_OK minecraft=" + context.minecraftVersion()
                        + " outer=" + context.outerVersion()
                        + " impl=" + context.implementationVersion()
                        + " preview=rendered"
                );
                client.execute(client::stop);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                System.exit(1);
            }
        }, "redycrosshair-preview-smoke");
        previewSmoke.setDaemon(true);
        previewSmoke.start();
    }
}
