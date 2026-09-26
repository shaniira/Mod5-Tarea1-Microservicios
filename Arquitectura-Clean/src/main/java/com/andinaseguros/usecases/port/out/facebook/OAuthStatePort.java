package com.andinaseguros.usecases.port.out.facebook;

public interface OAuthStatePort {
    String create();

    boolean consume(String state);
}