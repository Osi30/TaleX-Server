package com.talex.server.services;

public interface IMessagePublisherService {
    void publishInteractionEvent(String message);
}
