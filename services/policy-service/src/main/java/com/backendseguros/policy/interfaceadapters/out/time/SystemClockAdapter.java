package com.backendseguros.policy.interfaceadapters.out.time;

import com.backendseguros.policy.usecases.port.out.time.ClockPort;
import java.time.Instant;

public class SystemClockAdapter implements ClockPort {
    public Instant now() {
        return Instant.now();
    }
}
