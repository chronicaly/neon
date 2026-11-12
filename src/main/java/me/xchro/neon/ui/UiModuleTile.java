package me.xchro.neon.ui;

import java.util.List;
import java.util.function.*;
import me.xchro.neon.Neon;
import me.xchro.neon.core.TimedValue;
import me.xchro.neon.module.ClientModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Compact button with separate name/body and right-aligned bind input targets. */
final class UiModuleTile extends AbstractContainerWidget {
    private final ClientModule module;
    private final UiButton body;
    private final UiBindButton binding;
    private final BooleanSupplier selected;
    private final Runnable configure;
    private TimedValue hover = new TimedValue(0), enabled;
    private double amount, on;
    private boolean wrapped;
    UiModuleTile(ClientModule module, Runnable toggle, Runnable configure, BooleanSupplier selected,
                 BooleanSupplier capturing, Runnable capture, Runnable mode) {
        this(module.label, module, toggle, configure, selected, capturing, capture, mode);
    }
    UiModuleTile(String section, Runnable open, BooleanSupplier selected) {
        this(section, null, open, open, selected, null, null, null);
    }
    private UiModuleTile(String label, ClientModule module, Runnable action, Runnable configure, BooleanSupplier selected,
                         BooleanSupplier capturing, Runnable capture, Runnable mode) {
        super(0, 0, 124, 26, Ui.label(label));
        this.module = module; this.configure = configure; this.selected = selected;
        enabled = new TimedValue(active() ? 1 : 0);
        body = new UiButton(label, 0, 0, 124, 26, UiButton.Kind.MODULE, action) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                String title=binding==null||wrapped?label:Ui.fit(label,Math.max(1,binding.textLeft()-getX()-15));
                Ui.text(g, title, getX() + 10, getY() + 7, module == null ? Ui.TEXT : UiEffects.mix(Ui.MUTED, Ui.TEXT, Math.max(on, amount)));
                if (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard()) Ui.stroke(g,getX(),getY(),getWidth(),getHeight(),Ui.BLUE);
            }
            @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
                if (event.button() == 1 && isMouseOver(event.x(), event.y())) { UiSounds.input(configure); return true; }
                return super.mouseClicked(event, twice);
            }
            @Override public boolean keyPressed(KeyEvent event) {
                if (event.key() == GLFW.GLFW_KEY_MENU || event.key() == GLFW.GLFW_KEY_F10 && (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0) {
                    UiSounds.input(configure); return true;
                }
                return super.keyPressed(event);
            }
        };
        body.identity((module == null ? "settings/" + label : module.name())+"/body")
            .hint(module == null ? "Open " + label : "Left click toggles " + label + "; right click or Shift+F10 opens settings.");
        binding = module == null ? null : new UiBindButton(module, capturing, capture, mode);
        place();
    }
    private boolean active() { return module == null ? selected.getAsBoolean() : module.enabled(Neon.config); }
    static int commonWidth() {
        int width = 124;
        for (var module : ClientModule.values()) width = Math.max(width, 27 + Ui.width(module.label) + Math.max(UiBindButton.measure("[…]"),UiBindButton.measure(SettingsScreen.bindingName(module.key()))));
        return width;
    }
    void inherit(UiModuleTile old) { hover = old.hover; enabled = old.enabled; }
    private void place() {
        if (body == null) return;
        if (binding != null) binding.refresh();
        wrapped = binding != null && 27 + Ui.width(module.label) + binding.getWidth() > getWidth();
        setHeight(wrapped ? 43 : 26);
        body.setX(getX()); body.setY(getY()); body.setWidth(getWidth()); body.setHeight(getHeight());
        if (binding == null) return;
        binding.setX(getRight() - binding.getWidth() - 6);
        binding.setY(getY() + (wrapped ? 19 : 0));
    }
    @Override public void setX(int x) { super.setX(x); place(); }
    @Override public void setY(int y) { super.setY(y); place(); }
    @Override public void setWidth(int w) { super.setWidth(w); place(); }
    @Override public List<AbstractWidget> children() { return binding == null ? List.of(body) : List.of(body, binding); }
    @Override protected int contentHeight() { return getHeight(); }
    @Override protected double scrollRate() { return 0; }
    @Override protected boolean isOverScrollbar(double x, double y) { return false; }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (!isMouseOver(event.x(),event.y())) return false;
        var target = binding != null && binding.isMouseOver(event.x(),event.y()) ? binding : body;
        if (!target.mouseClicked(event,twice)) return false;
        setFocused(target); setDragging(event.button()==0); return true;
    }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        place(); long now=System.nanoTime();
        if(binding!=null)binding.prepareHover(mx,my);
        boolean keyboard=isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
        amount=hover.to(isMouseOver(mx,my)||keyboard?1:0,now,220_000_000L);
        on=enabled.to(active()?1:0,now,100_000_000L);
        g.pose().pushMatrix(); g.pose().translate(0,(float)(-2/PixelScreen.UI_SCALE*amount));
        try {
            UiEffects.buttonGlow(g,getX(),getY(),getWidth(),getHeight(),amount);
            double emphasis=Math.max(amount,selected.getAsBoolean()?1:0);
            int cyan=Ui.buttonSurface(UiEffects.mix(Ui.FIELD,Ui.BLUE,.045*on+.12*amount),emphasis);
            int violet=Ui.buttonSurface(UiEffects.mix(Ui.FIELD,UiEffects.hoverViolet(),.045*on+.22*amount),emphasis);
            UiEffects.gradient(g,getX(),getY(),getWidth(),getHeight(),cyan,violet,violet,cyan);
            int idleEdge=UiEffects.mix(Ui.LINE,UiEffects.mix(Ui.BLUE,Ui.VIOLET,.25),Math.max(.25*on,selected.getAsBoolean()?.42:0));
            int leftEdge=UiEffects.mix(idleEdge,UiEffects.mix(Ui.BLUE,UiEffects.hoverViolet(),.18),amount);
            int rightEdge=UiEffects.mix(idleEdge,UiEffects.hoverViolet(),amount);
            UiEffects.stroke(g,getX(),getY(),getWidth(),getHeight(),Ui.faded(leftEdge),Ui.faded(rightEdge));
            if(module != null) Ui.rect(g,getX()+4,getY()+8,1,10,UiEffects.mix(Ui.LINE,Ui.BLUE,on));
            body.extractRenderState(g,mx,my,delta);
            if(binding != null) binding.extractRenderState(g,mx,my,delta);
        } finally { g.pose().popMatrix(); }
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        if (getFocused() instanceof AbstractWidget widget) widget.updateNarration(output);
    }
}
