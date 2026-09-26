package com.andinaseguros.notification.notification;

import com.andinaseguros.notification.config.WhatsAppProperties;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class NotificationConfiguration {
    @Bean
    RestClient whatsappRestClient(WhatsAppProperties properties) {
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    WhatsAppClient whatsappClient(RestClient whatsappRestClient, WhatsAppProperties properties) {
        return new WhatsAppClient(whatsappRestClient, properties);
    }

    @Bean
    NotificationPort notificationPort(WhatsAppClient client) {
        return new WhatsAppNotificationAdapter(client);
    }
}
