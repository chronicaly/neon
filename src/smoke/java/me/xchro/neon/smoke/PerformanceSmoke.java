package me.xchro.neon.smoke;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import me.xchro.neon.Neon;
import me.xchro.neon.ui.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.input.*;

/** Deterministic production-JAR benchmark. All instrumentation lives in the test-only mod. */
public final class PerformanceSmoke {
    private static final String[] CASES = {"gameplay", "motion-blur", "idle-menu", "navigation-scroll", "color-edit", "hud-placement", "font-churn", "first-open", "drawer-reversals", "category-switch", "continuous-color", "menu-resize"};
    private static final int ONLY = Arrays.asList(CASES).indexOf(System.getProperty("neon.perf.scenario", ""));
    private static final int WARM = 240, SAMPLES = 480;
    private static final com.sun.management.ThreadMXBean CPU = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static PerformanceSmoke active;
    private static int depth;
    private static long scopeCpu, scopeAlloc, scopeWall, cpu, wall, allocated, uploads, copies, passes;
    private final StringBuilder csv = new StringBuilder("run,scenario,frame,frame_ms,render_ms,neon_cpu_us,neon_scope_us,neon_alloc_bytes,upload_bytes,copy_pixels,render_passes,event\n");
    private String event="";
    private MouseButtonEvent gesture;
    private double originX,originY;
    private int tick, stage = -1, frame;
    private long previous, start;
    private boolean ready, done;
    private Minecraft mc;
    public void start() {
        if (System.getProperty("neon.perf.scenario") != null && ONLY < 0) throw new IllegalArgumentException("Unknown performance scenario");
        try {
            Path jar = Path.of(Neon.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            String hash = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
            if (Boolean.getBoolean("neon.perf.baseline") && !hash.equals("478646c2d7e0b4a145cf99a5ddf35fcf48d4df532e4397bf255b1310687d1150")) throw new AssertionError("Wrong baseline JAR: " + jar + " " + hash);
            String expected = System.getProperty("neon.perf.expectedHash");
            if (expected != null && !hash.equalsIgnoreCase(expected)) throw new AssertionError("Wrong comparison JAR: " + hash);
            System.out.println("NEON_PERF_JAR " + jar + " SHA256=" + hash);
            Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("neon-perf-" + System.getProperty("neon.perf.label", "after") + "-jar.txt"), jar + "\nSHA256=" + hash + "\n");
        } catch (Exception e) { throw new AssertionError(e); }
        active = this; ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }
    private void tick(Minecraft client) {
        mc = client;
        if (mc.player == null || mc.level == null || done) return;
        if (++tick == 1) {
            try { Files.writeString(mc.gameDirectory.toPath().resolve("config/neon-window.json"), "{\"layoutVersion\":4,\"customized\":false}"); }
            catch (Exception e) { throw new AssertionError(e); }
            mc.options.pauseOnLostFocus = false; mc.options.enableVsync().set(false); mc.options.framerateLimit().set(120);
            mc.options.menuBackgroundBlurriness().set(5);
            mc.options.renderDistance().set(6); mc.options.simulationDistance().set(5); mc.options.guiScale().set(2);
            mc.options.setCameraType(CameraType.FIRST_PERSON); mc.getWindow().setWindowed(1280, 800); mc.resizeGui();
            mc.gui.setScreen(null);
            mc.getSingleplayerServer().execute(() -> {
                var server = mc.getSingleplayerServer(); var p = server.getPlayerList().getPlayers().getFirst();
                p.teleportTo(.5, 101, .5); p.setInvulnerable(true);
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); p.setHealth(20); p.getFoodData().setFoodLevel(20); p.getInventory().clearContent();
                var slots = new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
                var items = new net.minecraft.world.item.Item[]{net.minecraft.world.item.Items.DIAMOND_HELMET, net.minecraft.world.item.Items.DIAMOND_CHESTPLATE, net.minecraft.world.item.Items.DIAMOND_LEGGINGS, net.minecraft.world.item.Items.DIAMOND_BOOTS};
                for (int i=0;i<4;i++) p.setItemSlot(slots[i], new net.minecraft.world.item.ItemStack(items[i]));
                p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
                p.getInventory().setItem(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
                for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=100;y<108;y++)
                    p.level().setBlockAndUpdate(new net.minecraft.core.BlockPos(x,y,z),(y==100?net.minecraft.world.level.block.Blocks.STONE:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                server.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, server);
                server.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER, false, server);
            });
        }
        if (tick == 240) { ready = true; next(); }
    }
    public static void begin() {
        if (active == null || !active.ready || active.done) return;
        if (depth++ == 0) { scopeWall = System.nanoTime(); scopeCpu = CPU.getCurrentThreadCpuTime(); scopeAlloc = CPU.getThreadAllocatedBytes(Thread.currentThread().threadId()); }
    }
    public static void end() {
        if (active == null || !active.ready || active.done) return;
        if (--depth == 0) { cpu += CPU.getCurrentThreadCpuTime() - scopeCpu; allocated += CPU.getThreadAllocatedBytes(Thread.currentThread().threadId()) - scopeAlloc; wall += System.nanoTime() - scopeWall; }
    }
    public static void upload(int w, int h, int bytes) { if (depth > 0) uploads += (long) w * h * bytes; }
    public static void copy(int w, int h) { if (depth > 0) copies += (long) w * h; }
    public static void pass() { if (depth > 0) passes++; }
    public static void frameStart() {
        if (active == null || !active.ready || active.done) return;
        active.start = System.nanoTime(); active.step();
    }
    public static void frameEnd() {
        if (active == null || !active.ready || active.done) return;
        active.record();
        // Include Neon tick/input work between rendered frames; never include the harness's own allocations.
        cpu = wall = allocated = uploads = copies = passes = 0; depth = 0;
    }
    private List<AbstractWidget> widgets(ContainerEventHandler root) {
        List<AbstractWidget> result = new ArrayList<>();
        for (var child : root.children()) { if (child instanceof AbstractWidget w) result.add(w); if (child instanceof ContainerEventHandler c) result.addAll(widgets(c)); }
        return result;
    }
    private void press(String text) { widgets(mc.gui.screen()).stream().filter(w -> w instanceof Button && w.getMessage().getString().equals(text)).map(w -> (Button) w).findFirst().orElseThrow().onPress(null); }
    private void next() {
        do { stage++; } while (stage < CASES.length * 3 && ONLY >= 0 && stage % CASES.length != ONLY);
        if (stage >= CASES.length * 3) { finish(); return; }
        frame = 0; previous = 0;gesture=null;
        Neon.config = new me.xchro.neon.config.NeonConfig();
        Neon.config.armorAnchor = me.xchro.neon.config.NeonConfig.ArmorAnchor.CUSTOM;
        Neon.config.totemFloating = true;
        mc.gui.setScreen(null); mc.player.setYRot(0); mc.player.setXRot(0);
        int scenario = stage % CASES.length;
        if (scenario == 1) Neon.config.motionBlurEnabled = true;
        if (scenario >= 2 && scenario <= 4) {
            mc.gui.setScreen(Menus.fresh(null));
            if (scenario == 3) { press("Visuals"); Menus.configure((SettingsScreen)mc.gui.screen(),"Zoom"); }
            if (scenario == 4) { press("Settings"); press("Colors"); press("Outline"); }
        }
        if (scenario == 5) mc.gui.setScreen(new HudEditor(null));
        if (scenario == 6) mc.gui.setScreen(new PixelScreen(net.minecraft.network.chat.Component.literal("Font verification")) {
            @Override protected void renderPixels(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta) {
                Ui.window(g,100,100,440,280); Ui.text(g,"Value " + (stage * 1000 + frame),120,120,Ui.TEXT);
            }
        });
        if(scenario>=7){mc.gui.setScreen(Menus.fresh(null));if(scenario==10){press("Settings");press("Colors");press("Outline");}}
        System.out.println("NEON_PERF run=" + (stage / CASES.length + 1) + " scenario=" + CASES[scenario]);
    }
    private void step() {
        // Synthetic screen events bypass the native input callback; keep the benchmark out of AFK throttling.
        mc.getFramerateLimitTracker().onInputReceived();
        int scenario = stage % CASES.length;
        event="";
        mc.player.setYRot(scenario == 1 ? (float) (Math.sin(frame * .007) * 20) : 0); mc.player.setXRot(0);
        // Allow the attached color drawer to become a pointer target before the first gesture.
        if((scenario==4||scenario==10)&&frame<60)return;
        if(scenario>=7){premiumStep(scenario);return;}
        if (frame % 8 != 0) return;
        if (scenario == 3) {
            var screen = (PixelScreen) mc.gui.screen(); var list = widgets(screen);
            var scroll = list.stream().filter(w -> w instanceof AbstractScrollArea).map(w -> (AbstractScrollArea) w).findFirst().orElseThrow();
            screen.mouseScrolled(screen.guiCoordinate(scroll.getX() + 12), screen.guiCoordinate(scroll.getY() + 12), 0, (frame / 64) % 2 == 0 ? -1 : 1);
        } else if (scenario == 4) {
            var screen = (PixelScreen) mc.gui.screen(); var shade = widgets(screen).stream().filter(w -> w.getMessage().getString().equals("Saturation and brightness")).findFirst().orElseThrow();
            double x = screen.guiCoordinate(shade.getX() + 5 + frame / 8 % Math.max(1, shade.getWidth() - 10));
            double y = screen.guiCoordinate(shade.getY() + shade.getHeight() / 2.0);
            var event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)); screen.mouseClicked(event, false); screen.mouseReleased(event);
        } else if (scenario == 5) {
            var b = me.xchro.neon.hud.NeonHud.bounds(mc.gui.screen().width, mc.gui.screen().height, true);
            var e = new MouseButtonEvent(b.x() + b.width() / 2.0, b.y() + b.height() / 2.0, new MouseButtonInfo(0, 0));
            mc.gui.screen().mouseClicked(e, false);
            var move = new MouseButtonEvent(e.x() + ((frame / 64) % 2 == 0 ? -2 : 2), e.y(), e.buttonInfo());
            mc.gui.screen().mouseDragged(move, move.x() - e.x(), 0); mc.gui.screen().mouseReleased(move);
        }
    }
    private void premiumStep(int scenario) {
        var screen=(SettingsScreen)mc.gui.screen();
        if(scenario==7&&frame%120==0){
            event="open";begin();try{mc.gui.setScreen(null);Ui.releaseFonts();mc.gui.setScreen(Menus.fresh(null));}finally{end();}
        }else if(scenario==8&&frame%12==0){
            event="reverse";begin();try{Menus.configure(screen,"Freelook");screen.mouseReleased(new MouseButtonEvent(-100,-100,new MouseButtonInfo(1,0)));}finally{end();}
        }else if(scenario==9&&frame%12==0){
            event="category";String label=new String[]{"Combat","Settings","Visuals"}[frame/12%3];
            var target=widgets(screen).stream().filter(w->w instanceof Button&&w.getMessage().getString().equals(label)).map(w->(Button)w).findFirst().orElseThrow();
            begin();try{target.onPress(null);}finally{end();}
        }else if(scenario==10){
            var shade=widgets(screen).stream().filter(w->w.getMessage().getString().equals("Saturation and brightness")).findFirst().orElseThrow();
            double x=screen.guiCoordinate(shade.getX()+3+(shade.getWidth()-6)*(.5+.45*Math.sin(frame*.04)));
            double y=screen.guiCoordinate(shade.getY()+3+(shade.getHeight()-6)*(.5+.45*Math.cos(frame*.03)));
            var next=new MouseButtonEvent(x,y,new MouseButtonInfo(0,0));event="color-drag";
            begin();try{if(gesture==null)screen.mouseClicked(next,false);else screen.mouseDragged(next,x-gesture.x(),y-gesture.y());if(frame%120==119){screen.mouseReleased(next);gesture=null;}else gesture=next;}finally{end();}
        }else if(scenario==11){
            if(gesture==null){var b=screen.expandedBounds();originX=screen.guiCoordinate(b.right()-1);originY=screen.guiCoordinate(b.bottom()-1);gesture=new MouseButtonEvent(originX,originY,new MouseButtonInfo(0,0));begin();try{screen.mouseClicked(gesture,false);}finally{end();}}
            double wave=Math.pow(Math.sin((frame%240)*Math.PI/239),2),x=originX-screen.guiCoordinate(120*wave),y=originY-screen.guiCoordinate(70*wave);
            var next=new MouseButtonEvent(x,y,new MouseButtonInfo(0,0));event="resize";
            begin();try{screen.mouseDragged(next,x-gesture.x(),y-gesture.y());if(frame%240==239){screen.mouseReleased(next);gesture=null;}else gesture=next;}finally{end();}
        }
    }
    private void record() {
        long now = System.nanoTime();
        if (frame >= WARM && previous != 0) csv.append(String.format(Locale.ROOT, "%d,%s,%d,%.6f,%.6f,%.3f,%.3f,%d,%d,%d,%d,%s%n", stage / CASES.length + 1, CASES[stage % CASES.length], frame - WARM,
            (start - previous) / 1e6, (now - start) / 1e6, cpu / 1e3, wall / 1e3, allocated, uploads, copies, passes,event));
        previous = start;
        if (++frame == WARM + SAMPLES) next();
    }
    private void finish() {
        done = true;
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("neon-perf-" + System.getProperty("neon.perf.label", "after") + ".csv"), csv);
            Files.writeString(mc.gameDirectory.toPath().resolve("neon-verification.txt"), "PASS performance: 3 warmed runs, " + (ONLY >= 0 ? 1 : CASES.length) + " scenarios, 480 frames each\n");
        } catch (Exception e) { throw new RuntimeException(e); }
        mc.stop();
    }
}
