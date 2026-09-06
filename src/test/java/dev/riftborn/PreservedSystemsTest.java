package dev.riftborn;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PreservedSystemsTest {
    @Test void overworldBladeMobsAndTemplatesStayByteIdentical() throws Exception {
        var expected = JsonParser.parseString(Files.readString(Path.of("src/test/resources/preserved-1.0-systems.json"))).getAsJsonObject();
        for (var file : expected.entrySet()) {
            String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(file.getKey()))));
            assertEquals(file.getValue().getAsString(), actual, "A protected 1.0 system changed: " + file.getKey());
        }
    }
}
