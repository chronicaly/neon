package me.xchro.neon.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/** One ordered writer, immutable snapshots, per-file coalescing and explicit durability barriers. */
public final class QueuedSave implements AutoCloseable {
    @FunctionalInterface public interface Writer { void write(Path path, String snapshot) throws IOException; }
    private record Pending(String snapshot, List<CompletableFuture<Void>> completions) {}
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Neon settings writer"); t.setDaemon(true); return t;
    });
    private final Map<Path, Pending> pending = new LinkedHashMap<>();
    private final Map<Path, String> written = new HashMap<>();
    private final Map<Path, String> latest = new HashMap<>();
    private final Writer writer;
    private final BiConsumer<Path, Throwable> failure;
    private ScheduledFuture<?> timer;
    private boolean closed;
    private CompletableFuture<Void> closing;
    public QueuedSave(BiConsumer<Path, Throwable> failure) { this(ConfigStore::writeAtomically, failure); }
    public QueuedSave(Writer writer, BiConsumer<Path, Throwable> failure) { this.writer = writer; this.failure = failure; }
    public synchronized CompletableFuture<Void> submit(Path path, String snapshot) {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Settings writer is closed"));
        path = path.toAbsolutePath().normalize(); latest.put(path, snapshot);
        var result = new CompletableFuture<Void>();
        Pending old = pending.get(path);
        var completions = old == null ? new ArrayList<CompletableFuture<Void>>() : old.completions;
        completions.add(result); pending.put(path, new Pending(snapshot, completions));
        if (timer != null) timer.cancel(false);
        timer = executor.schedule(this::drain, 250, TimeUnit.MILLISECONDS);
        return result;
    }
    private void drain() {
        Map<Path, Pending> batch;
        synchronized (this) { batch = new LinkedHashMap<>(pending); pending.clear(); }
        Throwable failed = null;
        for (var entry : batch.entrySet()) {
            Path path = entry.getKey(); Pending item = entry.getValue();
            try {
                if (!item.snapshot.equals(written.get(path))) writer.write(path, item.snapshot);
                written.put(path, item.snapshot);
                item.completions.forEach(f -> f.complete(null));
            } catch (Exception error) {
                // Keep the newest unsaved state for an explicit flush or a subsequent edit to retry.
                synchronized (this) { pending.putIfAbsent(path, new Pending(item.snapshot, new ArrayList<>())); }
                item.completions.forEach(f -> f.completeExceptionally(error));
                failure.accept(path, error); failed = error;
            }
        }
        if (failed != null) throw new CompletionException(failed);
    }
    public synchronized CompletableFuture<Void> flush() {
        if (closed) return closing == null ? CompletableFuture.completedFuture(null) : closing;
        if (timer != null) { timer.cancel(false); timer = null; }
        return CompletableFuture.runAsync(this::drain, executor);
    }
    public synchronized boolean hasPending() { return !pending.isEmpty(); }
    public synchronized String latest(Path path) { return latest.get(path.toAbsolutePath().normalize()); }
    @Override public void close() {
        CompletableFuture<Void> finish;
        synchronized (this) { if (closed) return; closing = finish = flush(); closed = true; }
        try { finish.join(); } finally { executor.shutdown(); }
    }
}
