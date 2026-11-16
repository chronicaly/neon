package me.xchro.neon.hud;

import me.xchro.neon.input.InputTelemetry;
import net.minecraft.client.Minecraft;

final class CpsHud extends HudCard {
    CpsHud() { super(HudKind.CPS); }
    @Override protected void update(Minecraft mc,boolean preview) {
        lines=new String[]{"CPS  "+InputTelemetry.left()+" L  /  "+InputTelemetry.right()+" R"};
    }
    @Override protected int contentWidth() { return Math.max(text.width("CPS  00 L  /  00 R"),super.contentWidth()); }
}
