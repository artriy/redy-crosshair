package dev.redycrosshair;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Properties;

public final class RedyCrosshairConfig {
    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_RGB = 0xFF0000;
    public static final boolean DEFAULT_USE_INDICATOR_STYLE = false;
    public static final boolean DEFAULT_INDICATOR_CUSTOM_COLOR = false;
    public static final boolean DEFAULT_INDICATOR_CORNERS_ONLY = false;
    public static final boolean DEFAULT_DISABLE_BLENDING = true;
    public static final boolean DEFAULT_DISABLE_BLENDING_ONLY_WHILE_REDY = true;
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("redycrosshair.properties");
    private static boolean enabled = DEFAULT_ENABLED;
    private static int rgb = DEFAULT_RGB;
    private static boolean useIndicatorStyle = DEFAULT_USE_INDICATOR_STYLE;
    private static boolean indicatorCustomColor = DEFAULT_INDICATOR_CUSTOM_COLOR;
    private static boolean indicatorCornersOnly = DEFAULT_INDICATOR_CORNERS_ONLY;
    private static boolean disableBlending = DEFAULT_DISABLE_BLENDING;
    private static boolean disableBlendingOnlyWhileRedy = DEFAULT_DISABLE_BLENDING_ONLY_WHILE_REDY;

    static {
        load();
    }

    private RedyCrosshairConfig() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static int rgb() {
        return rgb;
    }

    public static int argb() {
        return 0xFF000000 | rgb;
    }

    public static void setRgb(int color) {
        rgb = color & 0xFFFFFF;
    }

    public static boolean useIndicatorStyle() {
        return useIndicatorStyle;
    }

    public static void setUseIndicatorStyle(boolean value) {
        useIndicatorStyle = value;
    }

    public static boolean indicatorCustomColor() {
        return indicatorCustomColor;
    }

    public static void setIndicatorCustomColor(boolean value) {
        indicatorCustomColor = value;
    }

    public static boolean indicatorCornersOnly() {
        return indicatorCornersOnly;
    }

    public static void setIndicatorCornersOnly(boolean value) {
        indicatorCornersOnly = value;
    }

    public static boolean disableBlending() {
        return disableBlending;
    }

    public static void setDisableBlending(boolean value) {
        disableBlending = value;
    }

    public static boolean disableBlendingOnlyWhileRedy() {
        return disableBlendingOnlyWhileRedy;
    }

    public static void setDisableBlendingOnlyWhileRedy(boolean value) {
        disableBlendingOnlyWhileRedy = value;
    }

    public static boolean shouldDisableBlending(boolean redyActive) {
        return enabled && disableBlending && (!disableBlendingOnlyWhileRedy || redyActive);
    }

    public static String hex() {
        return String.format(Locale.ROOT, "#%06X", rgb);
    }

    public static void load() {
        if (!Files.isRegularFile(PATH)) {
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(PATH)) {
            properties.load(reader);
            enabled = parseBoolean(properties.getProperty("enabled"), DEFAULT_ENABLED);
            Integer parsed = parseHex(properties.getProperty("color"));
            if (parsed != null) {
                rgb = parsed;
            }
            useIndicatorStyle = parseBoolean(properties.getProperty("useIndicatorStyle"), DEFAULT_USE_INDICATOR_STYLE);
            indicatorCustomColor = parseBoolean(
                properties.getProperty("indicatorCustomColor"),
                DEFAULT_INDICATOR_CUSTOM_COLOR
            );
            indicatorCornersOnly = parseBoolean(
                properties.getProperty("indicatorCornersOnly"),
                DEFAULT_INDICATOR_CORNERS_ONLY
            );
            disableBlending = parseBoolean(properties.getProperty("disableBlending"), DEFAULT_DISABLE_BLENDING);
            disableBlendingOnlyWhileRedy = parseBoolean(
                properties.getProperty("disableBlendingOnlyWhileRedy"),
                DEFAULT_DISABLE_BLENDING_ONLY_WHILE_REDY
            );
        } catch (IOException exception) {
            System.err.println("[Redy Crosshair] Could not read " + PATH + ": " + exception.getMessage());
        }
    }

    public static void save() {
        Properties properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("color", hex());
        properties.setProperty("useIndicatorStyle", Boolean.toString(useIndicatorStyle));
        properties.setProperty("indicatorCustomColor", Boolean.toString(indicatorCustomColor));
        properties.setProperty("indicatorCornersOnly", Boolean.toString(indicatorCornersOnly));
        properties.setProperty("disableBlending", Boolean.toString(disableBlending));
        properties.setProperty("disableBlendingOnlyWhileRedy", Boolean.toString(disableBlendingOnlyWhileRedy));
        Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                properties.store(writer, "Redy Crosshair configuration");
            }
            Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            System.err.println("[Redy Crosshair] Could not write " + PATH + ": " + exception.getMessage());
        }
    }

    public static Integer parseHex(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean parseBoolean(String input, boolean fallback) {
        if (input == null) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(input.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(input.trim())) {
            return false;
        }
        return fallback;
    }
}
