package me.xchro.neon.input;

import me.xchro.neon.Neon;
import me.xchro.neon.core.ClickWindow;
import net.minecraft.client.Minecraft;

public final class InputTelemetry {
    private static final ClickWindow LEFT=new ClickWindow(),RIGHT=new ClickWindow();
    private InputTelemetry() {}
    public static void press(int button) {
        if(!Neon.ready(Minecraft.getInstance()))return;
        if(button==0)LEFT.press(System.nanoTime());else if(button==1)RIGHT.press(System.nanoTime());
    }
    public static int left() { return LEFT.count(System.nanoTime()); }
    public static int right() { return RIGHT.count(System.nanoTime()); }
    public static void tick(Minecraft mc) { if(!Neon.ready(mc)) { LEFT.clear();RIGHT.clear(); } }
}
