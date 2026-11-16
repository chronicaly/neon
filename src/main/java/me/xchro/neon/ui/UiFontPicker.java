package me.xchro.neon.ui;

import java.util.*;
import java.util.function.Consumer;
import me.xchro.neon.Neon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarrationElementOutput;

/** A fixed search/preview above a smooth, scrollbar-free list in the existing settings drawer. */
final class UiFontPicker extends AbstractContainerWidget {
    private final Consumer<String> choose;
    private List<AbstractWidget> controls=List.of();
    private String filter;
    private final List<UiButton> options=new ArrayList<>();
    private final UiEditBox search;
    private UiScroll list;
    UiFontPicker(int width,int height,String query,Consumer<String> queryChanged,Consumer<String> choose){
        super(0,0,width,height,Ui.label("Installed fonts"));this.choose=choose;
        search=new UiEditBox(7,6,width-14,"Search fonts");search.setMaxLength(128);search.setHint(Ui.label("Search installed fonts"));search.setValue(query);
        search.setResponder(value->{queryChanged.accept(value);populate();});populate();
    }
    private void populate(){
        if(Objects.equals(filter,search.getValue()))return;filter=search.getValue();
        var rows=LinearLayout.vertical().spacing(4);options.clear();
        String query=search.getValue().strip().toLowerCase(Locale.ROOT);
        for(String family:FontCatalog.families())if(family.toLowerCase(Locale.ROOT).contains(query)) {
            var option=new UiButton(family,0,0,getWidth()-2,26,UiButton.Kind.CARD,()->choose.accept(family))
                .selected(()->family.equalsIgnoreCase(Neon.config.clientFont)).identity("font/"+family).hint(family.equals(FontCatalog.DEFAULT)?"Bundled Neon font.":family+" — installed on this computer.");
            options.add(option);rows.addChild(option);
        }
        if(options.isEmpty())rows.addChild(new UiLabel("No matching fonts",getWidth()-2,26,Ui.MUTED));
        var previous=list;list=new UiScroll(getX()+1,getY()+61,getWidth()-2,Math.max(26,getHeight()-62),rows);
        controls=List.of(search,list);
        if(getFocused()==previous)setFocused(search);
        place();
    }
    private void place(){
        if(search==null)return;search.setX(getX()+7);search.setY(getY()+6);search.setWidth(Math.max(1,getWidth()-14));
        if(list!=null){list.setX(getX()+1);list.setY(getY()+61);list.setWidth(Math.max(1,getWidth()-2));list.setHeight(Math.max(26,getHeight()-62));}
    }
    @Override public void setX(int x){super.setX(x);place();}
    @Override public void setY(int y){super.setY(y);place();}
    @Override public void setWidth(int width){super.setWidth(width);if(options!=null)for(var option:options)option.setWidth(Math.max(1,width-2));place();if(list!=null)list.relayout();}
    @Override public void setHeight(int height){super.setHeight(height);place();}
    @Override public List<AbstractWidget> children(){return controls;}
    @Override protected int contentHeight(){return getHeight();}
    @Override protected double scrollRate(){return 0;}
    @Override protected boolean isOverScrollbar(double x,double y){return false;}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){return list.mouseScrolled(x,y,dx,dy);}
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        Ui.clip(g,getX(),getY(),getRight(),getBottom());
        search.extractRenderState(g,mx,my,delta);
        Ui.text(g,Ui.fit("Aa Bb 0123456789",getWidth()-8),getX()+4,getY()+32,Ui.TEXT);
        String status=FontCatalog.available(Neon.config.clientFont)?Neon.config.clientFont:"Using Inter (missing font)";
        Ui.small(g,Ui.fit(status,getWidth()-8),getX()+4,getY()+47,Ui.MUTED);
        list.extractRenderState(g,mx,my,delta);g.disableScissor();
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output){if(getFocused() instanceof AbstractWidget widget)widget.updateNarration(output);}
}
