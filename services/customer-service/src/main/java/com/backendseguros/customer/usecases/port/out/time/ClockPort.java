package com.backendseguros.customer.usecases.port.out.time;

import java.time.Instant;

public interface ClockPort {
    Instant now();
}
