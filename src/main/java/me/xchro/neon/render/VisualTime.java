package me.xchro.neon.render;

import java.util.Set;
import me.xchro.neon.Neon;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.*;
import net.minecraft.world.clock.*;

/** A separate clock for a strictly visual attribute allowlist, never the world's actual clock. */
public final class VisualTime implements ClockManager {
    private static final Set<EnvironmentAttribute<?>> VISUAL=Set.of(
        EnvironmentAttributes.SUN_ANGLE,EnvironmentAttributes.MOON_ANGLE,EnvironmentAttributes.STAR_ANGLE,
        EnvironmentAttributes.SKY_COLOR,EnvironmentAttributes.FOG_COLOR,EnvironmentAttributes.CLOUD_COLOR,
        EnvironmentAttributes.SUNRISE_SUNSET_COLOR,EnvironmentAttributes.STAR_BRIGHTNESS,
        EnvironmentAttributes.SKY_LIGHT_COLOR,EnvironmentAttributes.SKY_LIGHT_FACTOR);
    private static int last=-1;
    private static Object level;
    private final ClockManager original;
    public VisualTime(ClockManager original) { this.original=original; }
    public static boolean visual(EnvironmentAttribute<?> attribute) { return VISUAL.contains(attribute); }
    @Override public long getTotalTicks(Holder<WorldClock> clock) {
        long actual=original.getTotalTicks(clock);
        return ClientModule.TIME_CHANGER.active()&&clock.is(WorldClocks.OVERWORLD)
            ?actual-Math.floorMod(actual,24000)+Neon.config.visualTime:actual;
    }
    public static void tick(Minecraft mc) {
        int next=ClientModule.TIME_CHANGER.active()?Neon.config.visualTime:-1;
        if(mc.level!=null&&(level!=mc.level||last!=next))mc.level.environmentAttributes().invalidateTickCache();
        level=mc.level;last=next;
    }
}
