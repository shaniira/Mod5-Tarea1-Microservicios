package com.backendseguros.quotation.interfaceadapters.out.time;

import com.backendseguros.quotation.usecases.port.out.time.ClockPort;
import java.time.Instant;

public class SystemClockAdapter implements ClockPort {
    public Instant now() {
        return Instant.now();
    }
}
