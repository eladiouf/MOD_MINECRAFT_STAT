package tong.statmod.balance;

import tong.statmod.STATMod;
import tong.statmod.io.ReportFiles;
import tong.statmod.stats.*;
import java.io.File;
import java.io.IOException;

public class BalanceBenchmark {

    public static void run() {
        StringBuilder report = new StringBuilder();
        report.append("STAT Mod Balance Benchmark\n");
        report.append("==========================\n\n");

        int[] testLevels = {0, 25, 50, 75, 100};

        report.append("--- Damage Output (Brute Force + Blade Technique) ---\n");
        report.append("BF Level | BT Level | Total Bonus | Effective DPS\n");
        for (int bf : testLevels) {
            for (int bt : testLevels) {
                float bonus = StatCalculator.getDamageBonus(bf) + StatCalculator.getBladeDamageBonus(bt);
                float dps = 100f * (1f + bonus);
                report.append(String.format("%8d | %8d | %10.1f%% | %14.1f\n",
                    bf, bt, bonus * 100, dps));
            }
        }

        report.append("\n--- Survivability (Endurance + Resistance) ---\n");
        report.append("END Level | RES Level | Hearts | Reduction | Effective HP\n");
        for (int end : testLevels) {
            for (int res : testLevels) {
                float hearts = 20f + StatCalculator.getEnduranceHearts(end);
                float reduction = StatCalculator.getDamageReduction(res);
                float ehp = hearts / (1f - reduction);
                report.append(String.format("%9d | %9d | %5.1f | %9.1f%% | %13.1f\n",
                    end, res, hearts, reduction * 100, ehp));
            }
        }

        report.append("\n--- Critical Hit Analysis ---\n");
        report.append("Precision Level | Crit Chance | Avg DPS Multiplier\n");
        for (int prec : testLevels) {
            float crit = StatCalculator.getCritChance(prec);
            float dpsMult = 1f + crit;
            report.append(String.format("%14d | %10.1f%% | %17.2f\n",
                prec, crit * 100, dpsMult));
        }

        report.append("\n--- Recommended Breakpoints ---\n");
        report.append("Brute Force 50: +10% damage\n");
        report.append("Blade Technique 100: +15% damage\n");
        report.append("Physical Resistance 50: -15% damage taken\n");
        report.append("Precision 80: +24% crit chance\n");
        report.append("Endurance 100: +20 hearts\n");

        try {
            ReportFiles.writeUtf8(new File("config/statmod/balance-report.txt"), report.toString());
            STATMod.LOGGER.info("Balance benchmark report saved to config/statmod/balance-report.txt");
        } catch (IOException e) {
            STATMod.LOGGER.error("Failed to write balance report", e);
        }
    }
}
