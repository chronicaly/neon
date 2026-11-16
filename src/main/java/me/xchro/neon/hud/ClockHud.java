package me.xchro.neon.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import me.xchro.neon.Neon;
import net.minecraft.client.Minecraft;

final class ClockHud extends HudCard {
    private static final DateTimeFormatter[] FORMATS={DateTimeFormatter.ofPattern("HH:mm",Locale.ROOT),DateTimeFormatter.ofPattern("HH:mm:ss",Locale.ROOT),DateTimeFormatter.ofPattern("h:mm a",Locale.ROOT),DateTimeFormatter.ofPattern("h:mm:ss a",Locale.ROOT)};
    ClockHud() { super(HudKind.CLOCK); }
    @Override protected long revision(Minecraft mc) { return System.currentTimeMillis()/1000; }
    @Override protected void update(Minecraft mc,boolean preview) {
        int format=(Neon.config.clock24Hour?0:2)+(Neon.config.clockSeconds?1:0);
        lines=new String[]{"CLOCK  "+LocalTime.now().format(FORMATS[format])};
    }
}
