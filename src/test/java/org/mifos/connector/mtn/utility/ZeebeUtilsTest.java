package org.mifos.connector.mtn.utility;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.mtn.MtnConnectorApplicationTests;

class ZeebeUtilsTest extends MtnConnectorApplicationTests {

    @DisplayName("Convert initial timer 'PT45S' to next power of 2 timer 'PT64S'")
    @Test
    void test_convert_45_seconds_to_64_seconds() {
        String initialTimer = "PT45S";

        String nextTimer = ZeebeUtils.getNextTimer(initialTimer);

        assertEquals("PT64S", nextTimer);
    }

    @DisplayName("Handle initial timer with value 'PT1S' (smallest valid input)")
    @Test
    void test_convert_1_second_to_2_seconds() {
        String initialTimer = "PT1S";

        String nextTimer = ZeebeUtils.getNextTimer(initialTimer);

        assertEquals("PT2S", nextTimer);
    }

}
