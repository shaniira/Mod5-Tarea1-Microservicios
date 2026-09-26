package com.andinaseguros.notification.contact;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ClienteContactService {
    private final ClienteContactRepository repository;

    public ClienteContactService(ClienteContactRepository repository) {
        this.repository = repository;
    }

    public ClienteContact buscarPorId(UUID clienteId) {
        return repository.findById(clienteId.toString())
                .map(document -> new ClienteContact(
                        clienteId,
                        nombreCompleto(document.nombres, document.apellidos),
                        document.correo,
                        document.telefono))
                .orElseThrow(() -> new IllegalStateException("Cliente no encontrado: " + clienteId));
    }

    private String nombreCompleto(String nombres, String apellidos) {
        return ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos)).trim();
    }
}
