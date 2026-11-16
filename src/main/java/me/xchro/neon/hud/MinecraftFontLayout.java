package me.xchro.neon.hud;

import org.joml.Matrix3x2f;

/** Bitmap glyphs need whole screen pixels, including after the menu's transforms. */
public final class MinecraftFontLayout {
    private MinecraftFontLayout() {}

    public static float menuScale(float configuredScale, float guiScale) {
        // Menu metrics are twice vanilla's. Round the final size, not either
        // intermediate transform, so Minecraft GUI scales 1–4 behave alike.
        return Math.max(1, Math.round(configuredScale * guiScale)) / (2f * guiScale);
    }

    public static void snapOrigin(Matrix3x2f pose, float guiScale) {
        pose.setTranslation(Math.round(pose.m20() * guiScale) / guiScale,
                Math.round(pose.m21() * guiScale) / guiScale);
    }
}

