package com.backendseguros.notification.interfaceadapters.out.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppTextResponse(boolean success, String message, Map<String, Object> data) {}
