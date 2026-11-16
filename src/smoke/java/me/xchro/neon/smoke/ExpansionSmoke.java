package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import org.lwjgl.glfw.GLFW;

/** Every added HUD runs through real placement, Undo, persistence and small-display checks. */
final class ExpansionSmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private int checks,age,index,viewport;
    private HudEditor editor;
    private final Map<Integer,net.minecraft.world.item.ItemStack> inventoryBefore=new HashMap<>();
    private net.minecraft.world.scores.Objective scoreboardFixture;
    private net.minecraft.world.scores.PlayerTeam scoreboardTeam;
    private final Map<net.minecraft.world.scores.DisplaySlot,net.minecraft.world.scores.Objective> scoreboardBefore=new EnumMap<>(net.minecraft.world.scores.DisplaySlot.class);
    ExpansionSmoke(Minecraft mc,int checks) { this.mc=mc;this.checks=checks;original=ConfigStore.copy(Neon.config); }
    int checks() { return checks; }
    private void check(boolean value,String name) {
        if(!value)throw new AssertionError("Expansion "+index+"/"+viewport+": "+name);
        checks++;System.out.println("NEON_CHECK "+name);
    }
    private List<AbstractWidget> widgets(ContainerEventHandler root) {
        var list=new ArrayList<AbstractWidget>();
        for(var child:root.children()) { if(child instanceof AbstractWidget w)list.add(w);if(child instanceof ContainerEventHandler group)list.addAll(widgets(group)); }
        return list;
    }
    private AbstractWidget named(String label) { return widgets(editor).stream().filter(w->w.getMessage().getString().equals(label)).findFirst().orElseThrow(); }
    private MouseButtonEvent event(double x,double y,int button) { return new MouseButtonEvent(x/editor.pixelScale(),y/editor.pixelScale(),new MouseButtonInfo(button,0)); }
    private void clickRow(String label,int button) {
        var row=named(label);
        var scroll=widgets(editor).stream().filter(w->w instanceof AbstractScrollArea).findFirst().orElseThrow();
        mc.setLastInputType(InputType.KEYBOARD_TAB);
        ((ContainerEventHandler)scroll).setFocused(row);
        check(row.getY()>=scroll.getY() && row.getBottom()<=scroll.getBottom(),"keyboard reveals full HUD row: "+label);
        mc.setLastInputType(InputType.MOUSE);
        var e=new MouseButtonEvent(editor.guiCoordinate(row.getX()+12),editor.guiCoordinate(row.getY()+10),new MouseButtonInfo(button,0));
        check(editor.mouseClicked(e,false),"HUD row receives pointer: "+label);editor.mouseReleased(e);
    }
    private List<String> content(HudKind kind,boolean preview) throws Exception {
        var card=HudWidgets.get(kind);card.invalidate();card.hasContent(preview);
        var field=HudCard.class.getDeclaredField("lines");field.setAccessible(true);
        return List.of((String[])field.get(card));
    }
    private void seedSlot(int slot,net.minecraft.world.item.ItemStack value) {
        inventoryBefore.put(slot,mc.player.getInventory().getItem(slot).copy());mc.player.getInventory().setItem(slot,value);
    }
    private void scoreboardFixture() {
        var board=mc.level.getScoreboard();var sidebar=net.minecraft.world.scores.DisplaySlot.SIDEBAR;
        scoreboardBefore.put(sidebar,board.getDisplayObjective(sidebar));
        var team=board.getPlayersTeam(mc.player.getScoreboardName());
        if(team!=null&&team.getColor().isPresent()) {
            var slot=team.getColor().get().displaySlot();scoreboardBefore.put(slot,board.getDisplayObjective(slot));
        }
        scoreboardFixture=board.addObjective("neon_hud_test",net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,net.minecraft.network.chat.Component.literal("Practice"),net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER,false,null);
        scoreboardBefore.keySet().forEach(slot->board.setDisplayObjective(slot,scoreboardFixture));
        var alpha=board.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("__neon_alpha"),scoreboardFixture);
        alpha.set(9);alpha.display(net.minecraft.network.chat.Component.literal("Alpha"));alpha.numberFormatOverride(new net.minecraft.network.chat.numbers.FixedFormat(net.minecraft.network.chat.Component.literal("nine")));
        var beta=board.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("__neon_beta"),scoreboardFixture);beta.set(2);beta.display(net.minecraft.network.chat.Component.literal("Beta"));
        board.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("#neon_hidden"),scoreboardFixture).set(99);
        scoreboardTeam=board.addPlayerTeam("neon_hud_test");scoreboardTeam.setPlayerPrefix(net.minecraft.network.chat.Component.literal("[A] ").withStyle(net.minecraft.ChatFormatting.AQUA));board.addPlayerToTeam("__neon_alpha",scoreboardTeam);
    }
    private int vanillaScoreboardText() throws Exception {
        var state=new net.minecraft.client.renderer.state.gui.GuiRenderState();var g=new net.minecraft.client.gui.GuiGraphicsExtractor(mc,state,0,0);
        var method=net.minecraft.client.gui.Hud.class.getDeclaredMethod("extractScoreboardSidebar",g.getClass(),net.minecraft.client.DeltaTracker.class);method.setAccessible(true);method.invoke(mc.gui.hud,g,null);
        int[] count={0};state.forEachText(text->count[0]++);return count[0];
    }
    private void dataChecks(HudKind kind) throws Exception {
        var text=content(kind,false);
        if(kind==HudKind.COORDINATES) {
            var typography=new HudTypography();typography.scale(2);
            check(typography.width("§aNeon")==typography.width("Neon"),"legacy formatting codes have no HUD advance");
            for(var style:List.of(net.minecraft.network.chat.Style.EMPTY.withObfuscated(true),net.minecraft.network.chat.Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.withDefaultNamespace("uniform"))))) {
                var state=new net.minecraft.client.renderer.state.gui.GuiRenderState();var g=new net.minecraft.client.gui.GuiGraphicsExtractor(mc,state,0,0);
                typography.draw(g,net.minecraft.network.chat.Component.literal("Native glyphs").withStyle(style).getVisualOrderText(),0,0,0xffffffff,true);
                int[] nativeRuns={0};state.forEachText(run->nativeRuns[0]++);
                check(nativeRuns[0]==1,"custom font and obfuscated HUD text retain the native renderer");
            }
        }
        switch(kind) {
            case COORDINATES -> {
                check(text.getFirst().equals("XYZ  "+mc.player.blockPosition().getX()+", "+mc.player.blockPosition().getY()+", "+mc.player.blockPosition().getZ()),"Unity coordinate text matches actual player block");
                Neon.config.coordinatesConversion=true;check(content(kind,false).getFirst().endsWith(" ("+HudNumbers.coordinate(mc.player.getX(),.125)+", "+HudNumbers.coordinate(mc.player.getZ(),.125)+")"),"Overworld offers inline Unity Nether conversion");
                Neon.config.coordinatesConversion=false;
            }
            case COMPASS -> {
                float yaw=mc.player.getYRot();mc.player.setYRot(180);
                check(content(kind,false).getFirst().startsWith("N  0°"),"compass reports north across yaw wrap");mc.player.setYRot(yaw);
            }
            case PING -> check(text.equals(List.of("PING  Local")),"singleplayer reports local rather than invented latency");
            case CPS -> {
                me.xchro.neon.input.InputTelemetry.tick(mc);me.xchro.neon.input.InputTelemetry.press(0);
                check(me.xchro.neon.input.InputTelemetry.left()==0,"menu clicks stay out of CPS");
                mc.gui.setScreen(null);check(Neon.ready(mc),"CPS fixture is real gameplay");
                me.xchro.neon.input.InputTelemetry.press(0);me.xchro.neon.input.InputTelemetry.press(0);me.xchro.neon.input.InputTelemetry.press(1);
                check(content(kind,false).getFirst().equals("CPS  2 L  /  1 R"),"CPS separates physical left and right events");
                mc.gui.setScreen(editor);me.xchro.neon.input.InputTelemetry.tick(mc);
            }
            case KEYSTROKES -> {
                int height=HudWidgets.get(kind).bounds(1280,800,true).height();
                Neon.config.keystrokesMouse=false;Neon.config.keystrokesJump=false;
                check(HudWidgets.get(kind).bounds(1280,800,true).height()<height,"keystroke optional rows change actual canvas geometry");
                Neon.config.keystrokesMouse=Neon.config.keystrokesJump=true;
            }
            case POTIONS -> {
                var originals=mc.player.getActiveEffects().stream().map(net.minecraft.world.effect.MobEffectInstance::new).toList();
                // removeAllEffects is server-authoritative; these are disposable client-only fixtures.
                mc.player.getActiveEffectsMap().clear();check(content(kind,false).isEmpty(),"no potion placeholder leaks into gameplay");
                check(!content(kind,true).isEmpty(),"empty potion editor has a useful sample");
                mc.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED,1800,1));
                check(content(kind,false).stream().anyMatch(line->line.equals("Speed 2")),"real effect name and amplifier are displayed");
                mc.player.getActiveEffectsMap().clear();originals.forEach(mc.player::addEffect);
            }
            case HELD_ITEM -> check(text.stream().anyMatch(line->line.equals("Diamond Pickaxe")),"held item uses the actual equipped stack name");
            case CLOCK -> {
                Neon.config.clockSeconds=true;check(content(kind,false).getFirst().matches("CLOCK  [0-9]{2}:[0-9]{2}:[0-9]{2}"),"clock includes requested seconds");
                Neon.config.clock24Hour=false;check(content(kind,false).getFirst().matches("CLOCK  [0-9]{1,2}:[0-9]{2}:[0-9]{2} [AP]M"),"clock supports twelve-hour time");
                Neon.config.clock24Hour=true;Neon.config.clockSeconds=false;
            }
            case SESSION -> check(HudWidgets.SESSION.seconds(System.nanoTime())>0,"session started at connection before HUD activation");
            case BIOME -> check(text.size()==2&&!text.get(1).equals("Unknown"),"biome resolves from a loaded world registry");
            case LIGHT -> {
                check(text.size()==2&&text.getFirst().matches("LIGHT  Block (?:[0-9]|1[0-5])"),"light reads bounded world light data");
                Neon.config.lightSky=false;check(content(kind,false).size()==1,"skylight option removes its row");Neon.config.lightSky=true;
            }
            case PACKS -> check(text.getFirst().equals("RESOURCE PACKS")&&text.size()>=2,"pack display resolves active repository or explicit built-in state");
            case SERVER -> check(text.contains("Singleplayer")&&text.stream().noneMatch(line->line.contains(":")),"local server state has no invented address");
            case MOVEMENT -> {
                check(text.getFirst().equals("Sprint  "+(mc.player.isSprinting()?"On":mc.options.keySprint.isDown()?"Ready":"Off")),"movement HUD reports actual sprint state separately from held input");
                check(text.getLast().equals("Sneak  "+(mc.player.isCrouching()?"On":mc.options.keyShift.isDown()?"Ready":"Off")),"movement HUD reports actual crouching state");
            }
            case WORLD_TIME -> {
                long actual=mc.level.getOverworldClockTime();
                check(text.equals(List.of("WORLD  "+HudNumbers.worldTime(actual),"Day  "+Math.floorDiv(actual,24000))),"world time reads the actual synchronized clock");
                me.xchro.neon.module.ClientModule.TIME_CHANGER.enabled(Neon.config,true);Neon.config.visualTime=18000;
                check(content(kind,false).equals(text),"world-time HUD is independent of cosmetic time changing");
                me.xchro.neon.module.ClientModule.TIME_CHANGER.enabled(Neon.config,false);Neon.config.worldTimeDay=false;
                check(content(kind,false).size()==1,"world-time day count is optional");Neon.config.worldTimeDay=true;
            }
            case ITEM_COUNTER -> {
                check(text.equals(List.of("Pearls  28")),"item counter totals matching backpack stacks and excludes other items");
                var before=mc.player.getOffhandItem().copy();
                try {
                    mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL,3));
                    check(content(kind,false).equals(List.of("Pearls  31")),"item counter includes offhand exactly once");
                    Neon.config.counterOffhand=false;check(content(kind,false).equals(List.of("Pearls  28")),"item counter can exclude offhand");
                } finally { mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,before);Neon.config.counterOffhand=true; }
            }
            case SCOREBOARD -> {
                check(text.equals(List.of("Practice","[A] Alpha  nine","Beta  2")),"scoreboard preserves title, team prefix, number format, ordering and hidden entries");
                check(mc.level.getScoreboard().listPlayerScores(scoreboardFixture).size()==3,"scoreboard presentation leaves original scores intact");
                check(vanillaScoreboardText()==0,"movable scoreboard replaces rather than duplicates the native sidebar");
                Neon.config.hud(kind).placement.visible=false;check(vanillaScoreboardText()>0,"disabled Neon scoreboard restores the native sidebar");Neon.config.hud(kind).placement.visible=true;
                Neon.config.scoreboardScores=false;check(content(kind,false).equals(List.of("Practice","[A] Alpha","Beta")),"scoreboard scores can be hidden independently");
                Neon.config.scoreboardTitle=false;check(content(kind,false).size()==2,"scoreboard title can be hidden independently");
                Neon.config.scoreboardRows=1;check(content(kind,false).size()==1,"scoreboard row limit applies to the actual entries");
                Neon.config.scoreboardTitle=Neon.config.scoreboardScores=true;Neon.config.scoreboardRows=15;
            }
            case MEMORY -> {
                check(text.getFirst().matches("MEMORY  (?:[0-9]{1,2}|100)%"),"memory percentage is bounded and readable");
                check(text.getLast().endsWith(" / "+(Runtime.getRuntime().maxMemory()>>20)+" MiB"),"memory HUD reports this client's actual heap limit");
            }
            default -> {}
        }
        HudWidgets.invalidate();
    }
    private void revealSettings(HudKind kind) {
        var fold=named(kind.label+" settings");var group=(ContainerEventHandler)fold;
        mc.setLastInputType(InputType.KEYBOARD_TAB);group.setFocused(group.children().getLast());
        var scroll=widgets(editor).stream().filter(w->w instanceof AbstractScrollArea).findFirst().orElseThrow();
        ((ContainerEventHandler)scroll).setFocused(fold);
        var last=(AbstractWidget)group.children().getLast();
        check(last.getBottom()<=scroll.getBottom(),kind+" final setting reveals within compact column");
        double before=((AbstractScrollArea)scroll).scrollAmount();
        editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_H,0,0));editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_H,0,0));
        var restored=(AbstractScrollArea)widgets(editor).stream().filter(w->w instanceof AbstractScrollArea).findFirst().orElseThrow();
        check(Math.abs(restored.scrollAmount()-before)<1,kind+" column hide/show preserves scrolled options ("+before+" -> "+restored.scrollAmount()+")");
        mc.setLastInputType(InputType.MOUSE);
    }
    boolean run() throws Exception {
        if(index>=HudKind.values().length) {
            if(viewport==0) { viewport=1;index=0;age=0;return false; }
            Neon.applyConfig(original);mc.gui.setScreen(null);return true;
        }
        var kind=HudKind.values()[index];
        if(age++==0) {
            mc.getWindow().setWindowed(viewport==0?1280:640,viewport==0?800:360);
            mc.options.guiScale().set(viewport==0?2:3);mc.resizeGui();
            Neon.applyConfig(new NeonConfig());Neon.config.armor.visible=Neon.config.totems.visible=Neon.config.fps.visible=false;
            if(kind==HudKind.INVENTORY) {
                seedSlot(9,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,32));
                seedSlot(17,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE,8));
                seedSlot(35,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL,16));
            }
            if(kind==HudKind.HELD_ITEM) {
                var tool=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);tool.setDamageValue(tool.getMaxDamage()-2);
                seedSlot(mc.player.getInventory().getSelectedSlot(),tool);
            }
            if(kind==HudKind.ITEM_COUNTER) {
                seedSlot(9,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL,12));
                seedSlot(35,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL,16));
                seedSlot(17,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET,9));
            }
            if(kind==HudKind.SCOREBOARD)scoreboardFixture();
            Neon.config.hud(kind).placement.x=.65;Neon.config.hud(kind).placement.y=.55;
            new HudPanelPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-hud-window.json"),Neon.SAVES);Neon.SAVES.flush().join();
            mc.gui.setScreen(editor=new HudEditor(null));
            check(!Neon.config.hud(kind).placement.visible,kind+" starts disabled");
            clickRow(kind.label,0);check(Neon.config.hud(kind).placement.visible,kind+" enables independently");
            clickRow(kind.label,1);
        }
        if(age==5)dataChecks(kind);
        if(age==12)revealSettings(kind);
        if(age==8) {
            var b=HudWidgets.get(kind).bounds(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true);
            check(b.width()>0&&b.height()>0&&b.x()>=0&&b.y()>=0&&b.x()+b.width()<=mc.getWindow().getWidth()&&b.y()+b.height()<=mc.getWindow().getHeight(),kind+" bounds fit viewport");
            check(HudWidgets.get(kind).hasContent(true),kind+" supplies editor content");
            var options=Neon.config.hud(kind);
            var session=new SettingsSession();
            session.change(()->{options.shadow=true;options.border=false;options.placement.scale=1.125;});
            check(session.undo(),kind+" appearance has Undo");
            check(Neon.config.hud(kind).border&&!Neon.config.hud(kind).shadow,kind+" Undo restores appearance");
            var profiles=new ProfileStore(mc.gameDirectory.toPath().resolve("config/neon-expansion-test-profiles.json"));
            profiles.initialize(Neon.config);profiles.put(kind.name(),Neon.config);
            check(profiles.get(kind.name()).hud(kind).placement.visible,kind+" survives profile persistence");
            var fontSession=new SettingsSession();fontSession.change(()->Neon.config.hudFont=NeonConfig.HudFont.MINECRAFT);
            var bitmapBounds=HudWidgets.get(kind).bounds(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true);
            check(bitmapBounds.width()>0&&bitmapBounds.height()>0,kind+" bitmap font has usable measured bounds");
            check(fontSession.undo()&&Neon.config.hudFont==NeonConfig.HudFont.INTER,kind+" font selection supports Undo");
            check(profiles.get(kind.name()).hudFont==NeonConfig.HudFont.INTER,kind+" profile preserves Inter selection");
            editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_H,0,0));
            b=HudWidgets.get(kind).bounds(mc.getWindow().getWidth(),mc.getWindow().getHeight(),true);
            double oldX=Neon.config.hud(kind).placement.x;
            var start=event(b.x()+b.width()/2.0,b.y()+b.height()/2.0,0);
            var end=new MouseButtonEvent(start.x()-24,start.y()-14,new MouseButtonInfo(0,0));
            check(editor.mouseClicked(start,false),kind+" starts canvas drag");
            editor.mouseDragged(end,-24,-14);editor.mouseReleased(end);
            check(Neon.config.hud(kind).placement.x!=oldX,kind+" moves directly");
            editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL));
            check(Math.abs(Neon.config.hud(kind).placement.x-oldX)<1e-9,kind+" gesture Undo restores position");
            Neon.config.hud(kind).placement.visible=false;
            check(!editor.mouseClicked(start,false),kind+" disabled canvas rejects pointer");
            Neon.config.hud(kind).placement.visible=true;
            editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_H,0,0));
        }
        if(age==16) Screenshot.grab(mc.gameDirectory,"neon-expansion-"+kind.name().toLowerCase(Locale.ROOT)+"-"+viewport+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});
        if(age==18) {
            inventoryBefore.forEach((slot,stack)->mc.player.getInventory().setItem(slot,stack));inventoryBefore.clear();
            if(scoreboardFixture!=null) {
                var board=mc.level.getScoreboard();scoreboardBefore.forEach(board::setDisplayObjective);scoreboardBefore.clear();
                board.removeObjective(scoreboardFixture);board.removePlayerTeam(scoreboardTeam);scoreboardFixture=null;scoreboardTeam=null;
            }
            index++;age=0;
        }
        return false;
    }
}
