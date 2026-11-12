package com.studin.app.platform.focus;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Java bridge over the legacy {@link UsageStatsManager} API (roadmap: Java for
 * isolated Android API bridges). Requires the user-granted Usage Access special
 * permission; every method degrades to null/empty when it is missing.
 * Results are approximations – event availability differs between OEMs.
 */
public final class UsageStatsBridge {

    private UsageStatsBridge() {}

    /** Package most recently moved to the foreground within {@code lookbackMs}, or null. */
    @Nullable
    public static String currentForegroundPackage(@NonNull Context context, long lookbackMs) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return null;
        long end = System.currentTimeMillis();
        long start = end - Math.max(1_000L, lookbackMs);
        try {
            UsageEvents events = usm.queryEvents(start, end);
            if (events == null) return null;
            UsageEvents.Event event = new UsageEvents.Event();
            String latest = null;
            long latestTime = 0L;
            int resumed = Build.VERSION.SDK_INT >= 29
                    ? UsageEvents.Event.ACTIVITY_RESUMED
                    : legacyMoveToForeground();
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                if (event.getEventType() == resumed && event.getTimeStamp() >= latestTime) {
                    latest = event.getPackageName();
                    latestTime = event.getTimeStamp();
                }
            }
            return latest;
        } catch (SecurityException | IllegalStateException e) {
            return null;
        }
    }

    /** Foreground time per package between two wall-clock instants (ms). */
    @NonNull
    public static Map<String, Long> foregroundTimeBetween(@NonNull Context context, long start, long end) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null || end <= start) return Collections.emptyMap();
        try {
            Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(start, end);
            if (stats == null || stats.isEmpty()) return Collections.emptyMap();
            Map<String, Long> out = new HashMap<>();
            for (Map.Entry<String, UsageStats> e : stats.entrySet()) {
                long t = e.getValue().getTotalTimeInForeground();
                if (t > 0) out.put(e.getKey(), t);
            }
            return out;
        } catch (SecurityException | IllegalStateException e) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("deprecation")
    private static int legacyMoveToForeground() {
        return UsageEvents.Event.MOVE_TO_FOREGROUND;
    }
}
