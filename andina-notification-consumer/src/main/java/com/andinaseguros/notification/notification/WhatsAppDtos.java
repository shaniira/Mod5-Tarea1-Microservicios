package com.andinaseguros.notification.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

record WhatsAppTextRequest(String number, String text) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record WhatsAppTextResponse(boolean success, String message, Map<String, Object> data) {}
