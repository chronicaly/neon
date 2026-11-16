package me.xchro.neon.smoke;

import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.config.*;
import me.xchro.neon.module.ClientModule;
import me.xchro.neon.ui.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;

/** Uses the packaged mixins, actual framebuffer and real drawers for every added module. */
final class ModulesSmoke {
    private final Minecraft mc;
    private final NeonConfig original;
    private final List<ClientModule> modules=Arrays.stream(ClientModule.values()).filter(ClientModule::extra).toList();
    private int checks,index,phase,age;
    private boolean pending,done;
    private String failure;
    private SettingsScreen menu;
    private CrosshairSmoke crosshairAudit;
    private boolean crosshairChecked;
    ModulesSmoke(Minecraft mc,int checks) { this.mc=mc;this.checks=checks;original=ConfigStore.copy(Neon.config); }
    int checks() { return checks; }
    private void check(boolean value,String name) {
        if(!value)throw new AssertionError("Module "+index+"/"+phase+": "+name);
        checks++;System.out.println("NEON_CHECK "+name);
    }
    private List<AbstractWidget> widgets(ContainerEventHandler root) {
        var result=new ArrayList<AbstractWidget>();
        for(var child:root.children()) { if(child instanceof AbstractWidget w)result.add(w);if(child instanceof ContainerEventHandler group)result.addAll(widgets(group)); }
        return result;
    }
    private void setup(ClientModule module) {
        mc.setLastInputType(InputType.MOUSE);mc.getWindow().setWindowed(1280,800);
        mc.options.guiScale().set(phase%2==0?1:3);mc.resizeGui();mc.options.setCameraType(CameraType.FIRST_PERSON);
        Neon.applyConfig(new NeonConfig());
        check(!module.enabled(Neon.config),module+" starts disabled");module.enabled(Neon.config,true);
        check(module.active(),module+" works without assigning a key");
        if(module==ClientModule.CROSSHAIR) { Neon.config.crosshair.color=0xff18e5bc;Neon.config.crosshair.thickness=2; }
        if(phase<2) { mc.gui.setScreen(null);return; }
        new MenuPlacement().queue(mc.gameDirectory.toPath().resolve("config/neon-window.json"),Neon.SAVES);Neon.SAVES.flush().join();
        mc.gui.setScreen(menu=Menus.fresh(null));
        if(phase==3) {
            var b=menu.menuBounds();var start=mouse(b.right()-1,b.bottom()-1);var end=mouse(b.right()-121,b.bottom()-71);
            menu.mouseClicked(start,false);menu.mouseDragged(end,end.x()-start.x(),end.y()-start.y());menu.mouseReleased(end);
        }
        widgets(menu).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals(module.page==0?"Visuals":"Combat")).map(w->(Button)w).findFirst().orElseThrow().onPress(null);
        var grid=widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).findFirst().orElseThrow();
        var tile=widgets(menu).stream().filter(w->w.getClass().getSimpleName().equals("UiModuleTile")&&w.getMessage().getString().equals(module.buttonLabel)).findFirst().orElseThrow();
        mc.setLastInputType(InputType.KEYBOARD_TAB);((ContainerEventHandler)grid).setFocused(tile);
        check(tile.getY()>=grid.getY()&&tile.getBottom()<=grid.getBottom(),module+" keyboard reveals its entire tile");mc.setLastInputType(InputType.MOUSE);
        var open=new MouseButtonEvent(menu.guiCoordinate(tile.getX()+12),menu.guiCoordinate(tile.getY()+12),new MouseButtonInfo(1,0));
        check(menu.mouseClicked(open,false),module+" opens through a visible right click");menu.mouseReleased(open);
    }
    private MouseButtonEvent mouse(double x,double y) { return new MouseButtonEvent(menu.guiCoordinate(x),menu.guiCoordinate(y),new MouseButtonInfo(0,0)); }
    private void verify(ClientModule module) throws Exception {
        if(phase<2) {
            if(phase==0)bindings(module);
            if(module==ClientModule.TOGGLE_SPRINT||module==ClientModule.TOGGLE_SNEAK)movement(module);
            if(module==ClientModule.DYNAMIC_FOV)fov();
            if(module==ClientModule.HURT_CAMERA)hurtCamera();
            if(module==ClientModule.VIEW_BOBBING)viewBobbing();
            if(module==ClientModule.LOW_FIRE)lowFire();
            if(module==ClientModule.VIEWMODEL)viewmodel();
            if(module==ClientModule.ENCHANT_GLINT)glint();
            if(module==ClientModule.HIT_COLOR)hitColor();
            if(module==ClientModule.TIME_CHANGER)visualTime();
            if(module==ClientModule.WEATHER)weather();
            if(module==ClientModule.FULLBRIGHT)fullbright();
            if(module==ClientModule.CLEAN_VIEW)cleanView();
            if(module==ClientModule.BLOCK_OUTLINE)blockOutline();return;
        }
        var shell=menu.expandedBounds();
        check(shell.left()>=0&&shell.top()>=0&&shell.right()<=menu.canvasWidth()&&shell.bottom()<=menu.canvasHeight(),module+" expanded drawer fits viewport");
        var all=widgets(menu);
        check(all.stream().anyMatch(w->w.getClass().getSimpleName().equals("UiModuleTile")&&w.getMessage().getString().equals(module.buttonLabel)),module+" remains in the grid with its drawer open");
        var scrolls=all.stream().filter(w->w.getClass().getSimpleName().equals("UiScroll")).map(w->(AbstractScrollArea)w).toList();
        check(scrolls.size()==2,module+" has separate grid and detail viewports");
        var detail=scrolls.getLast();var controls=widgets((ContainerEventHandler)detail);
        check(!controls.isEmpty(),module+" exposes useful settings");
        check(controls.stream().allMatch(w->w.getX()>=detail.getX()&&w.getRight()<=detail.getRight()),module+" settings fit horizontally");
        double position=detail.scrollAmount();detail.setScrollAmount(detail.maxScrollAmount());
        check(controls.stream().mapToInt(AbstractWidget::getBottom).max().orElse(detail.getY())<=detail.getBottom(),module+" last setting is reachable");detail.setScrollAmount(position);
        check(controls.stream().noneMatch(w->w.getMessage().getString().contains("[")),module+" has no duplicate binds in settings");
        var session=new SettingsSession();session.change(()->module.enabled(Neon.config,false));check(session.undo()&&module.enabled(Neon.config),module+" enable state supports Undo");
        var store=new ProfileStore(mc.gameDirectory.toPath().resolve("config/neon-module-test-profiles.json"));
        store.initialize(Neon.config);store.put(module.name(),Neon.config);check(module.enabled(store.get(module.name())),module+" persists in profiles");
    }
    private void bindings(ClientModule module) {
        var key=module.key();String saved=key.saveString();var mode=module.mode(Neon.config);boolean enabled=module.enabled(Neon.config);
        try {
            check(Neon.ready(mc),"binding fixture is interactive gameplay");
            key.setKey(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_F8));KeyMapping.resetMapping();
            for(var tested:me.xchro.neon.core.BindMode.values()) {
                module.enabled(Neon.config,true);module.mode(Neon.config,tested);Neon.MODULE_BINDINGS.reset();
                key.setDown(false);Neon.MODULE_BINDINGS.tick(mc);
                check(module.active()==(tested!=me.xchro.neon.core.BindMode.HOLD),module+" released state follows "+tested);
                key.setDown(true);Neon.MODULE_BINDINGS.tick(mc);
                check(module.active()==(tested==me.xchro.neon.core.BindMode.HOLD),module+" pressed state follows "+tested);
                key.setDown(false);Neon.MODULE_BINDINGS.tick(mc);
                check(module.active()==(tested==me.xchro.neon.core.BindMode.REVERSE_HOLD),module+" release follows "+tested);
            }
            var session=new SettingsSession();session.change(()->key.setKey(com.mojang.blaze3d.platform.InputConstants.UNKNOWN));
            check(session.undo()&&key.saveString().equals("key.keyboard.f8"),module+" binding participates in shared Undo");
        } finally {
            key.setDown(false);key.setKey(com.mojang.blaze3d.platform.InputConstants.getKey(saved));KeyMapping.resetMapping();
            module.mode(Neon.config,mode);module.enabled(Neon.config,enabled);Neon.MODULE_BINDINGS.reset();
        }
    }
    private void blockOutline() throws Exception {
        Neon.config.blockOutlineMode=NeonConfig.BlockOutlineMode.WIREFRAME;
        var calls=new ArrayList<Object[]>();var collector=(net.minecraft.client.renderer.SubmitNodeCollector)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{net.minecraft.client.renderer.SubmitNodeCollector.class},(proxy,method,args)->{
            if(method.getName().equals("submitShapeOutline"))calls.add(args.clone());return null;
        });
        var state=new net.minecraft.client.renderer.state.level.LevelRenderState();state.cameraRenderState.pos=net.minecraft.world.phys.Vec3.ZERO;
        var shape=net.minecraft.world.phys.shapes.Shapes.block();state.blockOutlineRenderState=new net.minecraft.client.renderer.state.level.BlockOutlineRenderState(net.minecraft.core.BlockPos.ZERO,false,true,shape);
        var pose=new com.mojang.blaze3d.vertex.PoseStack();
        var submit=net.minecraft.client.renderer.LevelRenderer.class.getDeclaredMethod("submitBlockOutline",pose.getClass(),net.minecraft.client.renderer.SubmitNodeCollector.class,state.getClass());submit.setAccessible(true);
        Neon.config.blockOutlineColor=0xff25b3d9;Neon.config.blockOutlineWidth=3.25;submit.invoke(mc.levelRenderer,pose,collector,state);
        check(calls.size()==2,"block outline preserves the native high-contrast backing pass");
        check((int)calls.getFirst()[3]==0xff000000&&(int)calls.getLast()[3]==0xff25b3d9,"block outline colors only the primary selection pass");
        check((float)calls.getLast()[4]==3.25f&&calls.getLast()[1]==shape,"block outline uses requested width and original target shape");
        check(pose.last().pose().equals(new org.joml.Matrix4f(),.00001f),"block outline leaves the parent pose intact");
        calls.clear();state.blockOutlineRenderState=null;submit.invoke(mc.levelRenderer,pose,collector,state);
        check(calls.isEmpty(),"block outline never creates a target when vanilla has none");
        Neon.config.blockOutlineColor=null;Neon.config.blockOutlineWidth=2;
        Neon.config.blockOutlineMode=NeonConfig.BlockOutlineMode.OUTLINE;
    }
    private int overlayElements(String name,Object value) throws Exception {
        var state=new net.minecraft.client.renderer.state.gui.GuiRenderState();var g=new net.minecraft.client.gui.GuiGraphicsExtractor(mc,state,0,0);
        java.lang.reflect.Method method;
        if(value instanceof net.minecraft.resources.Identifier id) {
            method=net.minecraft.client.gui.Hud.class.getDeclaredMethod(name,g.getClass(),net.minecraft.resources.Identifier.class,float.class);method.setAccessible(true);method.invoke(mc.gui.hud,g,id,1f);
        } else if(value instanceof net.minecraft.world.entity.Entity entity) {
            method=net.minecraft.client.gui.Hud.class.getDeclaredMethod(name,g.getClass(),net.minecraft.world.entity.Entity.class);method.setAccessible(true);method.invoke(mc.gui.hud,g,entity);
        } else {
            method=net.minecraft.client.gui.Hud.class.getDeclaredMethod(name,g.getClass(),float.class);method.setAccessible(true);method.invoke(mc.gui.hud,g,1f);
        }
        int[] count={0};state.forEachElement(element->count[0]++,net.minecraft.client.renderer.state.gui.GuiRenderState.TraverseRange.ALL);return count[0];
    }
    private void cleanView() throws Exception {
        var equipment=net.minecraft.world.item.Items.CARVED_PUMPKIN.getDefaultInstance().get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        var pumpkin=equipment.cameraOverlay().orElseThrow().withPath(path->"textures/"+path+".png");
        check(overlayElements("extractTextureOverlay",pumpkin)==0,"clean view removes the actual equipped pumpkin overlay");
        check(overlayElements("extractPortalOverlay",null)==0,"clean view removes portal overlay geometry");
        check(overlayElements("extractVignette",mc.player)==0,"clean view removes vignette geometry");
        check(overlayElements("extractTextureOverlay",net.minecraft.resources.Identifier.withDefaultNamespace("textures/misc/powder_snow_outline.png"))>0,"clean view preserves the freezing indicator");
        ClientModule.CLEAN_VIEW.enabled(Neon.config,false);
        check(overlayElements("extractTextureOverlay",pumpkin)>0&&overlayElements("extractPortalOverlay",null)>0&&overlayElements("extractVignette",mc.player)>0,"disabled clean view restores native overlays");ClientModule.CLEAN_VIEW.enabled(Neon.config,true);
    }
    private void fullbright() {
        var before=mc.player.getActiveEffects().stream().map(net.minecraft.world.effect.MobEffectInstance::new).toList();double gamma=mc.options.gamma().get();
        var extractor=new net.minecraft.client.renderer.LightmapRenderStateExtractor(mc.gameRenderer,mc);
        var state=new net.minecraft.client.renderer.state.LightmapRenderState();
        try {
            mc.player.getActiveEffectsMap().clear();Neon.config.fullbrightStrength=.63;extractor.tick();extractor.extract(state,0);
            check(Math.abs(state.nightVisionEffectIntensity-.63)<.00001,"fullbright reaches the real lightmap without a status effect");
            check(mc.player.getActiveEffects().isEmpty()&&mc.options.gamma().get()==gamma,"fullbright preserves effects and saved gamma");
            for(var effect:List.of(net.minecraft.world.effect.MobEffects.DARKNESS,net.minecraft.world.effect.MobEffects.BLINDNESS)) {
                mc.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,200));extractor.tick();extractor.extract(state,0);
                check(state.nightVisionEffectIntensity==0,"fullbright preserves "+effect.value().getDescriptionId());mc.player.getActiveEffectsMap().clear();
            }
            ClientModule.FULLBRIGHT.enabled(Neon.config,false);extractor.tick();extractor.extract(state,0);
            check(state.nightVisionEffectIntensity==0,"disabled fullbright restores native lightmap data");
        } finally { mc.player.getActiveEffectsMap().clear();before.forEach(mc.player::addEffect);ClientModule.FULLBRIGHT.enabled(Neon.config,true);Neon.config.fullbrightStrength=1; }
    }
    private void weather() {
        float actual=mc.level.getRainLevel(0);
        try(var renderer=new net.minecraft.client.renderer.WeatherEffectRenderer()) {
            var state=new net.minecraft.client.renderer.state.level.WeatherRenderState();
            Neon.config.weather=NeonConfig.Weather.RAIN;Neon.config.weatherIntensity=.6;
            renderer.extractRenderState(mc.level,0,mc.player.position(),state);
            check(Math.abs(state.intensity-.6)<.00001,"weather intensity reaches native precipitation extraction");
            state.reset();Neon.config.weather=NeonConfig.Weather.CLEAR;renderer.extractRenderState(mc.level,0,mc.player.position(),state);
            check(state.intensity==0&&state.rainColumns.isEmpty()&&state.snowColumns.isEmpty(),"clear weather submits no precipitation");
            check(mc.level.getRainLevel(0)==actual,"visual weather preserves the actual world weather");
            ClientModule.WEATHER.enabled(Neon.config,false);state.reset();renderer.extractRenderState(mc.level,0,mc.player.position(),state);
            check(state.intensity==actual,"disabled weather restores native precipitation");ClientModule.WEATHER.enabled(Neon.config,true);
        }
    }
    private void visualTime() {
        var attributes=mc.level.environmentAttributes();var sun=net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE;
        long actual=mc.level.getOverworldClockTime();ClientModule.TIME_CHANGER.enabled(Neon.config,false);attributes.invalidateTickCache();
        float nativeAngle=attributes.getDimensionValue(sun);
        ClientModule.TIME_CHANGER.enabled(Neon.config,true);Neon.config.visualTime=6000;attributes.invalidateTickCache();float noon=attributes.getDimensionValue(sun);
        Neon.config.visualTime=18000;attributes.invalidateTickCache();float midnight=attributes.getDimensionValue(sun);
        check(Math.abs(noon-midnight)>.1,"time changer changes actual visual sky timeline sampling");
        check(mc.level.getOverworldClockTime()==actual,"time changer preserves the real client world clock");
        check(!me.xchro.neon.render.VisualTime.visual(net.minecraft.world.attribute.EnvironmentAttributes.BED_RULE),"time changer excludes gameplay environment attributes");
        ClientModule.TIME_CHANGER.enabled(Neon.config,false);attributes.invalidateTickCache();
        check(attributes.getDimensionValue(sun)==nativeAngle,"disabled time changer restores the actual sky time");
        Neon.config.visualTime=6000;ClientModule.TIME_CHANGER.enabled(Neon.config,true);attributes.invalidateTickCache();
    }
    private void hitColor() throws Exception {
        try(var overlay=new net.minecraft.client.renderer.texture.OverlayTexture()) {
            var field=overlay.getClass().getDeclaredField("texture");field.setAccessible(true);var texture=(net.minecraft.client.renderer.texture.DynamicTexture)field.get(overlay);
            var pixels=texture.getPixels();int original=pixels.getPixel(0,0),white=pixels.getPixel(6,10);
            Neon.config.hitColor=0xff30c0e0;Neon.config.hitColorStrength=.4;overlay.getTextureView();
            check(pixels.getPixel(0,0)==0x9930c0e0&&pixels.getPixel(15,7)==0x9930c0e0,"hit color updates every actual damage overlay row with correct alpha");
            check(pixels.getPixel(6,10)==white,"hit color preserves white flashes");
            Neon.config.hitColor=0x0030c0e0;overlay.getTextureView();
            check(pixels.getPixel(0,0)==0xff30c0e0,"transparent hit color has zero overlay strength");
            ClientModule.HIT_COLOR.enabled(Neon.config,false);overlay.getTextureView();check(pixels.getPixel(0,0)==original,"disabled hit color restores the original overlay texture");
        }
        Neon.config.hitColor=null;Neon.config.hitColorStrength=.3;ClientModule.HIT_COLOR.enabled(Neon.config,true);
    }
    private void glint() throws Exception {
        var field=net.minecraft.client.renderer.GameRenderer.class.getDeclaredField("gameRenderState");field.setAccessible(true);
        var options=((net.minecraft.client.renderer.state.GameRenderState)field.get(mc.gameRenderer)).optionsRenderState;
        var extract=net.minecraft.client.renderer.GameRenderer.class.getDeclaredMethod("extractOptions");extract.setAccessible(true);
        double strength=mc.options.glintStrength().get(),speed=mc.options.glintSpeed().get();
        Neon.config.glintStrength=.23;Neon.config.glintSpeed=.81;extract.invoke(mc.gameRenderer);
        check(options.glintStrength==.23&&options.glintSpeed==.81,"glint controls reach the actual render options");
        check(mc.options.glintStrength().get()==strength&&mc.options.glintSpeed().get()==speed,"glint leaves vanilla preferences unchanged");
        ClientModule.ENCHANT_GLINT.enabled(Neon.config,false);extract.invoke(mc.gameRenderer);
        check(options.glintStrength==strength&&options.glintSpeed==speed,"disabled glint restores native render options");
        ClientModule.ENCHANT_GLINT.enabled(Neon.config,true);Neon.config.glintStrength=.6;Neon.config.glintSpeed=.35;
    }
    private org.joml.Matrix4f handPose(net.minecraft.world.InteractionHand hand) throws Exception {
        var pose=new com.mojang.blaze3d.vertex.PoseStack();var captured=new org.joml.Matrix4f[1];
        var collector=(net.minecraft.client.renderer.SubmitNodeCollector)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{net.minecraft.client.renderer.SubmitNodeCollector.class},(proxy,method,args)->{
            if(method.getName().equals("order"))return proxy;
            if(args!=null)for(var arg:args)if(arg instanceof com.mojang.blaze3d.vertex.PoseStack p)captured[0]=new org.joml.Matrix4f(p.last().pose());return null;
        });
        var submit=net.minecraft.client.renderer.ItemInHandRenderer.class.getDeclaredMethod("submitArmWithItem",net.minecraft.client.player.AbstractClientPlayer.class,float.class,float.class,net.minecraft.world.InteractionHand.class,float.class,net.minecraft.world.item.ItemStack.class,float.class,pose.getClass(),net.minecraft.client.renderer.SubmitNodeCollector.class,int.class);submit.setAccessible(true);
        submit.invoke(mc.gameRenderer.itemInHandRenderer,mc.player,0f,0f,hand,0f,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD),0f,pose,collector,15728880);
        check(captured[0]!=null,"viewmodel fixture submits actual item geometry");check(pose.last().pose().equals(new org.joml.Matrix4f(),.00001f),"viewmodel restores its parent pose");return captured[0];
    }
    private void viewmodel() throws Exception {
        var main=net.minecraft.world.InteractionHand.MAIN_HAND;var off=net.minecraft.world.InteractionHand.OFF_HAND;
        ClientModule.VIEWMODEL.enabled(Neon.config,false);var nativeMain=handPose(main);var nativeOff=handPose(off);
        ClientModule.VIEWMODEL.enabled(Neon.config,true);var options=Neon.config.viewmodel;options.vertical=-.2;options.horizontal=.15;
        var moved=handPose(main);float sign=mc.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT?1:-1;
        check(Math.abs(moved.m31()-nativeMain.m31()+.2)<.00001&&Math.abs(moved.m30()-nativeMain.m30()-.15*sign)<.00001,"viewmodel offsets affect only the rendered main hand");
        var movedOff=handPose(off);check(Math.abs(movedOff.m30()-nativeOff.m30()+.15*sign)<.00001,"offhand horizontal offset mirrors the physical arm");
        options.offhand=false;check(handPose(off).equals(nativeOff,.00001f),"offhand can retain native placement independently");
        options.horizontal=0;options.vertical=-.12;options.offhand=true;
    }
    private void lowFire() throws Exception {
        var field=net.minecraft.client.renderer.GameRenderer.class.getDeclaredField("screenEffectRenderer");field.setAccessible(true);var renderer=field.get(mc.gameRenderer);
        var spritesField=renderer.getClass().getDeclaredField("sprites");spritesField.setAccessible(true);
        var sprites=(net.minecraft.client.resources.model.sprite.SpriteGetter)spritesField.get(renderer);
        var sprite=sprites.get(net.minecraft.client.resources.model.ModelBakery.FIRE_1);
        var pose=new com.mojang.blaze3d.vertex.PoseStack();
        var deferred=new net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer[1];
        var collector=(net.minecraft.client.renderer.SubmitNodeCollector)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{net.minecraft.client.renderer.SubmitNodeCollector.class},(proxy,method,args)->{
            if(method.getName().equals("submitCustomGeometry"))deferred[0]=(net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer)args[2];return null;
        });
        var submit=renderer.getClass().getDeclaredMethod("submitFire",pose.getClass(),net.minecraft.client.renderer.SubmitNodeCollector.class,net.minecraft.client.renderer.texture.TextureAtlasSprite.class);submit.setAccessible(true);
        submit.invoke(null,pose,collector,sprite);
        var points=new ArrayList<org.joml.Vector3f>();
        var vertices=(com.mojang.blaze3d.vertex.VertexConsumer)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{com.mojang.blaze3d.vertex.VertexConsumer.class},(proxy,method,args)->{
            if(method.isDefault())return java.lang.reflect.InvocationHandler.invokeDefault(proxy,method,args);
            if(method.getName().equals("addVertex"))points.add(new org.joml.Vector3f((float)args[0],(float)args[1],(float)args[2]));
            return method.getReturnType()==void.class?null:proxy;
        });
        ClientModule.LOW_FIRE.enabled(Neon.config,false);deferred[0].render(pose.last(),vertices);
        var nativePoints=new ArrayList<>(points);points.clear();
        ClientModule.LOW_FIRE.enabled(Neon.config,true);Neon.config.fireLowering=.4;deferred[0].render(pose.last(),vertices);
        check(nativePoints.size()==8&&points.size()==8,"low fire retains both native textured quads");
        for(int i=0;i<points.size();i++) {
            var a=nativePoints.get(i);var b=points.get(i);
            check(Math.abs(b.y-a.y+.28)<.00001&&Math.abs(a.x-b.x)<.00001&&Math.abs(a.z-b.z)<.00001,"low fire lowers final deferred vertex "+i);
        }
        points.clear();Neon.config.fireLowering=0;deferred[0].render(pose.last(),vertices);
        check(points.equals(nativePoints),"zero lowering exactly restores native fire vertices");
        check(pose.last().pose().equals(new org.joml.Matrix4f(),.00001f),"low fire leaves the shared pose intact");
        Neon.config.fireLowering=.4;
    }
    private void viewBobbing() throws Exception {
        var state=new net.minecraft.client.renderer.state.level.CameraRenderState();var entity=state.entityRenderState;
        entity.isPlayer=true;entity.backwardsInterpolatedWalkDistance=.25f;entity.bob=.8f;
        ClientModule.VIEW_BOBBING.enabled(Neon.config,false);var nativePose=cameraPose("bobView",state);
        check(!nativePose.equals(new org.joml.Matrix4f(),.00001f),"walking fixture produces native bob");
        ClientModule.VIEW_BOBBING.enabled(Neon.config,true);Neon.config.viewBobbingStrength=0;
        check(cameraPose("bobView",state).equals(new org.joml.Matrix4f(),.00001f),"zero bob strength removes both translation and rotation");
        Neon.config.viewBobbingStrength=.3;var reduced=cameraPose("bobView",state);
        check(!reduced.equals(nativePose,.00001f)&&!reduced.equals(new org.joml.Matrix4f(),.00001f),"reduced walking bob remains animated");
        Neon.config.viewBobbingStrength=1;
        check(cameraPose("bobView",state).equals(nativePose,.00001f),"full bob strength matches vanilla");
        check(entity.bob==.8f,"view bobbing leaves the source movement state intact");Neon.config.viewBobbingStrength=.3;
    }
    private org.joml.Matrix4f cameraPose(String methodName,net.minecraft.client.renderer.state.level.CameraRenderState state) throws Exception {
        var method=net.minecraft.client.renderer.GameRenderer.class.getDeclaredMethod(methodName,state.getClass(),com.mojang.blaze3d.vertex.PoseStack.class);method.setAccessible(true);
        var pose=new com.mojang.blaze3d.vertex.PoseStack();method.invoke(mc.gameRenderer,state,pose);return new org.joml.Matrix4f(pose.last().pose());
    }
    private void hurtCamera() throws Exception {
        var field=net.minecraft.client.renderer.GameRenderer.class.getDeclaredField("gameRenderState");field.setAccessible(true);
        var options=((net.minecraft.client.renderer.state.GameRenderState)field.get(mc.gameRenderer)).optionsRenderState;
        double savedStrength=options.damageTiltStrength;options.damageTiltStrength=1;
        try {
        var state=new net.minecraft.client.renderer.state.level.CameraRenderState();var entity=state.entityRenderState;
        entity.isLiving=true;entity.hurtTime=6;entity.hurtDuration=10;entity.hurtDir=20;
        ClientModule.HURT_CAMERA.enabled(Neon.config,false);var nativePose=cameraPose("bobHurt",state);
        check(!nativePose.equals(new org.joml.Matrix4f(),.00001f),"damage fixture produces a native tilt");
        ClientModule.HURT_CAMERA.enabled(Neon.config,true);Neon.config.hurtCameraStrength=0;
        check(cameraPose("bobHurt",state).equals(new org.joml.Matrix4f(),.00001f),"zero hurt-camera strength removes tilt in the real renderer");
        Neon.config.hurtCameraStrength=1;check(cameraPose("bobHurt",state).equals(nativePose,.00001f),"full hurt-camera strength matches vanilla");
        Neon.config.hurtCameraStrength=0;entity.isDeadOrDying=true;entity.deathTime=10;entity.hurtTime=-1;
        var death=cameraPose("bobHurt",state);ClientModule.HURT_CAMERA.enabled(Neon.config,false);
        check(cameraPose("bobHurt",state).equals(death,.00001f),"hurt-camera adjustment preserves the death camera");
        check(entity.hurtDuration==10&&entity.hurtDir==20,"hurt-camera adjustment preserves damage render state");
        ClientModule.HURT_CAMERA.enabled(Neon.config,true);
        } finally { options.damageTiltStrength=savedStrength; }
    }
    private void fov() {
        boolean flying=mc.player.getAbilities().flying;int base=mc.options.fov().get();
        try {
            mc.player.getAbilities().flying=true;ClientModule.DYNAMIC_FOV.enabled(Neon.config,false);
            float nativeValue=mc.player.getFieldOfViewModifier(true,1);
            check(Math.abs(nativeValue-1)>.01,"FOV fixture contains a real native effect");
            ClientModule.DYNAMIC_FOV.enabled(Neon.config,true);Neon.config.fovEffectStrength=0;
            check(mc.player.getFieldOfViewModifier(true,1)==1,"zero FOV strength removes movement/use effects");
            Neon.config.fovEffectStrength=.5;
            check(Math.abs(mc.player.getFieldOfViewModifier(true,1)-(1+(nativeValue-1)*.5))<.00001,"FOV strength scales the native effect continuously");
            Neon.config.fovEffectStrength=1;
            check(mc.player.getFieldOfViewModifier(true,1)==nativeValue,"full FOV strength matches vanilla");
            check(mc.options.fov().get()==base,"dynamic FOV leaves saved base FOV unchanged");
        } finally { mc.player.getAbilities().flying=flying;Neon.config.fovEffectStrength=0; }
    }
    private void movement(ClientModule module) {
        boolean sprint=module==ClientModule.TOGGLE_SPRINT;
        var key=sprint?mc.options.keySprint:mc.options.keyShift;var nativeOption=sprint?mc.options.toggleSprint():mc.options.toggleCrouch();boolean before=nativeOption.get();
        var access=(me.xchro.neon.mixin.ToggleKeyAccessor)key;
        try {
            nativeOption.set(false);access.neon$resetToggle();key.setDown(true);key.setDown(false);
            check(key.isDown(),module+" native input remains latched after release");key.setDown(true);
            check(!key.isDown(),module+" second press releases the native input");
            key.setDown(true);access.neon$releaseToggle();
            check(!key.isDown()&&!((ToggleKeyMapping)key).shouldRestoreStateOnScreenClosed(),module+" menus release without automatic resuming by default");
            Neon.config.sprintResume=Neon.config.sneakResume=true;key.setDown(true);access.neon$releaseToggle();
            check(((ToggleKeyMapping)key).shouldRestoreStateOnScreenClosed(),module+" optional resume uses vanilla restoration");
            key.setDown(true);me.xchro.neon.input.MovementToggles.tick(mc);module.enabled(Neon.config,false);me.xchro.neon.input.MovementToggles.tick(mc);
            check(!key.isDown(),module+" disabling clears latched movement");
            key.setDown(true);key.setDown(false);check(!key.isDown(),module+" disabled restores hold behavior");
            check(!nativeOption.get(),module+" leaves the vanilla preference unchanged");
        } finally { access.neon$resetToggle();nativeOption.set(before);module.enabled(Neon.config,true);Neon.config.sprintResume=Neon.config.sneakResume=false; }
    }
    private void capture(ClientModule module) {
        pending=true;
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{
            try(image) {
                if(phase<2&&module==ClientModule.CROSSHAIR) {
                    int x=mc.getWindow().getWidth()/2,y=mc.getWindow().getHeight()/2;
                    check((image.getPixel(x+5,y)&0xffffff)==0x18e5bc,"custom crosshair reaches the real framebuffer at GUI scale "+mc.getWindow().getGuiScale());
                    check((image.getPixel(x+5,y+1)&0xffffff)!=0x18e5bc,"reticle thickness stays in display pixels");
                }
                image.writeToFile(mc.gameDirectory.toPath().resolve("screenshots/neon-module-"+module.name().toLowerCase(Locale.ROOT)+"-"+phase+".png"));done=true;
            } catch(Throwable error) { failure=error.toString(); }
        });
    }
    boolean run() throws Exception {
        if(!crosshairChecked) {
            if(crosshairAudit==null)crosshairAudit=new CrosshairSmoke(mc,checks);
            if(!crosshairAudit.run())return false;checks=crosshairAudit.checks();crosshairChecked=true;
        }
        if(failure!=null)throw new AssertionError(failure);
        if(index>=modules.size()) { Neon.applyConfig(original);mc.gui.setScreen(null);return true; }
        var module=modules.get(index);
        if(age++==0)setup(module);
        if(age<12)return false;
        if(!pending) { verify(module);capture(module);return false; }
        if(!done)return false;
        age=0;pending=done=false;if(++phase==4) { phase=0;index++; }return false;
    }
}
