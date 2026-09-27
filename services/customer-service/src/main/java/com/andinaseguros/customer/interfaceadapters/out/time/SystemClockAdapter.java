package com.andinaseguros.customer.interfaceadapters.out.time;

import com.andinaseguros.customer.usecases.port.out.time.ClockPort;
import java.time.Instant;

public class SystemClockAdapter implements ClockPort {
    public Instant now() {
        return Instant.now();
    }
}
