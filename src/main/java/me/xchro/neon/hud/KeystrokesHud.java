package me.xchro.neon.hud;

import me.xchro.neon.Neon;
import me.xchro.neon.config.ThemeColor;
import me.xchro.neon.core.UiMotion;
import me.xchro.neon.ui.Ui;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class KeystrokesHud extends HudCard {
    private final UiMotion[] presses=new UiMotion[7];
    KeystrokesHud() { super(HudKind.KEYSTROKES);for(int i=0;i<presses.length;i++)presses[i]=new UiMotion(0); }
    @Override protected void update(Minecraft mc,boolean preview) { lines=new String[]{"Keys"}; }
    @Override protected int contentWidth() { return 72; }
    @Override protected int contentHeight() { return 45+(Neon.config.keystrokesMouse?24:0)+(Neon.config.keystrokesJump?18:0); }
    @Override protected void renderContent(GuiGraphicsExtractor g,boolean preview) {
        var mc=Minecraft.getInstance();var o=mc.options;boolean live=Neon.ready(mc);
        key(g,0,o.keyUp,29,4,22,20,live);
        key(g,1,o.keyLeft,5,28,22,20,live);
        key(g,2,o.keyDown,29,28,22,20,live);
        key(g,3,o.keyRight,53,28,22,20,live);
        int y=52;
        if(Neon.config.keystrokesMouse) {
            // These labels intentionally describe physical mouse buttons, not mapped attacks.
            cell(g,4,"LMB",5,y,34,20,live&&mc.mouseHandler.isLeftPressed());
            cell(g,5,"RMB",41,y,34,20,live&&mc.mouseHandler.isRightPressed());y+=24;
        }
        if(Neon.config.keystrokesJump) key(g,6,o.keyJump,5,y,70,14,live);
    }
    private void key(GuiGraphicsExtractor g,int index,KeyMapping key,int x,int y,int w,int h,boolean live) {
        String label=key.isUnbound()?"—":key.getTranslatedKeyMessage().getString();
        cell(g,index,label,x,y,w,h,live&&key.isDown());
    }
    private void cell(GuiGraphicsExtractor g,int index,String text,int x,int y,int w,int h,boolean pressed) {
        double amount=presses[index].to(pressed?1:0,System.nanoTime(),pressed?50_000_000L:100_000_000L);
        var t=Neon.config.theme;
        int accent=t.color(ThemeColor.ACCENT),muted=t.color(ThemeColor.MUTED);
        if(HudChrome.backgrounds()||amount>.01)g.fill(x,y,x+w,y+h,Ui.alpha(accent,(int)((accent>>>24)*((HudChrome.backgrounds()?.06:0)+.35*amount))));
        if(HudChrome.outlines()||amount>.01)g.fill(x,y+h-1,x+w,y+h,Ui.alpha(accent,(int)((accent>>>24)*((HudChrome.outlines()?.20:0)+.65*amount))));
        String label=this.text.trim(text,w-4);
        this.text.draw(g,label,x+(w-this.text.width(label))/2,y+(h-this.text.height())/2,
            amount>.35?accent:t.color(ThemeColor.TEXT),Neon.config.hud(kind).shadow);
    }
}
