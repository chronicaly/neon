package me.xchro.neon.ui;

import java.util.*;
import java.util.function.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.NeonConfig;
import me.xchro.neon.config.FpsColor;
import me.xchro.neon.core.UiMotion;
import me.xchro.neon.core.ExpandingMenu;
import me.xchro.neon.config.HudPanelPlacement;
import me.xchro.neon.config.NeonConfig.ArmorAnchor;
import me.xchro.neon.hud.*;
import me.xchro.neon.core.NumberValue;
import me.xchro.neon.core.UiSoundPolicy.Cue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Unity HudEditorScreen's root/group/bool/range row model, adapted to Neon's widgets and renderer. */
public final class HudEditor extends PixelScreen {
    private enum Element { ARMOR, TOTEMS, FPS, COORDINATES, COMPASS, PING, SPEED, CPS, KEYSTROKES, POTIONS, INVENTORY, HELD_ITEM, CLOCK, SESSION, BIOME, LIGHT, PACKS, SERVER, MOVEMENT, WORLD_TIME, ITEM_COUNTER, SCOREBOARD, MEMORY, CHAT }
    private static final Element[] CANVAS_ORDER = Element.values();
    private record CanvasHit(Element element, int corner) {}
    private static final int COLUMN_TOP=28;
    private static final int COLLAPSED_HEIGHT = COLUMN_TOP + 1 + Math.min(7,Element.values().length + 1) * 22;
    private enum Gesture { NONE, MOVE, RESIZE, PANEL }
    private final Screen parent;
    private final SettingsSession session;
    private final java.nio.file.Path placementFile = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("neon-hud-window.json");
    private final HudPanelPlacement placement;
    private int viewportWidth, viewportHeight;
    private final List<Runnable> refreshers = new ArrayList<>();
    private final List<AbstractWidget> inspector = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>();
    private final Map<String, UiModuleFold> folds = new HashMap<>();
    private record Row(String key, String label, int depth, Object value, Runnable left, Runnable right) {}
    private record Range(double value, double min, double max, double step, DoubleConsumer setter) {}
    private UiScroll column;
    private UiSearch search;
    private String query="";
    private boolean filtering,refocusSearch,searchOpen;
    private double unfilteredPosition;
    private double columnPosition;
    private boolean initialFocusSet;
    private Element selected = Element.ARMOR;
    private Gesture gesture = Gesture.NONE;
    private boolean toolbar = true, damagePreview, dragMoved;
    private int px = 12, py = 12, pw = 188, ph, panelOriginX, panelOriginY;
    private double startX, startY, startScale, panelGrabX, panelGrabY;
    private HudBounds origin;
    private HudResize resizeGesture;
    private final HudSnap.Drag dragSnap=new HudSnap.Drag();
    private final me.xchro.neon.core.OutlineTravel outlineTravel=new me.xchro.neon.core.OutlineTravel();
    private int resizeCorner;
    private double guideX = -1, guideY = -1;
    private String error = "";
    private boolean animateRows;
    private final UiMotion colorMotion = new UiMotion(0);
    private FpsColor colorRole;
    private ColorPicker colorPicker, closingPicker;
    private String colorTitle = "";
    private int shellX, shellY, shellWidth, shellHeight, colorWidth;
    public HudEditor(Screen parent) {
        super(Ui.label("HUD editor")); this.parent = parent;
        session = parent instanceof SettingsScreen settings ? settings.session() : new SettingsSession();
        HudPanelPlacement loaded;
        try {
            String pending = Neon.SAVES.latest(placementFile);
            loaded = pending == null ? HudPanelPlacement.load(placementFile) : HudPanelPlacement.decode(pending);
        } catch (Exception error) { loaded = new HudPanelPlacement(); Neon.LOG.warn("Could not read HUD column placement", error); }
        placement = loaded;
    }
    public boolean damagePreview() { return damagePreview; }
    @Override protected void setInitialFocus() {
        // Screen rebuilds otherwise select the first row and reveal it after restoring scroll.
        if (!initialFocusSet) { initialFocusSet = true; super.setInitialFocus(); }
    }
    public boolean controlsVisible() { return toolbar && (!dragMoved || gesture != Gesture.MOVE && gesture != Gesture.RESIZE); }
    public ScreenRectangle inspectorBounds() { return new ScreenRectangle(shellX, shellY, shellWidth, shellHeight); }
    private NeonConfig.Widget widget() { return widget(selected); }
    private NeonConfig.Widget widget(Element element) {
        return switch (element) { case ARMOR -> Neon.config.armor; case TOTEMS -> Neon.config.totems; case FPS -> Neon.config.fps;
            default -> Neon.config.hud(HudKind.valueOf(element.name())).placement; };
    }
    private boolean visible(Element element) { return widget(element).visible; }
    private int hudWidth() { return Math.min(width * pixelScale(), minecraft.getWindow().getWidth()); }
    private int hudHeight() { return Math.min(height * pixelScale(), minecraft.getWindow().getHeight()); }
    private HudBounds bounds(Element element) {
        return switch (element) {
            case ARMOR -> NeonHud.pixelBounds(hudWidth(), hudHeight(), true);
            case TOTEMS -> NeonHud.totemPixelBounds(hudWidth(), hudHeight());
            case FPS -> FpsHud.bounds(hudWidth(), hudHeight());
            default -> HudWidgets.get(HudKind.valueOf(element.name())).bounds(hudWidth(),hudHeight(),true);
        };
    }
    private double hudCoordinate(double canvas) { return canvas * UI_SCALE; }
    private double detachedScale() { return Math.clamp(pixelScale() / NeonHud.density(), .5, 3); }
    private HudBounds bounds() { return bounds(selected); }
    // Ported from Unity HudEditorScreen: a root toggles on left click and folds on right click.
    private void fold(String key) { if (column != null) column.pinHeader(key); if (!expanded.remove(key)) expanded.add(key); if (key.equals("FPS") && !expanded.contains(key)) closeColor(false); animateRows = true; rebuildWidgets(); UiSounds.cue(expanded.contains(key) ? Cue.NAVIGATE : Cue.BACK); }
    private void root(List<Row> out, Element element, String name) {
        out.add(new Row(element.name(), name, 0, visible(element),
            () -> { selected = element; session.change(() -> widget().visible = !widget().visible); UiSounds.toggled(widget().visible); rebuildWidgets(); },
            () -> { selected = element; fold(element.name()); }));
    }
    private void group(List<Row> out, String key, String name, int depth) { out.add(new Row(key, name, depth, null, () -> fold(key), () -> fold(key))); }
    private void bool(List<Row> out, String key, String name, int depth, boolean value, Runnable action) {
        out.add(new Row(key, name, depth, value, () -> { session.change(action); rebuildWidgets(); UiSounds.toggled(!value); }, null));
    }
    private void range(List<Row> out, String key, String name, int depth, double value, double min, double max, double step, DoubleConsumer setter) {
        out.add(new Row(key, name, depth, new Range(value, min, max, step, setter), null, null));
    }
    private void choice(List<Row> out, String key, String name, String value, Runnable cycle) {
        out.add(new Row(key, name + ": " + value, 1, null, () -> { session.change(cycle); rebuildWidgets(); UiSounds.cue(Cue.CONFIRM); }, null));
    }
    private List<Row> makeRows() {
        List<Row> out = new ArrayList<>(); var c = Neon.config;
        for (Element e : Element.values()) {
            String name=elementName(e);
            if(!UiSearch.matches(name+" "+e.name(),query))continue;
            root(out, e, name);
            var widget = widget(e);
            if (e != Element.TOTEMS || c.totemFloating)
                range(out, e + ":Scale", "Scale", 1, widget.scale, .5, 3, .05,
                    v -> { selected = e; resizeAtTopLeft(v); });
            if (e == Element.ARMOR) {
                choice(out, "armor:layout", "Layout", c.armorVertical ? "Vertical" : "Horizontal", () -> c.armorVertical = !c.armorVertical);
                bool(out, "armor:frames", "Slot frames", 1, c.armorSlots, () -> c.armorSlots = !c.armorSlots);
                choice(out, "armor:order", "Order", c.armorReverse ? "Boots first" : "Helmet first", () -> c.armorReverse = !c.armorReverse);
                range(out, "armor:spacing", "Spacing", 1, c.armorGap, 0, 8, 1, v -> c.armorGap = (int) v);
                bool(out, "armor:warnings", "Warnings", 1, c.armorWarnings, () -> c.armorWarnings = !c.armorWarnings);
                if (c.armorWarnings) range(out, "armor:threshold", "Warn below", 1, c.armorWarningPercent, 1, 30, 1, v -> c.armorWarningPercent = (int) v);
                bool(out, "armor:preview", "Damage preview", 1, damagePreview, () -> damagePreview = !damagePreview);
            } else if (e == Element.TOTEMS) {
                choice(out, "totems:mode", "Mode", c.totemFloating ? "Floating" : "Offhand", () -> {
                    selected = Element.TOTEMS; if (c.totemFloating) c.totemFloating = false; else detachTotem();
                });
            } else if(e == Element.FPS) {
                bool(out, "fps:smooth", "Smooth FPS", 1, c.fpsAnimation, () -> c.fpsAnimation = !c.fpsAnimation);
                bool(out, "fps:background", "Background", 1, c.fpsBackground, () -> c.fpsBackground = !c.fpsBackground);
                bool(out, "fps:border", "Border", 1, c.fpsBorder, () -> c.fpsBorder = !c.fpsBorder);
                bool(out, "fps:shadow", "Shadow", 1, c.fpsShadow, () -> c.fpsShadow = !c.fpsShadow);
                for (FpsColor role : FpsColor.values()) out.add(new Row("fps:"+role.name(),role.label,1,role,null,null));
            } else {
                var appearance=c.hud(HudKind.valueOf(e.name()));
                bool(out,e+":background","Background",1,appearance.background,()->appearance.background=!appearance.background);
                bool(out,e+":border","Border",1,appearance.border,()->appearance.border=!appearance.border);
                bool(out,e+":shadow","Shadow",1,appearance.shadow,()->appearance.shadow=!appearance.shadow);
                if(e==Element.CHAT) bool(out,e+":messageBar","Message bar",1,c.betterChat.messageBar,()->c.betterChat.messageBar=!c.betterChat.messageBar);
                if(e==Element.POTIONS) bool(out,e+":vanillaIcons","Hide vanilla icons",1,c.potionHideVanillaIcons,()->c.potionHideVanillaIcons=!c.potionHideVanillaIcons);
                if(e==Element.COORDINATES) {
                    bool(out,e+":conversion","Nether conversion",1,c.coordinatesConversion,()->c.coordinatesConversion=!c.coordinatesConversion);
                }
                if(e==Element.COMPASS) bool(out,e+":degrees","Degrees",1,c.compassDegrees,()->c.compassDegrees=!c.compassDegrees);
                if(e==Element.KEYSTROKES) {
                    bool(out,e+":mouse","Mouse buttons",1,c.keystrokesMouse,()->c.keystrokesMouse=!c.keystrokesMouse);
                    bool(out,e+":jump","Jump key",1,c.keystrokesJump,()->c.keystrokesJump=!c.keystrokesJump);
                }
                if(e==Element.POTIONS) range(out,e+":rows","Maximum rows",1,c.potionMaxRows,1,12,1,v->c.potionMaxRows=(int)v);
                if(e==Element.LIGHT) bool(out,e+":sky","Skylight",1,c.lightSky,()->c.lightSky=!c.lightSky);
                if(e==Element.WORLD_TIME) bool(out,e+":day","Day count",1,c.worldTimeDay,()->c.worldTimeDay=!c.worldTimeDay);
                if(e==Element.ITEM_COUNTER) {
                    choice(out,e+":item","Item",c.counterItem.label,()->c.counterItem=NeonConfig.CounterItem.values()[(c.counterItem.ordinal()+1)%NeonConfig.CounterItem.values().length]);
                    bool(out,e+":offhand","Include offhand",1,c.counterOffhand,()->c.counterOffhand=!c.counterOffhand);
                }
                if(e==Element.SCOREBOARD) {
                    bool(out,e+":title","Title",1,c.scoreboardTitle,()->c.scoreboardTitle=!c.scoreboardTitle);
                    bool(out,e+":scores","Scores",1,c.scoreboardScores,()->c.scoreboardScores=!c.scoreboardScores);
                    range(out,e+":rows","Maximum rows",1,c.scoreboardRows,1,15,1,v->c.scoreboardRows=(int)v);
                }
                if(e==Element.PACKS) range(out,e+":rows","Maximum rows",1,c.packMaxRows,1,5,1,v->c.packMaxRows=(int)v);
                if(e==Element.SERVER) {
                    bool(out,e+":address","Address",1,c.serverAddress,()->c.serverAddress=!c.serverAddress);
                    bool(out,e+":players","Player count",1,c.serverPlayers,()->c.serverPlayers=!c.serverPlayers);
                }
                if(e==Element.INVENTORY) {
                    bool(out,e+":slots","Slot backgrounds",1,c.inventorySlots,()->c.inventorySlots=!c.inventorySlots);
                    bool(out,e+":hotbar","Include hotbar",1,c.inventoryHotbar,()->c.inventoryHotbar=!c.inventoryHotbar);
                }
                if(e==Element.HELD_ITEM) {
                    bool(out,e+":offhand","Include offhand",1,c.heldItemOffhand,()->c.heldItemOffhand=!c.heldItemOffhand);
                    bool(out,e+":percent","Percentage",1,c.heldItemPercent,()->c.heldItemPercent=!c.heldItemPercent);
                }
                if(e==Element.CLOCK) {
                    bool(out,e+":hours","24-hour time",1,c.clock24Hour,()->c.clock24Hour=!c.clock24Hour);
                    bool(out,e+":seconds","Seconds",1,c.clockSeconds,()->c.clockSeconds=!c.clockSeconds);
                }
            }
        }
        if(!UiSearch.matches("General",query))return out;
        group(out, "general", "General", 0);
        {
            range(out, "general:scale", "HUD scale", 1, c.hudScale, .5, 2, .05, v -> c.hudScale = v);
            choice(out,"general:font","Font",c.hudFont==NeonConfig.HudFont.INTER?"Client":"Minecraft",()->c.hudFont=c.hudFont==NeonConfig.HudFont.INTER?NeonConfig.HudFont.MINECRAFT:NeonConfig.HudFont.INTER);
            bool(out, "general:surfaces", "HUD surfaces", 1, c.hudSurfaces, () -> c.hudSurfaces = !c.hudSurfaces);
            bool(out, "general:backgrounds", "Backgrounds", 1, c.hudBackgrounds, () -> c.hudBackgrounds = !c.hudBackgrounds);
            bool(out, "general:outlines", "Outlines", 1, c.hudOutlines, () -> c.hudOutlines = !c.hudOutlines);
            bool(out, "general:panels", "Equipment panels", 1, c.hudNeonPanels, () -> c.hudNeonPanels = !c.hudNeonPanels);
            bool(out, "general:outline", "Animated outline", 1, c.hudAnimatedOutline, () -> c.hudAnimatedOutline = !c.hudAnimatedOutline);
            bool(out, "general:guides", "Guides", 1, c.editorGuides, () -> c.editorGuides = !c.editorGuides);
            bool(out, "general:snap", "Snap", 1, c.editorSnap, () -> c.editorSnap = !c.editorSnap);
            range(out, "general:sensitivity", "Snap sensitivity", 1, c.snapSensitivity, 1, 24, 1, v -> c.snapSensitivity = (int) v);
        }
        return out;
    }
    @Override protected void init() {
        boolean searchFocused=searchOpen&&(refocusSearch||search!=null&&search.isFocused());refocusSearch=false;
        HudWidgets.invalidate();
        finishPointerGesture();
        WidgetMemory memory = new WidgetMemory(column);
        double position = filtering?(query.isEmpty()?unfilteredPosition:0):column == null ? columnPosition : column.scrollAmount(); columnPosition=position; column = null;
        session.end(); gesture = Gesture.NONE; resizeGesture = null; refreshers.clear(); inspector.clear(); guideX = guideY = -1;
        if (!toolbar) return;
        pw = Math.min(188, canvasWidth() - 16);
        if (viewportWidth != canvasWidth() || viewportHeight != canvasHeight()) {
            viewportWidth = canvasWidth(); viewportHeight = canvasHeight();
            var positionInView = placement.fit(viewportWidth, viewportHeight, pw, COLLAPSED_HEIGHT);
            px = positionInView.x(); py = positionInView.y();
        }
        px = Math.clamp(px, 8, canvasWidth() - pw - 8);
        py = Math.clamp(py, 8, Math.max(8, canvasHeight() - COLLAPSED_HEIGHT));
        LinearLayout rows = LinearLayout.vertical().spacing(0);
        List<Row> model = makeRows();
        if(model.isEmpty())rows.addChild(new UiLabel("No matching HUD elements",pw-12,26,Ui.MUTED));
        for (int i = 0; i < model.size();) {
            Row root = model.get(i++);
            rows.addChild(control(root));
            LinearLayout content = LinearLayout.vertical().spacing(0);
            while (i < model.size() && model.get(i).depth > 0) content.addChild(control(model.get(i++)));
            UiModuleFold previous = folds.get(root.key);
            if (expanded.contains(root.key) || previous != null) {
                var fold = new UiModuleFold(root.label, pw - 2, content, previous, expanded.contains(root.key), animateRows, this::layoutColumn, true);
                folds.put(root.key, fold); rows.addChild(fold);
            }
        }
        rows.arrangeElements();
        ph = Math.min(columnTop()+1 + rows.getHeight(), Math.min(300,canvasHeight() - py - 8));
        column = addInspector(new UiScroll(px + 1, py + columnTop(), pw - 2, Math.max(22, ph-columnTop()-1), rows));
        if (animateRows) column.restorePosition(position); else column.setScrollAmount(position);
        memory.restore(column,false,true,!filtering);animateRows=false;
        if(filtering)column.setFocused((GuiEventListener)null);
        filtering=false;
        if (column.getFocused() != null) setFocused(column);
        if(search==null)search=new UiSearch("Search HUD",query,this::filter);
        if(searchOpen)addInspector(search);
        if(searchFocused)setFocused(search);
        if (colorPicker != null) {
            FpsColor role=colorRole;
            colorPicker.bind(() -> role.color(Neon.config),(value,rgb) -> role.color(Neon.config,value));
            addWidget(colorPicker);
        }
        layoutColor();
        refresh();
    }
    private static String elementName(Element element){return switch(element){case ARMOR->"Armor";case TOTEMS->"Totems";case FPS->"FPS";default->HudKind.valueOf(element.name()).label;};}
    private void filter(String value){
        if(query.equals(value))return;
        if(query.isEmpty()&&column!=null)unfilteredPosition=column.scrollAmount();
        query=value;filtering=refocusSearch=true;
        if (colorPicker != null && !UiSearch.matches("FPS", query)) closeColor(false);
        rebuildWidgets();
    }
    private void revealSelected(){
        if(search!=null&&!UiSearch.matches(elementName(selected)+" "+selected.name(),query)){
            search.setValue("");refocusSearch=false;setFocused((GuiEventListener)null);
        }
    }
    private AbstractWidget control(Row row) {
        if (row.value instanceof FpsColor role) return new ColorRow(role.label,pw-2,() -> role.color(Neon.config),
            () -> openColor(role),() -> colorRole == role).hudRow().identity(row.key);
        if (row.value instanceof Range r) return new ExactSlider(this, session, row.label, pw - 2, r.value, r.min, r.max, r.step,
            n -> NumberValue.format(n) + (row.label.endsWith("scale") || row.label.equals("Scale") ? "x" : row.label.equals("Warn below") ? "%" : row.label.equals("Snap sensitivity") ? "px" : ""), r.setter).inlineRow().identity(row.key);
        return new HudEntry(row.label, pw - 2, () -> Boolean.TRUE.equals(row.value),
            () -> expanded.contains(row.key), row.left == null ? () -> {} : row.left, row.right).depth(row.depth).toggleRow(row.value instanceof Boolean).identity(row.key);
    }
    private int columnTop(){return COLUMN_TOP+(searchOpen?23:0);}
    private void layoutColumn() {
        if (column == null) return;
        column.relayout();
        ph = Math.min(columnTop()+1 + column.contentHeight(), Math.min(300,canvasHeight() - py - 8));
        column.setHeight(Math.max(22, ph-columnTop()-1));
        column.relayout();
        layoutColor();
    }
    private void openColor(FpsColor role) {
        if (colorRole == role) { closeColor(true); return; }
        if (colorPicker != null) { colorPicker.finish(); removeWidget(colorPicker); }
        closingPicker = null; colorRole = role; colorTitle = role.label;
        colorPicker = new ColorPicker(role.label,role.color(Neon.config),session,() -> role.color(Neon.config),
            (value,rgb) -> role.color(Neon.config,value)).independent();
        colorPicker.sync(() -> role.synced(Neon.config),() -> role.sync(Neon.config));
        addWidget(colorPicker); colorMotion.to(1,System.nanoTime(),ExpandingMenu.DURATION); layoutColor();
        UiSounds.cue(Cue.NAVIGATE);
    }
    private void closeColor(boolean sound) {
        if (colorPicker == null) return;
        finishPointerGesture(); colorPicker.finish(); removeWidget(colorPicker);
        if (getFocused() == colorPicker) setFocused(column);
        closingPicker = colorPicker; closingPicker.active = false; colorPicker = null; colorRole = null;
        colorMotion.to(0,System.nanoTime(),ExpandingMenu.DURATION); layoutColor();
        if (sound) UiSounds.cue(Cue.BACK);
    }
    private void layoutColor() {
        double reveal = colorMotion.value(System.nanoTime(),ExpandingMenu.DURATION);
        colorWidth = (int)Math.round(ExpandingMenu.COLOR_WIDTH * reveal);
        shellWidth = pw + colorWidth;
        shellHeight = ph + (int)Math.round(Math.max(0,Math.min(260,canvasHeight()-16)-ph)*reveal);
        shellX = Math.clamp(px,8,Math.max(8,canvasWidth()-shellWidth-8));
        shellY = Math.clamp(py,8,Math.max(8,canvasHeight()-shellHeight-8));
        if (column != null) { column.setX(shellX+1); column.setY(shellY+columnTop()); }
        if(search!=null){search.setX(shellX+9);search.setY(shellY+28);search.setWidth(pw-18);}
        var picker = colorPicker != null ? colorPicker : closingPicker;
        if (picker != null) picker.layout(shellX+pw+12,shellY+22,ExpandingMenu.COLOR_WIDTH-24,
            Math.min(230,Math.max(110,shellHeight-30)),Math.max(0,colorWidth-24));
        if (colorPicker == null && colorWidth == 0) closingPicker = null;
    }
    private <T extends AbstractWidget> T addInspector(T widget) { inspector.add(widget); return addRenderableWidget(widget); }
    private void detachTotem() {
        if (selected != Element.TOTEMS || Neon.config.totemFloating) return;
        HudBounds before = bounds(); Neon.config.totemFloating = true; widget().scale = detachedScale();
        before.move(widget(), before.x(), before.y(), hudWidth(), hudHeight());
    }
    private void resizeAtTopLeft(double scale) {
        if (selected == Element.TOTEMS && !Neon.config.totemFloating) return;
        HudBounds before = bounds(); widget().scale = scale; move(before.x(), before.y(), false);
    }
    private void refresh() { refreshers.forEach(Runnable::run); }
    private ExactSlider editingSlider() {
        return activeNumber();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void pixelBackground(GuiGraphicsExtractor g, int x, int y, float delta) {}
    @Override protected void renderPixels(GuiGraphicsExtractor g, int mx, int my, float delta) {
        layoutColor();
        boolean folding = false;
        for (var fold : folds.values()) folding |= fold.tick();
        if (folding) layoutColumn();
        // Gameplay and editor share physical-pixel geometry; attached totems follow the real offhand slot.
        g.pose().pushMatrix(); g.pose().scale(1 / UI_SCALE);
        NeonHud.drawPixels(g, true, hudWidth(), hudHeight());
        if (Neon.config.totems.visible && !Neon.config.totemFloating && (minecraft.player == null || !minecraft.player.getOffhandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING))) NeonHud.drawTotemPixels(g, true, hudWidth(), hudHeight());
        g.pose().popMatrix();
        boolean moving = dragMoved&&(gesture == Gesture.MOVE || gesture == Gesture.RESIZE);
        if (moving && Neon.config.editorGuides) {
            g.pose().pushMatrix(); g.pose().scale(1 / UI_SCALE);
            Ui.rect(g, hudWidth() / 2, 0, 1, hudHeight(), 0x30ffffff);
            Ui.rect(g, 0, hudHeight() / 2, hudWidth(), 1, 0x30ffffff);
            if (guideX >= 0) Ui.rect(g, Math.min(hudWidth()-1, (int) Math.round(guideX)), 0, 1, hudHeight(), Ui.BLUE);
            if (guideY >= 0) Ui.rect(g, 0, Math.min(hudHeight()-1, (int) Math.round(guideY)), hudWidth(), 1, Ui.BLUE);
            g.pose().popMatrix();
        }
        if (controlsVisible()) {
            RoundedUi.panel(g,shellX,shellY,shellWidth,shellHeight,-1);
            UiEffects.outline(g,shellX,shellY,shellWidth,shellHeight,outlineTravel);
            Ui.title(g,"HUD editor",shellX+10,shellY+5);
            refresh(); renderWidgets(g, mx, my, delta);
            var picker = colorPicker != null ? colorPicker : closingPicker;
            if (picker != null && colorWidth > 1) {
                Ui.clip(g,shellX+pw,shellY+1,shellX+shellWidth-1,shellY+shellHeight-1);
                Ui.title(g,colorTitle,shellX+pw+12,shellY+5);
                picker.extractRenderState(g,colorPicker == null ? -10000 : mx,colorPicker == null ? -10000 : my,delta);
                g.disableScissor();
            }
        }
        if (!error.isEmpty()) Ui.small(g, error, 12, 12, Ui.RED);
    }
    private HudBounds handle(HudBounds b, int corner) {
        int size = (int) Math.ceil(hudCoordinate(7));
        int x = (corner % 2 == 0 ? b.x() : b.x() + b.width()) - size / 2;
        int y = (corner < 2 ? b.y() : b.y() + b.height()) - size / 2;
        return new HudBounds(Math.clamp(x, 0, hudWidth() - size), Math.clamp(y, 0, hudHeight() - size), size, size);
    }
    @Override protected void cursor(GuiGraphicsExtractor g,int mx,int my) {
        super.cursor(g,mx,my);
        if(gesture==Gesture.RESIZE){g.requestCursor(UiCursors.corner(resizeCorner));return;}
        if(gesture==Gesture.MOVE||gesture==Gesture.PANEL){g.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_ALL);return;}
        if(hasPointerGesture())return;
        if(controlsVisible()&&mx>=shellX&&mx<=shellX+shellWidth&&my>=shellY&&my<=shellY+shellHeight){
            if(my<shellY+22)g.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_ALL);
            return;
        }
        if (colorPicker != null) return;
        CanvasHit hit = canvasHit(hudCoordinate(mx), hudCoordinate(my));
        if (hit != null) g.requestCursor(hit.corner() >= 0 ? UiCursors.corner(hit.corner()) : com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_ALL);
    }
    private CanvasHit canvasHit(double x, double y) {
        CanvasHit hit = hit(selected, x, y);
        if (hit != null) return hit;
        for (Element element : CANVAS_ORDER) {
            if (element == selected) continue;
            hit = hit(element, x, y);
            if (hit != null) return hit;
        }
        return null;
    }
    private CanvasHit hit(Element element, double x, double y) {
        if (!visible(element)) return null;
        HudBounds b = bounds(element);
        if (element != Element.TOTEMS || Neon.config.totemFloating)
            for (int corner = 0; corner < 4; corner++)
                if (handle(b, corner).contains(x, y)) return new CanvasHit(element, corner);
        return b.contains(x, y) ? new CanvasHit(element, -1) : null;
    }
    private void move(double x, double y, boolean snap) {
        detachTotem();
        HudBounds b = bounds(); guideX = guideY = -1;
        if (snap) {
            var neighbors = Arrays.stream(Element.values()).filter(e -> e != selected && visible(e)).map(this::bounds).toList();
            var snapped = dragSnap.move(b, neighbors, hudWidth(), hudHeight(), x, y, Neon.config.snapSensitivity,Neon.config.editorSnap);
            x = snapped.x(); y = snapped.y(); guideX = snapped.guideX(); guideY = snapped.guideY();
        } else dragSnap.clear();
        b.move(widget(), x, y, hudWidth(), hudHeight());
        if (selected == Element.ARMOR) Neon.config.armorAnchor = ArmorAnchor.CUSTOM;
    }
    @Override protected boolean pixelMouseClicked(MouseButtonEvent e, boolean twice) {
        boolean header = toolbar && e.button() == 0 && e.x() >= shellX && e.x() <= shellX + shellWidth && e.y() >= shellY && e.y() < shellY + 22;
        if (colorPicker != null) {
            if (colorPicker.isMouseOver(e.x(),e.y())) {
                colorPicker.mouseClicked(e,twice); setFocused(colorPicker); setDragging(e.button()==0);
                if (e.button()==0 && colorPicker.draggingPointer()) capturePointer(colorPicker,e);
                return true;
            }
            if (!header && !ColorRow.underPointer(column, e)) {
                closeColor(true);
                return true;
            }
        }
        ExactSlider editing = editingSlider();
        if (editing != null && !editing.isMouseOver(e.x(), e.y()) && !editing.finishInline(true)) return true;
        if (header) {
            if (colorPicker != null) colorPicker.finish();
            setFocused((GuiEventListener)null);
            gesture = Gesture.PANEL; panelOriginX = px; panelOriginY = py; px=shellX;py=shellY;panelGrabX = e.x() - px; panelGrabY = e.y() - py; return true;
        }
        if (super.pixelMouseClicked(e, twice)) return true;
        if (toolbar && e.x() >= shellX && e.x() <= shellX + shellWidth && e.y() >= shellY && e.y() <= shellY + shellHeight) return true;
        if (e.button() != 0 && e.button() != 1) return false;
        e = new MouseButtonEvent(hudCoordinate(e.x()), hudCoordinate(e.y()), e.buttonInfo());
        if (e.button() == 0) setFocused((GuiEventListener) null);
        CanvasHit hit = canvasHit(e.x(), e.y());
        if (hit == null) return false;
        selected = hit.element();
        if (e.button() == 1) {
            revealSelected();animateRows = expanded.add(selected.name());
            boolean opened = animateRows || !toolbar; toolbar = true; rebuildWidgets();
            if (opened) UiSounds.cue(Cue.NAVIGATE);
            column.children().stream().filter(w -> w instanceof HudEntry entry && entry.identity().equals(selected.name()))
                .findFirst().ifPresent(w -> column.reveal(w.getY(), w.getBottom() + 22));
        } else if (hit.corner() >= 0) {
            start(e, Gesture.RESIZE);
            resizeCorner = hit.corner();
            resizeGesture = new HudResize(widget(), origin, resizeCorner, startScale);
        } else {
            start(e, Gesture.MOVE);
        }
        return true;
    }
    private void start(MouseButtonEvent e, Gesture type) {
        dragSnap.clear();dragMoved=false;
        session.begin(); gesture = type; origin = bounds(); startX = e.x(); startY = e.y();
        startScale = selected == Element.TOTEMS && !Neon.config.totemFloating ? detachedScale() : widget().scale;
    }
    @Override protected boolean pixelMouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (gesture == Gesture.PANEL) {
            int nx = Math.clamp((int) (e.x() - panelGrabX), 8, Math.max(8,canvasWidth() - shellWidth - 8));
            int ny = Math.clamp((int) (e.y() - panelGrabY), 8, Math.max(8, canvasHeight() - shellHeight - 8));
            int ox = nx - px, oy = ny - py; px = nx; py = ny;
            for (AbstractWidget widget : inspector) { widget.setX(widget.getX() + ox); widget.setY(widget.getY() + oy); } layoutColor(); return true;
        }
        if (gesture != Gesture.MOVE && gesture != Gesture.RESIZE) return super.pixelMouseDragged(e, dx, dy);
        e = new MouseButtonEvent(hudCoordinate(e.x()), hudCoordinate(e.y()), e.buttonInfo());
        if(!dragMoved&&Math.hypot(e.x()-startX,e.y()-startY)<=3)return true;
        dragMoved=true;
        if (gesture == Gesture.MOVE) { move(origin.x() + e.x() - startX, origin.y() + e.y() - startY, (e.modifiers() & GLFW.GLFW_MOD_ALT) == 0); refresh(); return true; }
        if (gesture == Gesture.RESIZE) {
            detachTotem();
            resizeGesture.drag(e.x() - startX, e.y() - startY, hudWidth(), hudHeight());
            if (selected == Element.ARMOR) Neon.config.armorAnchor = ArmorAnchor.CUSTOM;
            resizeGesture.place(bounds(), hudWidth(), hudHeight());
            refresh(); return true;
        }
        return false;
    }
    @Override protected boolean pixelMouseReleased(MouseButtonEvent e) {
        if (gesture != Gesture.NONE && e.button() != 0) return true;
        if (gesture == Gesture.PANEL) {
            placement.capture(px, py, canvasWidth(), canvasHeight()); placement.queue(placementFile, Neon.SAVES);
        }
        boolean open=(gesture==Gesture.MOVE||gesture==Gesture.RESIZE)&&!dragMoved;
        boolean handled = gesture != Gesture.NONE; gesture = Gesture.NONE; guideX = guideY = -1; session.end();
        boolean control = super.pixelMouseReleased(e);
        if(open){revealSelected();boolean expandedNow=expanded.add(selected.name());animateRows=expandedNow;boolean show=expandedNow||!toolbar;toolbar=true;if(show)UiSounds.cue(Cue.NAVIGATE);}
        if (handled) rebuildWidgets();
        if(open)column.children().stream().filter(w->w instanceof HudEntry entry&&entry.identity().equals(selected.name())).findFirst().ifPresent(w->column.reveal(w.getY(),w.getBottom()+22));
        return handled || control;
    }
    @Override protected boolean pixelMouseScrolled(double x, double y, double dx, double dy) {
        if (colorPicker != null && colorPicker.isMouseOver(x,y)) return true;
        if (toolbar && x >= shellX && x <= shellX + shellWidth && y >= shellY && y <= shellY + shellHeight) { super.pixelMouseScrolled(x, y, dx, dy); return true; }
        if (visible(selected) && bounds().contains(hudCoordinate(x), hudCoordinate(y)) && dy != 0) {
            if (selected == Element.TOTEMS && !Neon.config.totemFloating) return true;
            double current = widget().scale;
            session.change(() -> resizeAtTopLeft(Math.clamp(current + Math.signum(dy) * .05, .5, 3))); rebuildWidgets(); return true;
        }
        return super.pixelMouseScrolled(x, y, dx, dy);
    }
    @Override protected boolean pixelKeyPressed(KeyEvent e) {
        if (cancelNumberDrag(e)) return true;
        if (colorPicker != null && colorPicker.editingHex() && (e.key()==GLFW.GLFW_KEY_ESCAPE || e.key()==GLFW.GLFW_KEY_ENTER || e.key()==GLFW.GLFW_KEY_KP_ENTER)) return colorPicker.keyPressed(e);
        ExactSlider editing = editingSlider();
        if (editing != null) return editing.keyPressed(e);
        if (e.key() == GLFW.GLFW_KEY_ESCAPE && gesture != Gesture.NONE) {
            if (gesture == Gesture.PANEL) { px = panelOriginX; py = panelOriginY; }
            else session.cancel();
            rebuildWidgets(); return true;
        }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE && hasPointerGesture()) { finishPointerGesture(); return true; }
        if(e.key()==GLFW.GLFW_KEY_F&&(e.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0&&(colorPicker==null||!colorPicker.editingHex())){
            if(!toolbar||!searchOpen){toolbar=searchOpen=true;rebuildWidgets();}setFocused(search);search.moveCursorToEnd(false);search.setHighlightPos(0);return true;
        }
        if (e.key() == GLFW.GLFW_KEY_ESCAPE && toolbar && searchOpen) {
            if (!query.isEmpty()) search.setValue("");
            else { searchOpen=false;setFocused((GuiEventListener)null);rebuildWidgets(); }
            return true;
        }
        if(search!=null&&search.isFocused()){
            if(e.key()==GLFW.GLFW_KEY_ENTER||e.key()==GLFW.GLFW_KEY_KP_ENTER||e.key()==GLFW.GLFW_KEY_DOWN){if(UiSearch.focusFirst(column))setFocused(column);return true;}
            // H, Tab, arrows, and the menu keys belong to the focused editor first.
            if(e.key()==GLFW.GLFW_KEY_Z&&(e.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0)return true;
            return super.pixelKeyPressed(e);
        }
        if (e.key() == GLFW.GLFW_KEY_Z && (e.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) { session.undo(); rebuildWidgets(); return true; }
        if (colorPicker != null && e.key()==GLFW.GLFW_KEY_ESCAPE) { closeColor(true); return true; }
        if (colorPicker != null && (colorPicker.editingHex() || e.key()==GLFW.GLFW_KEY_TAB)) { colorMotion.snap(1);layoutColor();return super.pixelKeyPressed(e); }
        if (e.key() == GLFW.GLFW_KEY_H || e.key() == GLFW.GLFW_KEY_TAB) { closeColor(false); toolbar = !toolbar; rebuildWidgets(); return true; }
        if (getFocused() != null || !visible(selected)) {
            var before = colorPicker; boolean handled = super.pixelKeyPressed(e);
            if (colorPicker != null && (before != colorPicker || getFocused() == colorPicker)) {
                if (before != colorPicker) {setFocused(colorPicker);colorPicker.setFocused(colorPicker.children().getFirst());}
                colorMotion.snap(1);layoutColor();
            }
            return handled;
        }
        int step = (e.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1; HudBounds b = bounds();
        switch (e.key()) {
            case GLFW.GLFW_KEY_LEFT -> session.change(() -> move(b.x() - step, b.y(), false));
            case GLFW.GLFW_KEY_RIGHT -> session.change(() -> move(b.x() + step, b.y(), false));
            case GLFW.GLFW_KEY_UP -> session.change(() -> move(b.x(), b.y() - step, false));
            case GLFW.GLFW_KEY_DOWN -> session.change(() -> move(b.x(), b.y() + step, false));
            default -> { return super.pixelKeyPressed(e); }
        }
        refresh(); return true;
    }
    @Override public void onClose() { session.end(); if (Neon.save()) { minecraft.gui.setScreen(parent); UiSounds.cue(Cue.BACK); } else error = "Could not save settings. Check the game log."; }
    @Override protected boolean soundControlsVisible() { return controlsVisible() && !hasPointerGesture(); }
    @Override protected boolean hasPointerGesture() { return super.hasPointerGesture() || gesture != Gesture.NONE; }
    @Override protected void finishPointerGesture() {
        super.finishPointerGesture();
        if (gesture == Gesture.PANEL) { placement.capture(px, py, canvasWidth(), canvasHeight()); placement.queue(placementFile, Neon.SAVES); }
        gesture = Gesture.NONE; resizeGesture = null; guideX = guideY = -1; session.end();
    }
    @Override public void removed() { super.removed(); if(colorPicker!=null)colorPicker.finish(); colorPicker=closingPicker=null;colorRole=null;colorMotion.snap(0);session.end(); gesture = Gesture.NONE; Neon.flushSaves(); }
}
