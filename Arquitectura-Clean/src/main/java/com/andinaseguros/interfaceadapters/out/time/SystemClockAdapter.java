package com.andinaseguros.interfaceadapters.out.time;

import com.andinaseguros.usecases.port.out.time.ClockPort;
import java.time.Instant;

public class SystemClockAdapter implements ClockPort {
    public Instant now() {
        return Instant.now();
    }
}
