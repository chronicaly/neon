package me.xchro.neon.hud;

import java.util.EnumMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudWidgets {
    public static final me.xchro.neon.core.SessionClock SESSION=new me.xchro.neon.core.SessionClock();
    private static final EnumMap<HudKind,HudCard> CARDS = new EnumMap<>(HudKind.class);
    static {
        register(new CoordinatesHud()); register(new CompassHud()); register(new PingHud());
        register(new SpeedHud()); register(new CpsHud()); register(new KeystrokesHud());
        register(new PotionHud()); register(new InventoryHud()); register(new HeldItemHud());
        register(new ClockHud()); register(new SessionHud()); register(new BiomeHud());
        register(new LightHud()); register(new PackHud()); register(new ServerHud());
        register(new MovementHud());
        register(new WorldTimeHud());
        register(new ItemCounterHud());
        register(new ScoreboardHud());
        register(new MemoryHud());
    }
    private HudWidgets() {}
    private static void register(HudCard card) { CARDS.put(card.kind,card); }
    public static HudCard get(HudKind kind) { return CARDS.get(kind); }
    public static void draw(GuiGraphicsExtractor g,int width,int height,boolean preview) {
        for(var card:CARDS.values()) card.draw(g,width,height,preview);
    }
    public static void invalidate() { CARDS.values().forEach(HudCard::invalidate); }
}
