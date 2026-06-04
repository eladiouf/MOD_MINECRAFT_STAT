package tong.statmod.profiling;

import tong.statmod.STATMod;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Profiler {
    private static boolean enabled = false;
    private static final Map<String, Long> totalTime = new ConcurrentHashMap<>();
    private static final Map<String, Integer> callCount = new ConcurrentHashMap<>();
    private static final Map<String, Long> maxTime = new ConcurrentHashMap<>();
    private static final long REPORT_THRESHOLD_NS = 1_000_000L;

    public static void start(String handler) {
        if (!enabled) return;
        totalTime.putIfAbsent(handler, 0L);
        callCount.putIfAbsent(handler, 0);
    }

    public static void end(String handler, long startNs) {
        if (!enabled) return;
        long elapsed = System.nanoTime() - startNs;
        totalTime.merge(handler, elapsed, Long::sum);
        callCount.merge(handler, 1, Integer::sum);
        maxTime.merge(handler, elapsed, Long::max);
        if (elapsed > REPORT_THRESHOLD_NS) {
            STATMod.LOGGER.warn("[PROFILE] Slow handler {}: {}ms", handler, elapsed / 1_000_000);
        }
    }

    public static void toggle() {
        enabled = !enabled;
        if (enabled) {
            totalTime.clear();
            callCount.clear();
            maxTime.clear();
            STATMod.LOGGER.info("Profiling enabled");
        } else {
            STATMod.LOGGER.info("Profiling disabled — generating report");
            generateReport();
        }
    }

    public static boolean isEnabled() { return enabled; }

    private static void generateReport() {
        try (FileWriter fw = new FileWriter("config/statmod/perf-report.json")) {
            StringBuilder sb = new StringBuilder("{\n  \"handlers\": [\n");
            boolean first = true;
            for (String handler : totalTime.keySet()) {
                if (!first) sb.append(",\n");
                first = false;
                long total = totalTime.getOrDefault(handler, 0L);
                int count = callCount.getOrDefault(handler, 0);
                long max = maxTime.getOrDefault(handler, 0L);
                double avgMs = count > 0 ? (total / (double) count) / 1_000_000.0 : 0;
                sb.append(String.format("    {\"name\":\"%s\",\"calls\":%d,\"totalMs\":%.2f,\"avgMs\":%.3f,\"maxMs\":%.3f}",
                    handler, count, total / 1_000_000.0, avgMs, max / 1_000_000.0));
            }
            sb.append("\n  ]\n}");
            fw.write(sb.toString());
            STATMod.LOGGER.info("Performance report saved to config/statmod/perf-report.json");
        } catch (IOException e) {
            STATMod.LOGGER.error("Failed to write profiler report", e);
        }
    }

    public static Map<String, String> getReportLines() {
        Map<String, String> lines = new LinkedHashMap<>();
        for (String handler : totalTime.keySet()) {
            long total = totalTime.getOrDefault(handler, 0L);
            int count = callCount.getOrDefault(handler, 0);
            double avgMs = count > 0 ? (total / (double) count) / 1_000_000.0 : 0;
            lines.put(handler, String.format("%d calls, avg %.3fms, max %.3fms",
                count, avgMs, maxTime.getOrDefault(handler, 0L) / 1_000_000.0));
        }
        return lines;
    }
}
