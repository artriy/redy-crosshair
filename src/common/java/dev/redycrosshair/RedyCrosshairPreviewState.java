package dev.redycrosshair;

public record RedyCrosshairPreviewState(
    boolean enabled,
    int rgb,
    boolean indicatorStyle,
    boolean customColor,
    boolean cornersOnly,
    boolean disableBlending
) {
    public RedyCrosshairPreviewState {
        rgb &= 0xFFFFFF;
    }

    public int argb() {
        return 0xFF000000 | this.rgb;
    }

    public boolean indicatorActive() {
        return this.enabled && this.indicatorStyle;
    }

    public boolean customColorActive() {
        return indicatorActive() && this.customColor;
    }

    public boolean tintBase() {
        return this.enabled && (
            !this.indicatorStyle || (this.customColor && !this.cornersOnly)
        );
    }

    public boolean disableBlendingActive() {
        return this.enabled && this.disableBlending;
    }
}
