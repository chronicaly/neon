package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.hud.*;
import me.xchro.neon.module.ClientModule;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;

/** Actual framebuffer checks for joined HUD surfaces, Unity block shaders and visible low fire. */
final class RefinementSmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private int phase,age,checks,outlinePixels;
    private boolean pending,done;
    private String failure;
    private HudBounds fps,coordinates;
    private double fireY,fireSum;
    private int fireSamples,firePixels;
    private ModulesSmoke prelude;
    private boolean preludeDone;
    private volatile boolean fireReady;
    private net.minecraft.server.level.ServerPlayer firePlayer;
    private int previousFireTicks;
    private boolean previousInvulnerable;
    private final BlockPos target=new BlockPos(0,102,3);
    private net.minecraft.world.level.block.state.BlockState oldBlock;
    RefinementSmoke(Minecraft mc,int checks){this.mc=mc;this.checks=checks;original=ConfigStore.copy(Neon.config);}
    int checks(){return checks;}
    private void check(boolean condition,String name){if(!condition)throw new AssertionError("Refinement "+phase+": "+name);checks++;System.out.println("NEON_CHECK "+name);}
    private void setup() {
        mc.gui.setScreen(null);mc.getWindow().setWindowed(phase==6?640:1280,phase==6?360:800);
        mc.options.guiScale().set(phase==6?3:phase%3+1);mc.resizeGui();mc.options.setCameraType(CameraType.FIRST_PERSON);
        Neon.applyConfig(new NeonConfig());var c=Neon.config;c.armor.visible=c.totems.visible=false;
        if(phase<=6) {
            c.theme.background=0xff204060;c.theme.outline=0xff50f0e0;c.theme.secondary=0xff8060ff;
            c.fpsBackground=c.fpsBorder=true;c.fpsShadow=false;c.coordinatesDimension=false;
            c.hud(HudKind.COORDINATES).placement.visible=phase!=5;
            c.hudBackgrounds=phase!=2;c.hudOutlines=phase!=3;c.hudSurfaces=phase!=4;
            if(phase==6){FpsColor.BACKGROUND.color(c,0xff802010);FpsColor.BORDER.color(c,0xffffff00);}
            int w=mc.getWindow().getWidth(),h=mc.getWindow().getHeight();
            fps=FpsHud.bounds(w,h);fps.move(c.fps,100,80,w,h);fps=FpsHud.bounds(w,h);
            coordinates=HudWidgets.get(HudKind.COORDINATES).bounds(w,h,true);
            coordinates.move(c.hud(HudKind.COORDINATES).placement,phase==6?fps.x()+fps.width()+6:100,
                phase==6?fps.y():fps.y()+fps.height()+(phase==1?24:6),w,h);
            coordinates=HudWidgets.get(HudKind.COORDINATES).bounds(w,h,true);
            check(fps.height()==coordinates.height(),"FPS and coordinate rows share tight physical height");
            mc.gui.setScreen(new PixelScreen(Component.literal("HUD surface verification")){
                @Override protected void renderPixels(GuiGraphicsExtractor g,int x,int y,float delta){
                    g.pose().pushMatrix();g.pose().scale(1/UI_SCALE);g.fill(0,0,mc.getWindow().getWidth(),mc.getWindow().getHeight(),0xff000000);
                    g.nextStratum();HudChrome.frame(4_000_000_000L);NeonHud.drawPixels(g,true,mc.getWindow().getWidth(),mc.getWindow().getHeight());g.pose().popMatrix();
                }
            });
        } else if(phase==7) {
            c.fpsBackground=c.fpsBorder=true;c.coordinatesDimension=false;
            int x=360,y=85,w=mc.getWindow().getWidth(),h=mc.getWindow().getHeight();
            var b=FpsHud.bounds(w,h);b.move(c.fps,x,y,w,h);y+=b.height()+6;
            for(var kind:new HudKind[]{HudKind.PING,HudKind.COORDINATES,HudKind.SPEED,HudKind.CPS,HudKind.BIOME,HudKind.LIGHT,HudKind.WORLD_TIME}) {
                c.hud(kind).placement.visible=true;b=HudWidgets.get(kind).bounds(w,h,true);b.move(c.hud(kind).placement,x,y,w,h);y+=b.height()+6;
            }
            c.hud(HudKind.KEYSTROKES).placement.visible=true;c.hud(HudKind.KEYSTROKES).placement.x=.75;c.hud(HudKind.KEYSTROKES).placement.y=.25;
            mc.gui.setScreen(new HudEditor(null));
        } else {
            c.fps.visible=false;mc.player.setYRot(0);mc.player.setXRot(0);mc.player.setDeltaMovement(0,0,0);
            if(phase==8)mc.getSingleplayerServer().execute(()->{
                var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayers().getFirst();player.teleportTo(.5,101,.5);
                oldBlock=player.level().getBlockState(target);player.level().setBlockAndUpdate(target,net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
            });
            if(phase<13) {
                ClientModule.BLOCK_OUTLINE.enabled(c,true);c.blockOutlineColor=0xffff00ff;c.blockOutlineGradient=false;c.blockOutlineWidth=2;
                c.blockOutlineFill=.5;c.blockOutlineMode=NeonConfig.BlockOutlineMode.values()[phase-8];c.blockOutlineGlow=phase==9;
            }
            if(phase==14)mc.getSingleplayerServer().execute(()->{
                firePlayer=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                previousFireTicks=firePlayer.getRemainingFireTicks();previousInvulnerable=firePlayer.isInvulnerable();
                // A client-only flag can be overwritten by entity-data synchronization between
                // the tick and deferred screenshot. Keep both sides burning for both captures.
                firePlayer.setInvulnerable(true);firePlayer.setRemainingFireTicks(200);firePlayer.setSharedFlagOnFire(true);fireReady=true;
            });
            // Keep flames comfortably in-frame across native animation frames. The module
            // vertex checks separately cover the default 40% and the exact zero transform.
            if(phase>=14){ClientModule.LOW_FIRE.enabled(c,phase==15);c.fireLowering=.2;}
        }
    }
    private void restoreFire(){
        if(!fireReady)return;fireReady=false;
        mc.getSingleplayerServer().execute(()->{
            firePlayer.setRemainingFireTicks(previousFireTicks);firePlayer.setInvulnerable(previousInvulnerable);
            firePlayer.setSharedFlagOnFire(previousFireTicks>0);
        });
    }
    private List<AbstractWidget> widgets(ContainerEventHandler root){var result=new ArrayList<AbstractWidget>();for(var child:root.children()){if(child instanceof AbstractWidget w)result.add(w);if(child instanceof ContainerEventHandler c)result.addAll(widgets(c));}return result;}
    private void click(HudEditor editor,String label,int button) {
        var row=widgets(editor).stream().filter(w->w.getMessage().getString().equals(label)).findFirst().orElseThrow();
        var scroll=widgets(editor).stream().filter(w->w instanceof AbstractScrollArea).findFirst().orElseThrow();
        mc.setLastInputType(InputType.KEYBOARD_TAB);((ContainerEventHandler)scroll).setFocused(row);
        check(row.getY()>=scroll.getY()&&row.getBottom()<=scroll.getBottom(),label+" keyboard reveal fits the compact column");
        var event=new MouseButtonEvent(editor.guiCoordinate(row.getX()+16),editor.guiCoordinate(row.getY()+10),new MouseButtonInfo(button,0));
        check(editor.mouseClicked(event,false),label+" responds through its real HUD row");editor.mouseReleased(event);
    }
    private void controls() throws Exception {
        Object previousMesh=null;
        for(int offset=0;offset<120;offset++) {
            var state=new net.minecraft.client.renderer.state.gui.GuiRenderState();
            var g=new GuiGraphicsExtractor(mc,state,0,0);
            HudGlassRenderer.draw(g,List.of(
                new HudGlassRenderer.Surface(new HudBounds(100+offset,100+offset,87,24),0x80204060,0xff50f0e0,0xff8060ff,true,true),
                new HudGlassRenderer.Surface(new HudBounds(193+offset,100+offset,313,134),0x80204060,0xff50f0e0,0xff8060ff,true,true)),0,true);
            Object[] mesh={null};state.forEachElement(element->{try{var f=element.getClass().getDeclaredField("fillMesh");f.setAccessible(true);mesh[0]=f.get(element);}catch(Exception e){throw new AssertionError(e);}},net.minecraft.client.renderer.state.gui.GuiRenderState.TraverseRange.ALL);
            check(mesh[0]!=null&&(previousMesh==null||mesh[0]==previousMesh),"moving a joined cluster reuses its tessellation");previousMesh=mesh[0];
        }
        var editor=(HudEditor)mc.gui.screen();click(editor,"General",1);click(editor,"HUD surfaces",0);
        check(!Neon.config.hudSurfaces&&Neon.config.fpsBorder&&Neon.config.hud(HudKind.COORDINATES).border,"master toggle preserves local outline choices");
        editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL));check(Neon.config.hudSurfaces,"master surface toggle supports Undo");
        click(editor,"Backgrounds",0);click(editor,"Outlines",0);
        var profiles=new ProfileStore(mc.gameDirectory.toPath().resolve("config/neon-glass-test-profiles.json"));profiles.initialize(Neon.config);profiles.put("Glass",Neon.config);
        var saved=profiles.get("Glass");check(!saved.hudBackgrounds&&!saved.hudOutlines&&saved.hudSurfaces,"independent master visibility survives profiles");
        editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL));editor.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z,0,GLFW.GLFW_MOD_CONTROL));
        check(Neon.config.hudBackgrounds&&Neon.config.hudOutlines,"Undo restores both global visibility gates");
    }
    boolean run()throws Exception {
        if(Boolean.getBoolean("neon.smoke.refinementAfterModules")&&!preludeDone){
            if(prelude==null)prelude=new ModulesSmoke(mc,checks);
            if(!prelude.run())return false;checks=prelude.checks();preludeDone=true;
        }
        if(failure!=null){restoreFire();throw new AssertionError(failure);}
        if(phase==16) {
            restoreFire();mc.player.setSharedFlagOnFire(false);Neon.applyConfig(original);mc.gui.setScreen(null);
            if(oldBlock!=null)mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().overworld().setBlockAndUpdate(target,oldBlock));
            return true;
        }
        if(age++==0)setup();
        if(phase>=14){if(!fireReady)return false;mc.player.setSharedFlagOnFire(true);}
        if(age<30)return false;
        if(phase==7&&age==30){controls();return false;}
        if(age<40)return false;
        if(!pending){pending=true;Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{
            try(image) {
                image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots/neon-refinement-"+phase+(phase>=14?"-"+fireSamples:"")+".png"));
                if(phase<=6) {
                    int edge=image.getPixel(fps.x(),fps.y()+fps.height()/2)&0xffffff;
                    int bridge=image.getPixel(phase==6?fps.x()+fps.width()+3:fps.x()+20,phase==6?fps.y()+12:fps.y()+fps.height()+3)&0xffffff;
                    int interior=image.getPixel(phase==6?fps.x()+fps.width()-2:fps.x()+20,phase==6?fps.y()+12:fps.y()+fps.height()-2)&0xffffff;
                    boolean hasEdge=((edge>>>8)&255)>80;
                    if(phase==0||phase==6){check(hasEdge,"shared glass outline reaches the framebuffer");check(bridge!=0,"nearby real widgets share a filled glass bridge");check(((interior>>>8)&255)<80,"joined HUD removes the internal border");}
                    if(phase==1)check(bridge==0,"separating widgets removes the glass bridge");
                    if(phase==2)check(hasEdge&&bridge==0,"background master removes fill while keeping outlines");
                    if(phase==3)check(!hasEdge&&bridge!=0,"outline master removes strokes while keeping merged background");
                    if(phase==4)check(edge==0&&bridge==0,"surface master removes both fill and outlines");
                    if(phase==5)check(bridge==0,"disabled widgets cannot attract a glass join");
                    if(phase==6){
                        int fill=image.getPixel(coordinates.x()+coordinates.width()-3,coordinates.y()+coordinates.height()/2);
                        int border=image.getPixel(coordinates.x()+coordinates.width()-1,coordinates.y()+coordinates.height()/2);
                        check((fill&255)>(fill>>>16&255)*2,"joined FPS background override stays out of coordinate fill");
                        check((border&255)>(border>>>16&255)&&(edge&255)<20,"joined FPS border override stays local while the neighbor keeps its palette");
                    }
                } else if(phase>=8&&phase<=13) {
                    int pixels=0;for(int y=image.getHeight()/4;y<image.getHeight()*3/4;y++)for(int x=image.getWidth()/4;x<image.getWidth()*3/4;x++) {
                        int color=image.getPixel(x,y);if((color>>>16&255)>120&&(color&255)>120&&(color>>>8&255)<80)pixels++;
                    }
                    if(phase==8){outlinePixels=pixels;check(pixels>100,"Unity silhouette outline reaches actual block framebuffer ("+pixels+")");}
                    if(phase==9||phase==10)check(pixels>outlinePixels*3,"Unity filled block mode draws the surface ("+pixels+")");
                    if(phase==11||phase==12)check(pixels>50,"Unity glow/native wire mode reaches framebuffer ("+pixels+")");
                    if(phase==13)check(pixels<10,"disabling custom block rendering restores native selection");
                    if(phase<13)check(ClientModule.BLOCK_OUTLINE.active(),"block shader completes without fallback or renderer errors");
                } else if(phase>=14) {
                    double sum=0;int count=0;
                    for(int y=80;y<image.getHeight();y++)for(int x=20;x<image.getWidth()/3;x++) {
                        int color=image.getPixel(x,y),r=color>>>16&255,g=color>>>8&255,b=color&255;
                        if(r>150&&g>40&&g<180&&b<70){sum+=y;count++;}
                    }
                    System.out.println("NEON_FIRE phase="+phase+" sample="+fireSamples+" pixels="+count+" burning="+mc.player.isOnFire()+" lowering="+Neon.config.fireLowering+" active="+ClientModule.LOW_FIRE.active());
                    check(mc.player.isOnFire(),"burning fixture stays active through native animation samples");
                    fireSum+=sum;firePixels+=count;
                    if(fireSamples==7){
                        // Orange coverage varies sharply across native texture frames; assert
                        // presence and the weighted position over the sample, not each sprite.
                        check(firePixels>4000,"burning fixture renders actual fire pixels across animation frames");
                        double center=fireSum/firePixels;
                        if(phase==14)fireY=center;else check(center>fireY+30,"low fire lowers visible flames across animation frames: "+fireY+" -> "+center);
                    }
                }
                done=true;
            }catch(Throwable error){failure=error.toString();}
        });return false;}
        if(!done)return false;
        if(phase>=14&&++fireSamples<8){age=35;pending=done=false;return false;}
        fireSamples=0;firePixels=0;fireSum=0;phase++;age=0;pending=done=false;return false;
    }
}
