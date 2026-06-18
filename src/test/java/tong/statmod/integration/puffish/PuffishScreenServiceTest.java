package tong.statmod.integration.puffish;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PuffishScreenServiceTest {
    @Test
    void refreshesCategoriesBeforeOpeningScreen() {
        List<String> operations = new ArrayList<>();

        PuffishScreenService.open(new PuffishScreenGateway() {
            @Override
            public void refreshCategories() {
                operations.add("refresh");
            }

            @Override
            public void openScreen() {
                operations.add("open");
            }
        });

        assertEquals(List.of("refresh", "open"), operations);
    }
}
