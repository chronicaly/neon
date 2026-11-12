package me.xchro.neon.ui;

import com.mojang.blaze3d.platform.cursor.*;
import org.lwjgl.glfw.GLFW;

/** Native OS cursor shapes, with Minecraft's portable fallback. Created on the render thread. */
final class UiCursors {
    private static CursorType northwest,southwest;
    private UiCursors() {}
    static CursorType corner(int corner) {
        if(corner==0||corner==3){if(northwest==null)northwest=CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR,"Neon northwest/southeast resize",CursorTypes.RESIZE_ALL);return northwest;}
        if(southwest==null)southwest=CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR,"Neon northeast/southwest resize",CursorTypes.RESIZE_ALL);return southwest;
    }
}
