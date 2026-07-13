# Redy Crosshair

Redy Crosshair is a client-side Fabric mod that tints the active resource pack's crosshair when Minecraft has an entity the player can actually attack. Red (`#FF0000`) is the default.

## Features

- Preserves the active resource pack's crosshair shape and transparency. Indicator mode adds its corner overlay without replacing the pack's crosshair.
- Uses Minecraft's own crosshair-picked entity, then rejects spectators, dead entities, and entities that skip attacks.
- Built-in color editor with an HSV wheel, brightness bar, hex input, RGB input, live preview, and separate Hit/Crit color selection.
- The settings screen previews the current vanilla or resource-pack crosshair at its real HUD size and updates immediately from unsaved color, indicator, crit, and blending changes.
- Optional critical-hit color has priority over the normal hit color and follows vanilla's actual crit requirements. It is disabled by default with bright blue (`#0080FF`) preselected.
- The entire mod can be disabled from its settings screen; disabled mode leaves the resource-pack crosshair and its blending untouched.
- Optional Crosshair Indicator style overlays four corner brackets without replacing the active resource-pack crosshair. The brackets can stay white, use the selected color together with the crosshair, or use the selected color on only the corners.
- Configuration is available from the compact crosshair-logo button at the top-right of **Options**, and from Redy Crosshair's gear button when Mod Menu is installed. It is saved to `config/redycrosshair.properties`.
- Blending is disabled by default only while Redy is active, producing a solid selected color instead of vanilla's inverted-color blend. Both blending switches apply to classic and indicator styles and can be changed independently.
- No Fabric API, YACL, Cloth Config, or Mod Menu dependency. Only Fabric Loader is required.
- One universal release JAR contains the small compatible implementations and lets Fabric Loader select the right one.
- The selected compatibility implementation is marked as a hidden Mod Menu library child, so Redy Crosshair is the only normal mod-list entry.

## Supported Minecraft versions

The v1.0 universal JAR covers:

- 1.21 through 1.21.11
- 26.1, 26.1.1, 26.1.2, and 26.2

The 26.2 implementation has an optimistic `>=26.2` range. For a future Minecraft version, test the existing universal JAR first. If it still works, only add that game-version tag to the existing release. If it does not work, add or update an internal implementation and bump the Redy Crosshair release version.

## Build

Use Java 25 and run:

```powershell
.\gradlew.bat universalJar
```

The only release file is:

```text
build/release/redy-crosshair-1.0.0.jar
```

The version-specific subprojects are internal nested modules, not separate downloads.

## License

MIT

The optional indicator shape is adapted from [Crosshair Indicator](https://modrinth.com/mod/crosshair-indicator), which is published under CC0-1.0.

The optional critical-hit color was inspired by [Advanced Crosshair](https://modrinth.com/mod/adv-crosshair); Redy Crosshair independently follows Minecraft's vanilla critical-hit rules.
