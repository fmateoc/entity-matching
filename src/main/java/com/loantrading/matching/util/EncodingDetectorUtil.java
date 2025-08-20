package com.loantrading.matching.util;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncodingDetectorUtil {

    private static final Logger logger = LoggerFactory.getLogger(EncodingDetectorUtil.class);
    private static final int MIN_CONFIDENCE = 50; // Minimum confidence level to accept detected charset

    /**
     * Detects the charset of a byte array.
     *
     * @param data The byte array to detect the charset from.
     * @return The detected Charset, or UTF-8 as a fallback.
     */
    public static Charset detectCharset(byte[] data) {
        if (data == null || data.length == 0) {
            return StandardCharsets.UTF_8;
        }

        CharsetDetector detector = new CharsetDetector();
        detector.setText(data);
        CharsetMatch match = detector.detect();

        if (match != null && match.getConfidence() >= MIN_CONFIDENCE) {
            logger.debug("Detected charset: {} with confidence {}", match.getName(), match.getConfidence());
            try {
                return Charset.forName(match.getName());
            } catch (Exception e) {
                logger.warn("Could not get Charset for name {}, falling back to UTF-8", match.getName(), e);
                return StandardCharsets.UTF_8;
            }
        } else {
            logger.debug("Could not detect charset with sufficient confidence, falling back to UTF-8");
            return StandardCharsets.UTF_8;
        }
    }
}
