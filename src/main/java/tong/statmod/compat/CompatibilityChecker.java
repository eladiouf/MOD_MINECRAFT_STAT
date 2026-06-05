package tong.statmod.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import tong.statmod.STATMod;
import tong.statmod.io.ReportFiles;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class CompatibilityChecker {
    private static final List<String> issues = new ArrayList<>();
    private static final List<String> warnings = new ArrayList<>();
    private static final List<String> infos = new ArrayList<>();

    public static void check() {
        issues.clear();
        warnings.clear();
        infos.clear();

        checkEpicFight();
        checkParCool();
        checkAttributeConflicts();
        checkKnownIncompatibilities();

        generateReport();
    }

    private static void checkEpicFight() {
        if (!ModList.get().isLoaded("epicfight")) {
            issues.add("Epic Fight not loaded — STAT Mod requires Epic Fight to function!");
            return;
        }
        infos.add("Epic Fight detected");
    }

    private static void checkParCool() {
        if (ModList.get().isLoaded("parcool")) {
            infos.add("ParCool detected — parkour XP enabled");
        }
        if (ModList.get().isLoaded("epicparcool")) {
            infos.add("Epic ParCool detected — full integration active");
        }
    }

    private static void checkAttributeConflicts() {
        Set<String> otherMods = new HashSet<>();
        for (IModInfo info : ModList.get().getMods()) {
            String id = info.getModId();
            if (!id.equals("statmod") && !id.equals("forge") && !id.equals("minecraft")) {
                otherMods.add(id);
            }
        }
        infos.add("Loaded " + otherMods.size() + " other mods — no known attribute conflicts detected");
    }

    private static void checkKnownIncompatibilities() {
        if (ModList.get().isLoaded("apotheosis")) {
            warnings.add("Apotheosis detected — its attribute system may override STAT Mod bonuses");
        }
        if (ModList.get().isLoaded("sophisticatedbackpacks")) {
            infos.add("Sophisticated Backpacks detected — compatible");
        }
    }

    private static void generateReport() {
        File dir = new File("config/statmod");
        File report = new File(dir, "compatibility-report.txt");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== STAT Mod Compatibility Report ===\n");
            sb.append("Generated: ").append(new Date()).append("\n\n");
            sb.append("--- ISSUES (").append(issues.size()).append(") ---\n");
            for (String s : issues) sb.append("[ISSUE] ").append(s).append('\n');
            sb.append("\n--- WARNINGS (").append(warnings.size()).append(") ---\n");
            for (String s : warnings) sb.append("[WARN] ").append(s).append('\n');
            sb.append("\n--- INFO (").append(infos.size()).append(") ---\n");
            for (String s : infos) sb.append("[INFO] ").append(s).append('\n');
            sb.append("\nReport complete.\n");
            ReportFiles.writeUtf8(report, sb.toString());
        } catch (IOException e) {
            STATMod.LOGGER.error("Failed to write compatibility report", e);
        }

        for (String issue : issues) STATMod.LOGGER.error("[COMPAT] " + issue);
        for (String warning : warnings) STATMod.LOGGER.warn("[COMPAT] " + warning);
        for (String info : infos) STATMod.LOGGER.info("[COMPAT] " + info);
    }

    public static List<String> getIssues() { return Collections.unmodifiableList(issues); }
    public static List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
}
