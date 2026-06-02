package simplexity.simplenicks.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Thin bridge over Paper's region based schedulers so SimpleNicks runs cleanly on
 * both regular Paper and region threaded servers such as Folia and Canvas.
 * <p>
 * Every place that used to call {@code Bukkit.getScheduler()} now goes through one
 * of these helpers: database work lands on the async scheduler, anything that
 * touches a player (display name, tab name, feedback) lands on that player's own
 * region thread, and the one repeating bookkeeping task lands on the global region
 * scheduler.
 */
public final class FoliaScheduler {

    private FoliaScheduler() {
    }

    /**
     * Runs work away from the main path, e.g. database reads and writes.
     */
    public static void async(@NotNull Plugin plugin, @NotNull Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    /**
     * Runs work on the region thread that currently owns the given player or entity.
     * If the entity is gone before the task fires the task is simply dropped.
     */
    public static void onEntity(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
        entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
    }

    /**
     * Schedules a repeating task on the global region thread and hands back a handle
     * that can be cancelled. The global scheduler refuses a zero initial delay, so
     * anything below one tick is bumped up to one.
     */
    public static ScheduledTask globalTimer(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks, long periodTicks) {
        long delay = Math.max(1L, delayTicks);
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), delay, periodTicks);
    }
}
