package com.andinaseguros.claims.interfaceadapters.out.time;

import com.andinaseguros.claims.usecases.port.out.time.ClockPort;
import java.time.Instant;

public class SystemClockAdapter implements ClockPort {
    public Instant now() {
        return Instant.now();
    }
}
