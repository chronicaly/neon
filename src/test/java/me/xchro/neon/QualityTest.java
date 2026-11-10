package me.xchro.neon;

import me.xchro.neon.core.*;
import me.xchro.neon.config.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class QualityTest {
    @TempDir Path directory;
    @Test void unityColorFormatsAndTransparencyRemainCompatible() {
        assertEquals(0xffaabbcc,ColorValue.parse("#abc").orElseThrow());
        assertEquals(0xcc162233,ColorValue.parse("#162233CC").orElseThrow());
        assertEquals(0x04112233,ColorValue.parse("17,34,51,4").orElseThrow());
        assertEquals(0xff00ff00,ColorValue.parse("hsb:120,100,100").orElseThrow());
        assertEquals(0xffff5555,ColorValue.parse("red").orElseThrow());
        for(String invalid:List.of("#12345","300,0,0","hsb:361,1,1","oops")) assertTrue(ColorValue.parse(invalid).isEmpty());
        var legacy=ConfigStore.decode("{\"theme\":{\"background\":1122867}}");
        assertEquals(0xff112233,legacy.theme.background);
        var transparent=ConfigStore.decode("{\"theme\":{\"alphaEnabled\":true,\"background\":1122867}}");
        assertEquals(0x00112233,ConfigStore.copy(transparent).theme.background);
    }
    @Test void armorDefaultsBesideHotbarWithoutMovingExistingPlacements() {
        assertEquals(NeonConfig.ArmorAnchor.HOTBAR_LEFT,new NeonConfig().armorAnchor);
        var old=ConfigStore.decode("{\"armor\":{\"x\":0.3,\"y\":0.6}}");
        assertEquals(NeonConfig.ArmorAnchor.CUSTOM,old.armorAnchor);
        assertEquals(.3,old.armor.x); assertEquals(.6,old.armor.y);
    }
    @Test void rapidSnapshotsCoalesceAndFlushIsAnOrderedDurabilityBarrier() throws Exception {
        var written = new ArrayList<String>(); var threads = new HashSet<String>();
        try (var queue = new QueuedSave((path, json) -> { threads.add(Thread.currentThread().getName()); written.add(json); Files.writeString(path, json); }, (p,e) -> fail(e))) {
            Path path = directory.resolve("config.json");
            var first = queue.submit(path, "first"); queue.submit(path, "middle"); var last = queue.submit(path, "last");
            assertFalse(last.isDone()); queue.flush().get(3, TimeUnit.SECONDS);
            first.join(); last.join(); assertEquals(List.of("last"), written); assertEquals("last", Files.readString(path));
            queue.submit(path, "last"); queue.flush().join(); assertEquals(1, written.size());
            assertEquals(Set.of("Neon settings writer"), threads);
        }
    }
    @Test void failureReportsAndRetainsUnsavedSnapshotForRetry() throws Exception {
        var failWrite = new AtomicBoolean(true); var failures = new AtomicInteger();
        try (var queue = new QueuedSave((path,json) -> { if (failWrite.get()) throw new java.io.IOException("locked"); Files.writeString(path,json); }, (p,e) -> failures.incrementAndGet())) {
            Path path = directory.resolve("config.json"); var saved = queue.submit(path, "latest");
            assertThrows(CompletionException.class, () -> queue.flush().join());
            assertTrue(saved.isCompletedExceptionally()); assertTrue(queue.hasPending()); assertEquals(1, failures.get());
            failWrite.set(false); queue.flush().join(); assertFalse(queue.hasPending()); assertEquals("latest", Files.readString(path));
        }
    }
    @Test void closeFlushesDebounceAndEditsDuringWriteRemainOrdered() throws Exception {
        var started = new CountDownLatch(1); var release = new CountDownLatch(1); var writes = new ArrayList<String>();
        var queue = new QueuedSave((p,s) -> {
            if (s.equals("one")) { started.countDown(); try { release.await(3, TimeUnit.SECONDS); } catch (InterruptedException e) { throw new java.io.IOException(e); } }
            writes.add(s);
        }, (p,e) -> fail(e));
        Path p = directory.resolve("c"); queue.submit(p, "one"); var flushing = queue.flush(); assertTrue(started.await(3, TimeUnit.SECONDS));
        var latest = queue.submit(p, "two"); release.countDown(); flushing.join(); queue.close(); latest.join(); queue.flush().join(); assertEquals(List.of("one", "two"), writes);
    }
    @Test void debounceWritesWithoutNeedingAnExplicitClose() throws Exception {
        var written = new CountDownLatch(1); long start = System.nanoTime();
        try (var queue = new QueuedSave((p,s) -> written.countDown(), (p,e) -> fail(e))) {
            queue.submit(directory.resolve("c"), "snapshot"); assertTrue(written.await(3, TimeUnit.SECONDS));
            assertTrue(System.nanoTime() - start >= 200_000_000L);
        }
    }
    @Test void adaptiveIgnoresWarmupAndUsesSlowRecoveryWithMinimumDwell() {
        var a = new AdaptiveEffects(); long now = 1; double budget = AdaptiveEffects.budgetNanos(144, 120);
        assertEquals(1e9/120, budget); assertEquals(1e9/60, AdaptiveEffects.budgetNanos(60, 240));
        a.frame(now, budget, true, false);
        for (int i=0;i<40;i++) a.frame(now += 20_000_000, budget, true, false);
        assertEquals(0,a.tier());
        for (int i=0;i<120;i++) a.frame(now += 20_000_000, budget, true, false);
        assertEquals(1,a.tier()); long first=now;
        for (int i=0;i<100;i++) a.frame(now += 20_000_000, budget, true, false);
        assertEquals(1,a.tier());
        for (int i=0;i<60;i++) a.frame(now += 20_000_000, budget, true, false);
        assertEquals(2,a.tier()); assertTrue(now-first>=3_000_000_000L);
        for(int i=0;i<500;i++) a.frame(now+=8_340_000,budget,true,false);
        assertEquals(2,a.tier());
        for(int i=0;i<120;i++) a.frame(now+=8_340_000,budget,true,false);
        assertEquals(1,a.tier());
        a.frame(now+1,budget,true,true); assertEquals(0,a.tier());
    }
    @Test void animationRetargetsContinuouslyAndCannotOvershoot() {
        var value = new TimedValue(0); value.to(100, 1, 100_000_000L);
        double middle = value.value(50_000_001,100_000_000L);
        assertTrue(middle>0 && middle<100); assertEquals(middle,value.to(20,50_000_001,100_000_000L));
        for(long time=50_000_001;time<160_000_001;time+=1_000_000) assertTrue(value.value(time,100_000_000L)>=20 && value.value(time,100_000_000L)<=middle);
        assertEquals(20,value.value(160_000_001,100_000_000L));
    }
    @Test void effectsCompatibilityAndProfileRoundTrip() throws Exception {
        assertEquals(NeonConfig.Effects.ADAPTIVE,ConfigStore.decode("{}").theme.effects);
        assertEquals(NeonConfig.Effects.ADAPTIVE,ConfigStore.decode("{\"theme\":{\"effects\":null}}").theme.effects);
        var c=new NeonConfig(); c.theme.effects=NeonConfig.Effects.FULL; var profiles=new ProfileStore(directory.resolve("p.json"));
        profiles.initialize(c); profiles.put("Full",c); var reload=new ProfileStore(directory.resolve("p.json")); reload.initialize(new NeonConfig());
        assertEquals(NeonConfig.Effects.FULL,reload.get("Full").theme.effects);
    }
}
