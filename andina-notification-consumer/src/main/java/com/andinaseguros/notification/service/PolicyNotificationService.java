package com.andinaseguros.notification.service;

import com.andinaseguros.notification.contact.ClienteContact;
import com.andinaseguros.notification.contact.ClienteContactService;
import com.andinaseguros.notification.contract.PolicyIssuedMessage;
import com.andinaseguros.notification.notification.NotificationMessage;
import com.andinaseguros.notification.notification.NotificationPort;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PolicyNotificationService {
    private final ClienteContactService contactService;
    private final NotificationPort notificationPort;

    public PolicyNotificationService(
            ClienteContactService contactService, NotificationPort notificationPort) {
        this.contactService = contactService;
        this.notificationPort = notificationPort;
    }

    public void notifyPolicyIssued(PolicyIssuedMessage message) {
        validate(message);
        ClienteContact contact = contactService.buscarPorId(message.data().customerId());
        if (contact.telefono() == null || contact.telefono().isBlank()) {
            throw new IllegalStateException("El cliente no tiene telefono: " + contact.clienteId());
        }

        String policyNumber = message.data().policyNumber();
        notificationPort.enviar(
                new NotificationMessage(
                        contact.telefono(),
                        "Su poliza " + policyNumber + " fue emitida correctamente.",
                        Map.of(
                                "policyId", message.data().policyId().toString(),
                                "customerId", message.data().customerId().toString(),
                                "policyNumber", policyNumber,
                                "customerName", contact.nombre())));
    }

    private void validate(PolicyIssuedMessage message) {
        if (message == null || message.eventId() == null || message.data() == null) {
            throw new IllegalArgumentException("PolicyIssuedMessage invalido");
        }
        if (!"PolicyIssued".equals(message.eventType()) || message.eventVersion() != 1) {
            throw new IllegalArgumentException("Version o tipo de evento no soportado");
        }
        if (message.data().customerId() == null || message.data().policyNumber() == null) {
            throw new IllegalArgumentException("Datos de cliente o poliza incompletos");
        }
    }
}
