package me.xchro.neon.hud;

import java.util.ArrayList;
import me.xchro.neon.Neon;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

final class CoordinatesHud extends HudCard {
    CoordinatesHud() { super(HudKind.COORDINATES); }
    @Override protected void update(Minecraft mc,boolean preview) {
        if(mc.player==null) { lines=preview ? new String[]{"XYZ  128  64  -256","Overworld"} : new String[0]; return; }
        var p=mc.player;
        var rows=new ArrayList<String>(3);
        rows.add("XYZ  "+HudNumbers.coordinate(p.getX(),1)+"  "+HudNumbers.coordinate(p.getY(),1)+"  "+HudNumbers.coordinate(p.getZ(),1));
        var dimension=mc.level.dimension();
        if(Neon.config.coordinatesDimension) rows.add(dimension.equals(Level.OVERWORLD) ? "Overworld" : dimension.equals(Level.NETHER) ? "The Nether" : dimension.equals(Level.END) ? "The End" : dimension.identifier().getPath());
        if(Neon.config.coordinatesConversion && (dimension.equals(Level.OVERWORLD)||dimension.equals(Level.NETHER))) {
            boolean nether=dimension.equals(Level.NETHER);
            double factor=nether ? 8 : .125;
            rows.add((nether ? "Overworld  " : "Nether  ")+HudNumbers.coordinate(p.getX(),factor)+"  "+HudNumbers.coordinate(p.getZ(),factor));
        }
        lines=rows.toArray(String[]::new);
    }
}
