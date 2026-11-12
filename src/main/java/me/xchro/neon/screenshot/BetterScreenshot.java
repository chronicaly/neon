package me.xchro.neon.screenshot;

import com.mojang.blaze3d.platform.NativeImage;
import com.sun.jna.Platform;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;
import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFWNativeWin32;

/** Unity's ExtraScreenshot clipboard worker adapted to Neon's module and local chat APIs. */
public final class BetterScreenshot {
    private static final ExecutorService CLIPBOARD = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Neon screenshot clipboard"); thread.setDaemon(true); return thread;
    });
    private BetterScreenshot() {}

    public static void capture(NativeImage image) {
        if (!Neon.MODULE_BINDINGS.active(ClientModule.BETTER_SCREENSHOT)) return;
        Minecraft mc = Minecraft.getInstance();
        boolean notify = Neon.config.betterScreenshot.notify;
        try {
            // Copy before vanilla's asynchronous PNG writer closes the native image.
            int width = image.getWidth(), height = image.getHeight();
            int[] pixels = image.getPixels();
            boolean windows = Platform.isWindows();
            long window = windows ? GLFWNativeWin32.glfwGetWin32Window(mc.getWindow().handle()) : 0;
            CLIPBOARD.execute(() -> {
                try {
                    if (windows) WindowsImageClipboard.copy(window, width, height, pixels);
                    else copyDesktop(width, height, pixels);
                    if (notify) message(mc, "Screenshot copied to clipboard");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt(); failed(mc, interrupted);
                } catch (Exception | LinkageError failure) { failed(mc, failure); }
            });
        } catch (Exception | LinkageError failure) { failed(mc, failure); }
    }

    private static void copyDesktop(int width, int height, int[] pixels) throws InterruptedException {
        BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        copy.setRGB(0, 0, width, height, pixels, 0, width);
        Transferable transferable = new Transferable() {
            public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.imageFlavor}; }
            public boolean isDataFlavorSupported(DataFlavor flavor) { return DataFlavor.imageFlavor.equals(flavor); }
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
                return copy;
            }
        };
        for (int attempt = 0; ; attempt++) {
            try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null); return; }
            catch (IllegalStateException busy) { if (attempt == 4) throw busy; Thread.sleep(100); }
        }
    }

    private static void failed(Minecraft mc, Throwable failure) {
        Neon.LOG.warn("Could not copy screenshot to clipboard", failure);
        message(mc, "Could not copy screenshot: " + failure.getClass().getSimpleName());
    }
    private static void message(Minecraft mc, String text) {
        // A local notice must not trigger Better chat's player/server message pings.
        mc.execute(() -> mc.gui.hud.getChat().addClientSystemMessage(Component.literal("Neon: " + text)));
    }
    public static void close() { CLIPBOARD.shutdown(); }
}
