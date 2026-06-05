package tong.statmod.profiling;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfilerTest {

    @Test
    void getReportLines_returnsImmutableView() {
        Map<String, String> report = Profiler.getReportLines();

        assertThrows(UnsupportedOperationException.class, () -> report.put("x", "y"));
    }
}
