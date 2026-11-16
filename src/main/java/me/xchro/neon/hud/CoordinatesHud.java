package me.xchro.neon.hud;

import java.util.ArrayList;
import me.xchro.neon.Neon;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

final class CoordinatesHud extends HudCard {
    CoordinatesHud() { super(HudKind.COORDINATES); }
    @Override protected void update(Minecraft mc,boolean preview) {
        if(mc.player==null) { lines=preview ? new String[]{"XYZ  128, 64, -256"} : new String[0]; return; }
        var p=mc.player;
        var rows=new ArrayList<String>(3);
        var dimension=mc.level.dimension();
        double factor=Neon.config.coordinatesConversion?(dimension.equals(Level.NETHER)?8:dimension.equals(Level.OVERWORLD)?.125:1):1;
        rows.add("XYZ  "+HudNumbers.coordinates(p.getX(),p.getY(),p.getZ(),factor));
        lines=rows.toArray(String[]::new);
    }
}
