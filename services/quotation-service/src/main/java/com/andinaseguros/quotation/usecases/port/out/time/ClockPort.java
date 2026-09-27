package com.andinaseguros.quotation.usecases.port.out.time;

import java.time.Instant;

public interface ClockPort {
    Instant now();
}
