package dev.frostguard.engine.helper;

import java.io.IOException;

import org.junit.jupiter.api.Assumptions;
import org.opencv.core.Mat;

import dev.frostguard.vision.match.OpenCvPatternLocator;

final class OpenCvTestSupport {

    private OpenCvTestSupport() {}

    static void assumeOpenCvAvailable() throws IOException {
        try {
            OpenCvPatternLocator.loadOpenCvNative();
            new Mat().release();
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ex) {
            Assumptions.assumeTrue(false,
                    "OpenCV native test binding is unavailable on this platform: " + ex.getMessage());
        }
    }
}
