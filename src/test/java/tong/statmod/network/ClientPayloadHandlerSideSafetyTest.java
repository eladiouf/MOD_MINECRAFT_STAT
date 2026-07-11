package tong.statmod.network;

import net.neoforged.api.distmarker.OnlyIn;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNull;

class ClientPayloadHandlerSideSafetyTest {

    @Test
    void payloadHandlerClassMustRemainLoadableDuringDedicatedServerRegistration() {
        assertNull(ClientPayloadHandler.class.getAnnotation(OnlyIn.class),
                "NetworkHandler creates method references to this class while the dedicated server registers payloads");
    }

    @Test
    void registrationHandlerBytecodeMustNotLinkMinecraftClientClasses() throws IOException {
        try (InputStream stream = ClientPayloadHandler.class
                .getResourceAsStream("ClientPayloadHandler.class")) {
            byte[] bytecode = stream.readAllBytes();
            String constants = new String(bytecode, StandardCharsets.ISO_8859_1);
            org.junit.jupiter.api.Assertions.assertFalse(constants.contains("net/minecraft/client/"),
                    "Dedicated-server registration loads this handler class");
        }
    }
}
