package dev.redycrosshair.smoke;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SmokeChecks {
    private SmokeChecks() {
    }

    public static Context verify(String guiClassName, String minecraftVersion) {
        FabricLoader loader = FabricLoader.getInstance();
        String outerVersion = loader.getModContainer("redycrosshair")
            .orElseThrow(() -> new IllegalStateException("Universal outer mod was not resolved"))
            .getMetadata().getVersion().getFriendlyString();
        String implementationVersion = loader.getModContainer("redycrosshair_impl")
            .orElseThrow(() -> new IllegalStateException("Nested implementation was not resolved"))
            .getMetadata().getVersion().getFriendlyString();
        String expectedOuterVersion = requiredProperty("redycrosshair.expectedVersion");
        String expectedImplementationVersion = requiredProperty("redycrosshair.expectedImplVersion");
        if (!expectedOuterVersion.equals(outerVersion) || !expectedImplementationVersion.equals(implementationVersion)) {
            throw new IllegalStateException("Wrong Redy Crosshair candidates: outer=" + outerVersion + ", impl=" + implementationVersion);
        }

        try {
            requireMixin(Class.forName(guiClassName));
            requireMixin(Class.forName("net.minecraft.client.gui.screens.options.OptionsScreen"));
            Class.forName("dev.redycrosshair.RedyCrosshairIconButton");
            Class.forName("dev.redycrosshair.RedyCrosshairTargeting");
            Class<?> config = Class.forName("dev.redycrosshair.RedyCrosshairConfig");
            int defaultColor = (int)config.getMethod("rgb").invoke(null);
            if (defaultColor != 0xFF0000) {
                throw new IllegalStateException("Wrong default color: " + Integer.toHexString(defaultColor));
            }
            requireCritConfig(config, loader);
            requireIndicatorConfig(config, loader);
            requireDefaultBlending(config);
            requireEnabledConfig(config, loader);
            requireMalformedConfigRecovery(config, loader);
            return new Context(requireModMenuBridge(loader), outerVersion, implementationVersion, minecraftVersion);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Expected " + minecraftVersion + " runtime contract is missing", exception);
        }
    }

    private static void requireCritConfig(Class<?> config, FabricLoader loader) throws ReflectiveOperationException {
        int defaultCritColor = (int)config.getMethod("critRgb").invoke(null);
        boolean defaultCritEnabled = (boolean)config.getMethod("critColorEnabled").invoke(null);
        int defaultResolvedCritColor = (int)config.getMethod("targetRgb", boolean.class).invoke(null, true);
        if (defaultCritEnabled || defaultCritColor != 0x0080FF || defaultResolvedCritColor != 0xFF0000) {
            throw new IllegalStateException("Wrong default crit color behavior");
        }
        config.getMethod("setCritColorEnabled", boolean.class).invoke(null, true);
        config.getMethod("setCritRgb", int.class).invoke(null, 0x123456);
        int hitColor = (int)config.getMethod("targetRgb", boolean.class).invoke(null, false);
        int critColor = (int)config.getMethod("targetRgb", boolean.class).invoke(null, true);
        if (hitColor != 0xFF0000 || critColor != 0x123456) {
            throw new IllegalStateException("Crit color did not take priority");
        }
        config.getMethod("save").invoke(null);
        Properties properties = loadProperties(loader);
        if (!"true".equals(properties.getProperty("critColorEnabled"))
            || !"#123456".equals(properties.getProperty("critColor"))) {
            throw new IllegalStateException("Crit settings were not persisted");
        }
        config.getMethod("setCritColorEnabled", boolean.class).invoke(null, false);
        config.getMethod("setCritRgb", int.class).invoke(null, 0x0080FF);
        config.getMethod("load").invoke(null);
        if (!(boolean)config.getMethod("critColorEnabled").invoke(null)
            || (int)config.getMethod("critRgb").invoke(null) != 0x123456) {
            throw new IllegalStateException("Crit settings were not loaded");
        }
        config.getMethod("setCritColorEnabled", boolean.class).invoke(null, false);
        config.getMethod("setCritRgb", int.class).invoke(null, 0x0080FF);
        config.getMethod("save").invoke(null);
    }

    private static void requireIndicatorConfig(Class<?> config, FabricLoader loader) throws ReflectiveOperationException {
        if ((boolean)config.getMethod("useIndicatorStyle").invoke(null)
            || (boolean)config.getMethod("indicatorCustomColor").invoke(null)
            || (boolean)config.getMethod("indicatorCornersOnly").invoke(null)) {
            throw new IllegalStateException("Wrong default indicator behavior");
        }
        config.getMethod("setUseIndicatorStyle", boolean.class).invoke(null, true);
        config.getMethod("setIndicatorCustomColor", boolean.class).invoke(null, true);
        config.getMethod("setIndicatorCornersOnly", boolean.class).invoke(null, true);
        config.getMethod("save").invoke(null);
        Properties properties = loadProperties(loader);
        if (!"true".equals(properties.getProperty("useIndicatorStyle"))
            || !"true".equals(properties.getProperty("indicatorCustomColor"))
            || !"true".equals(properties.getProperty("indicatorCornersOnly"))) {
            throw new IllegalStateException("Indicator settings were not persisted");
        }
        config.getMethod("setUseIndicatorStyle", boolean.class).invoke(null, false);
        config.getMethod("setIndicatorCustomColor", boolean.class).invoke(null, false);
        config.getMethod("setIndicatorCornersOnly", boolean.class).invoke(null, false);
        config.getMethod("load").invoke(null);
        if (!(boolean)config.getMethod("useIndicatorStyle").invoke(null)
            || !(boolean)config.getMethod("indicatorCustomColor").invoke(null)
            || !(boolean)config.getMethod("indicatorCornersOnly").invoke(null)) {
            throw new IllegalStateException("Indicator settings were not loaded");
        }
        config.getMethod("setUseIndicatorStyle", boolean.class).invoke(null, false);
        config.getMethod("setIndicatorCustomColor", boolean.class).invoke(null, false);
        config.getMethod("setIndicatorCornersOnly", boolean.class).invoke(null, false);
        config.getMethod("save").invoke(null);
    }

    private static void requireDefaultBlending(Class<?> config) throws ReflectiveOperationException {
        boolean disabled = (boolean)config.getMethod("disableBlending").invoke(null);
        boolean onlyWhileRedy = (boolean)config.getMethod("disableBlendingOnlyWhileRedy").invoke(null);
        boolean disabledNormally = (boolean)config.getMethod("shouldDisableBlending", boolean.class).invoke(null, false);
        boolean disabledWhileRedy = (boolean)config.getMethod("shouldDisableBlending", boolean.class).invoke(null, true);
        if (!disabled || !onlyWhileRedy || disabledNormally || !disabledWhileRedy) {
            throw new IllegalStateException("Wrong default blending behavior");
        }
    }

    private static void requireEnabledConfig(Class<?> config, FabricLoader loader) throws ReflectiveOperationException {
        if (!(boolean)config.getMethod("enabled").invoke(null)) {
            throw new IllegalStateException("Redy Crosshair should be enabled by default");
        }
        config.getMethod("setEnabled", boolean.class).invoke(null, false);
        if ((boolean)config.getMethod("shouldDisableBlending", boolean.class).invoke(null, true)) {
            throw new IllegalStateException("Disabled Redy Crosshair still changes blending");
        }
        config.getMethod("save").invoke(null);
        config.getMethod("setEnabled", boolean.class).invoke(null, true);
        config.getMethod("load").invoke(null);
        if ((boolean)config.getMethod("enabled").invoke(null)) {
            throw new IllegalStateException("Disabled state was not loaded");
        }
        Properties properties = loadProperties(loader);
        if (!"false".equals(properties.getProperty("enabled"))) {
            throw new IllegalStateException("Disabled state was not persisted");
        }
        config.getMethod("setEnabled", boolean.class).invoke(null, true);
        config.getMethod("save").invoke(null);
    }

    private static void requireMalformedConfigRecovery(Class<?> config, FabricLoader loader) throws ReflectiveOperationException {
        Path path = loader.getConfigDir().resolve("redycrosshair.properties");
        try {
            Files.writeString(path, "color=" + '\\' + "u12");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write malformed configuration fixture", exception);
        }
        config.getMethod("load").invoke(null);
        config.getMethod("save").invoke(null);
    }

    private static Properties loadProperties(FabricLoader loader) {
        Path path = loader.getConfigDir().resolve("redycrosshair.properties");
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read saved configuration", exception);
        }
    }

    private static ConfigScreenFactory<?> requireModMenuBridge(FabricLoader loader) {
        ConfigScreenFactory<?> factory = loader.getEntrypoints("modmenu", ModMenuApi.class).stream()
            .map(ModMenuApi::getProvidedConfigScreenFactories)
            .map(factories -> factories.get("redycrosshair"))
            .filter(candidate -> candidate != null)
            .findFirst()
            .orElse(null);
        if (factory == null || !factory.create(null).getClass().getName().equals("dev.redycrosshair.RedyCrosshairScreen")) {
            throw new IllegalStateException("Mod Menu did not receive Redy Crosshair's config screen");
        }
        return factory;
    }

    private static void requireMixin(Class<?> target) {
        for (var method : target.getDeclaredMethods()) {
            if (method.getName().contains("redycrosshair")) {
                return;
            }
        }
        throw new IllegalStateException("Redy Crosshair Mixin was not applied to " + target.getName());
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing system property " + name);
        }
        return value;
    }

    public record Context(
        ConfigScreenFactory<?> configFactory,
        String outerVersion,
        String implementationVersion,
        String minecraftVersion
    ) {
    }
}
