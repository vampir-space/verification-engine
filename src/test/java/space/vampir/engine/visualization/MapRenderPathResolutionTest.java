package space.vampir.engine.visualization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapRenderPathResolutionTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesSiblingFilesRelativeToFilesystemConfigDirectory() throws Exception {
        Path configFile = tempDir.resolve("config.json");
        Path siblingSvg = tempDir.resolve("map.svg");
        Files.writeString(configFile, "{}");
        Files.writeString(siblingSvg, "<svg/>");

        Path configDirectory = invokePath("resolveConfigDirectory", configFile.toString());
        assertEquals(tempDir, configDirectory);

        URL resolved = invokeUrl("resolveReferenceUrl", "map.svg", configDirectory, null);
        assertNotNull(resolved);
        assertEquals(siblingSvg.toUri().toURL(), resolved);
    }

    @Test
    void resolvesRelativeClasspathFilesUsingConfigParentFolder() throws Exception {
        String configResourceDirectory = invokeString("resolveConfigResourceDirectory", "/BME_Town_medium/BME_Town_medium.json");
        assertEquals("BME_Town_medium", configResourceDirectory);

        URL resolvedWithPlainRelativePath = invokeUrl("resolveReferenceUrl", "BME_Town_medium.svg", null, configResourceDirectory);
        URL resolvedWithLeadingSlash = invokeUrl("resolveReferenceUrl", "/BME_Town_medium.svg", null, configResourceDirectory);
        URL resolvedWithBackslash = invokeUrl("resolveReferenceUrl", "\\BME_Town_medium.svg", null, configResourceDirectory);

        assertNotNull(resolvedWithPlainRelativePath);
        assertNotNull(resolvedWithLeadingSlash);
        assertNotNull(resolvedWithBackslash);
        assertTrue(resolvedWithPlainRelativePath.toString().contains("/BME_Town_medium/BME_Town_medium.svg"));
        assertTrue(resolvedWithLeadingSlash.toString().contains("/BME_Town_medium/BME_Town_medium.svg"));
        assertTrue(resolvedWithBackslash.toString().contains("/BME_Town_medium/BME_Town_medium.svg"));
    }

    private static Path invokePath(String methodName, String argument) throws Exception {
        Method method = MapRender.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        return (Path) method.invoke(null, argument);
    }

    private static String invokeString(String methodName, String argument) throws Exception {
        Method method = MapRender.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, argument);
    }

    private static URL invokeUrl(String methodName, String rawPath, Path configDirectory, String configResourceDirectory) throws Exception {
        Method method = MapRender.class.getDeclaredMethod(methodName, String.class, Path.class, String.class);
        method.setAccessible(true);
        return (URL) method.invoke(null, rawPath, configDirectory, configResourceDirectory);
    }
}

