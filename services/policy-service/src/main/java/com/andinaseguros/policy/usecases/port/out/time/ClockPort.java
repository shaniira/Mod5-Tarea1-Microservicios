package com.andinaseguros.policy.usecases.port.out.time;

import java.time.Instant;

public interface ClockPort {
    Instant now();
}
