package dev.redycrosshair.smoke;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class SmokeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SmokeChecks.Context context = SmokeChecks.verify("net.minecraft.client.gui.Hud", "26.3");
        Thread smoke = new Thread(() -> run(context), "redycrosshair-26.3-smoke");
        smoke.setDaemon(true);
        smoke.start();
    }

    private static void run(SmokeChecks.Context context) {
        Minecraft client = Minecraft.getInstance();
        try {
            await(client, () -> {
                if (client.gui != null && client.gui.screen() instanceof AccessibilityOnboardingScreen onboarding) {
                    onboarding.onClose();
                }
                return client.gui != null && client.gui.screen() != null
                    && client.gui.screen().getClass().getName().equals("net.minecraft.client.gui.screens.TitleScreen");
            }, "title screen");
            onClient(client, () -> {
                Path runDirectory = client.gameDirectory.toPath().toRealPath();
                if (!runDirectory.endsWith(Path.of("build", "smoke-run-26.3"))
                    || !client.getLevelSource().getBaseDir().toAbsolutePath().normalize().startsWith(runDirectory)) {
                    throw new IllegalStateException("Smoke refuses world creation outside build/smoke-run-26.3: " + runDirectory);
                }
                client.gui.setScreen(context.configFactory().create(client.gui.screen()));
                return null;
            });
            await(client, () -> client.gui.screen() != null
                && client.gui.screen().getClass().getName().equals("dev.redycrosshair.RedyCrosshairScreen"), "settings preview");
            Screen preview = onClient(client, () -> client.gui.screen());
            Thread.sleep(2_000L);
            onClient(client, () -> {
                if (client.gui.screen() != preview) {
                    throw new IllegalStateException("Settings screen closed while rendering preview");
                }
                AbstractWidget wheel = (AbstractWidget)preview.children().stream()
                    .filter(child -> child.getClass().getName().equals("dev.redycrosshair.ColorWheelWidget"))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Settings color wheel is missing"));
                EditBox hex = (EditBox)preview.children().stream()
                    .filter(child -> child instanceof EditBox box
                        && box.getMessage().getContents() instanceof TranslatableContents text
                        && text.getKey().equals("redycrosshair.hex"))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Settings hex input is missing"));
                MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
                MouseButtonEvent center = new MouseButtonEvent(wheel.getX() + (wheel.getWidth() - 1) / 2.0,
                    wheel.getY() + (wheel.getWidth() - 1) / 2.0, left);
                if (!preview.mouseClicked(center, false) || hexRgb(hex) != 0xFFFFFF) {
                    throw new IllegalStateException("Color wheel click did not select white: " + hex.getValue());
                }
                preview.mouseReleased(center);
                MouseButtonEvent dark = new MouseButtonEvent(wheel.getX(), wheel.getY() + wheel.getHeight() - 6, left);
                if (!preview.mouseClicked(dark, false) || hexRgb(hex) != 0x000000) {
                    throw new IllegalStateException("Brightness click did not select black: " + hex.getValue());
                }
                MouseButtonEvent bright = new MouseButtonEvent(wheel.getX() + wheel.getWidth() - 1, dark.y(), left);
                if (!preview.mouseDragged(bright, wheel.getWidth() - 1, 0) || hexRgb(hex) != 0xFFFFFF) {
                    throw new IllegalStateException("Brightness drag did not restore white: " + hex.getValue());
                }
                preview.mouseReleased(bright);
                MouseButtonEvent red = new MouseButtonEvent(wheel.getX() + wheel.getWidth() - 0.5, center.y(), left);
                if (!preview.mouseClicked(red, false) || hexRgb(hex) != 0xFF0000) {
                    throw new IllegalStateException("Color wheel did not select saturated red: " + hex.getValue());
                }
                preview.mouseReleased(red);
                System.out.println("REDY_COLOR_INPUT_OK wheel=click brightness=click-and-drag hex=synchronized");
                return null;
            });
            capture(client, "26.3-preview.png");
            System.out.println("REDY_PREVIEW_OK minecraft=26.3 screenshot=26.3-preview.png");
            onClient(client, () -> {
                preview.onClose();
                Screen parent = client.gui.screen();
                if (parent == null || !parent.getClass().getName().equals("net.minecraft.client.gui.screens.TitleScreen")) {
                    throw new IllegalStateException("Mod Menu settings did not return to its title parent");
                }
                OptionsScreen options = new OptionsScreen(parent, client.options);
                client.gui.setScreen(options);
                var logoButtons = options.children().stream().filter(child -> child instanceof Button button
                    && button.getMessage().getContents() instanceof TranslatableContents text
                    && text.getKey().equals("redycrosshair.options")).toList();
                if (logoButtons.size() != 1) {
                    throw new IllegalStateException("Initialized OptionsScreen must contain exactly one Redy logo button");
                }
                Button logo = (Button)logoButtons.getFirst();
                if (!options.mouseClicked(new MouseButtonEvent(logo.getX() + logo.getWidth() / 2.0,
                    logo.getY() + logo.getHeight() / 2.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false)
                    || client.gui.screen() == null
                    || !client.gui.screen().getClass().getName().equals("dev.redycrosshair.RedyCrosshairScreen")) {
                    throw new IllegalStateException("Options logo mouse dispatch did not open settings");
                }
                client.gui.screen().onClose();
                if (client.gui.screen() != options) {
                    throw new IllegalStateException("Options settings onClose did not return to its parent");
                }
                System.out.println("REDY_OPTIONS_NAV_OK logo=mouse-dispatched close=parent-restored");
                return null;
            });
            onClient(client, () -> {
                CreateWorldScreen.testWorld(client, () -> {
                    throw new IllegalStateException("Smoke test-world creation was cancelled");
                });
                if (!(client.gui.screen() instanceof CreateWorldScreen create)) {
                    throw new IllegalStateException("Vanilla testWorld did not open CreateWorldScreen");
                }
                create.getUiState().setName("Redy 26.3 smoke");
                create.getUiState().setSeed("263");
                Method onCreate = CreateWorldScreen.class.getDeclaredMethod("onCreate");
                onCreate.setAccessible(true);
                onCreate.invoke(create);
                return null;
            });
            await(client, () -> {
                if (client.gui.screen() instanceof ConfirmScreen confirmation
                    && confirmation.getTitle().getContents() instanceof TranslatableContents title
                    && (title.getKey().equals("selectWorld.warning.experimental.title")
                        || title.getKey().equals("selectWorld.warning.deprecated.title"))) {
                    Button proceed = (Button)confirmation.children().stream()
                        .filter(child -> child instanceof Button button && button.getMessage().equals(CommonComponents.GUI_YES))
                        .findFirst().orElseThrow(() -> new IllegalStateException("Test-world warning has no confirmation button"));
                    if (!confirmation.mouseClicked(new MouseButtonEvent(proceed.getX() + proceed.getWidth() / 2.0,
                        proceed.getY() + proceed.getHeight() / 2.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false)) {
                        throw new IllegalStateException("Test-world warning confirmation was rejected");
                    }
                }
                return client.level != null && client.player != null && client.gameMode != null
                    && client.gui.screen() == null && client.player.connection.getPlayerInfo(client.player.getUUID()) != null;
            }, "flat test-world player");
            onClient(client, () -> {
                GameplayChecks.run(client);
                return null;
            });
            Thread.sleep(2_000L);
            capture(client, "26.3-gameplay.png");
            System.out.println(
                "REDY_CROSSHAIR_SMOKE_OK minecraft=" + context.minecraftVersion()
                    + " outer=" + context.outerVersion()
                    + " impl=" + context.implementationVersion()
                    + " preview=rendered gameplay=checked rendering=extracted screenshot=26.3-gameplay.png"
            );
            onClient(client, () -> {
                client.stop();
                return null;
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            client.execute(client::stop);
            System.exit(1);
        }
    }

    private static <T> T onClient(Minecraft client, Callable<T> action) throws Exception {
        CompletableFuture<T> completion = new CompletableFuture<>();
        client.execute(() -> {
            try {
                completion.complete(action.call());
            } catch (Throwable throwable) {
                completion.completeExceptionally(throwable);
            }
        });
        try {
            return completion.get(120L, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            throw new IllegalStateException("Minecraft client thread did not complete its smoke step within 120 seconds", exception);
        }
    }

    private static void await(Minecraft client, Callable<Boolean> ready, String description) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120L);
        while (!onClient(client, ready)) {
            if (System.nanoTime() >= deadline) {
                String currentScreen = onClient(client, () -> client.gui.screen() == null
                    ? "none" : client.gui.screen().getClass().getName());
                throw new IllegalStateException("Timed out waiting for " + description + "; current screen: " + currentScreen);
            }
            Thread.sleep(50L);
        }
    }

    private static int hexRgb(EditBox input) {
        String hex = input.getValue();
        return Integer.parseInt(hex, hex.startsWith("#") ? 1 : 0, hex.length(), 16);
    }

    private static void capture(Minecraft client, String filename) throws Exception {
        CompletableFuture<Void> captured = new CompletableFuture<>();
        onClient(client, () -> {
            Path screenshot = client.gameDirectory.toPath().resolve("screenshots").resolve(filename);
            Files.deleteIfExists(screenshot);
            Screenshot.grab(client.gameDirectory, filename, client.gameRenderer.mainRenderTarget(), 1, message -> {
                try {
                    if (!Files.isRegularFile(screenshot) || Files.size(screenshot) <= 8L) {
                        throw new IllegalStateException("Engine screenshot failed: " + message.getString());
                    }
                    captured.complete(null);
                } catch (Throwable throwable) {
                    captured.completeExceptionally(throwable);
                }
            });
            return null;
        });
        try {
            captured.get(30L, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            throw new IllegalStateException("Engine screenshot did not finish within 30 seconds: " + filename, exception);
        }
    }
}
