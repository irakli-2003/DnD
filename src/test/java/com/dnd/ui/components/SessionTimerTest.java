package com.dnd.ui.components;

import org.junit.Test;

import static org.junit.Assert.*;

public class SessionTimerTest {

    @Test
    public void formatsUnderAnHourAsMinutesAndSeconds() {
        assertEquals("00:00", SessionTimer.format(0));
        assertEquals("00:09", SessionTimer.format(9));
        assertEquals("01:00", SessionTimer.format(60));
        assertEquals("59:59", SessionTimer.format(3599));
    }

    @Test
    public void formatsAnHourAndOverWithAnHourField() {
        assertEquals("1:00:00", SessionTimer.format(3600));
        assertEquals("2:05:07", SessionTimer.format(2 * 3600 + 5 * 60 + 7));
    }

    @Test
    public void negativeDurationsClampToZeroRatherThanRenderingNonsense() {
        assertEquals("00:00", SessionTimer.format(-30));
    }
}
