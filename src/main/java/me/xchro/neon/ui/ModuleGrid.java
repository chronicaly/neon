package me.xchro.neon.ui;

import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.layouts.*;

/** Content-sized equal-width buttons, with space for lifted borders and local bloom. */
final class ModuleGrid extends AbstractLayout {
    private final List<UiModuleTile> tiles=new ArrayList<>();
    private final IntSupplier available;
    ModuleGrid(IntSupplier available){super(0,0,0,0);this.available=available;}
    void add(UiModuleTile tile){tiles.add(tile);}
    @Override public void arrangeElements(){
        int tileWidth=Math.min(UiModuleTile.commonWidth(),Math.max(1,available.getAsInt()-24));
        int x=12,y=14,rowHeight=0; width=height=0;
        for(var tile:tiles){
            tile.setWidth(tileWidth);
            if(x>12&&x+tileWidth>available.getAsInt()-12){x=12;y+=rowHeight+10;rowHeight=0;}
            tile.setX(getX()+x);tile.setY(getY()+y);x+=tileWidth+10;
            rowHeight=Math.max(rowHeight,tile.getHeight());width=Math.max(width,x+2);height=y+rowHeight+12;
        }
    }
    @Override public void visitChildren(Consumer<LayoutElement> visitor){tiles.forEach(visitor);}
    @Override public void removeChildren(){tiles.clear();}
}
