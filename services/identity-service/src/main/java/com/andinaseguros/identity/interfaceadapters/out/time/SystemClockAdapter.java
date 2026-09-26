package com.andinaseguros.identity.interfaceadapters.out.time;

import com.andinaseguros.identity.usecases.port.out.time.ClockPort;
import java.time.Instant;

public class SystemClockAdapter implements ClockPort {
    public Instant now() {
        return Instant.now();
    }
}
