package dev.frostguard.engine.helper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.frostguard.vision.match.OpenCvPatternLocator;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Guards the platform-aware OpenCV native loading contract.
 *
 * <p>The bundled {@code opencv_java4110.dll} only loads on Windows, so a build
 * or test run on any other operating system used to die with
 * {@link UnsatisfiedLinkError}. {@link OpenCvPatternLocator#loadNativeLibrary()}
 * now picks the native image that matches the host, which is what allows the
 * vision and OCR regression suites to run on Linux CI runners while the shipped
 * Windows bundle keeps using its own DLL.</p>
 */
class OpenCvNativeLoadingTest {

    @Test
    void loadsAnOpenCvNativeImageOnWhicheverPlatformTheBuildRunsOn() {
        assertDoesNotThrow(OpenCvPatternLocator::loadNativeLibrary,
                "OpenCV must load on the current platform, not only on Windows");

        // Allocating a Mat crosses the JNI boundary, so it only succeeds when a
        // native image really was bound rather than merely resolved on disk.
        Mat probe = new Mat(4, 6, CvType.CV_8UC3);
        try {
            assertEquals(4, probe.rows());
            assertEquals(6, probe.cols());
            assertTrue(Core.VERSION.startsWith("4."),
                    "Unexpected OpenCV version: " + Core.VERSION);
        } finally {
            probe.release();
        }
    }

    @Test
    void repeatedLoadsAreIdempotent() {
        assertDoesNotThrow(OpenCvPatternLocator::loadNativeLibrary);
        // A second call must not attempt to bind the library again; System.load
        // would throw if the same image were loaded twice in one JVM.
        assertDoesNotThrow(OpenCvPatternLocator::loadNativeLibrary);
    }

    @Test
    void theWindowsNativeImageStaysBundledForTheShippedDesktopArchive() throws Exception {
        assertEquals("/native/opencv/opencv_java4110.dll",
                OpenCvPatternLocator.WINDOWS_NATIVE_RESOURCE,
                "The Windows bundle resolves the DLL from this classpath location");

        try (InputStream dll = OpenCvPatternLocator.class
                .getResourceAsStream(OpenCvPatternLocator.WINDOWS_NATIVE_RESOURCE)) {
            assertNotNull(dll, "The Windows OpenCV DLL must remain on the fg-vision classpath");
        }
    }
}
