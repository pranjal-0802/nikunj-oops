package com.iitsm.infrastructure.notification;

import com.iitsm.application.port.RealtimeNotificationPort;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Registry managing active Server-Sent Event (SSE) client connections per recipient.
 */
@Component
public class SseEmitterRegistry implements RealtimeNotificationPort {

    // ConcurrentHashMap chosen deliberately for thread-safe concurrent connection mapping
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String recipientId) {
        SseEmitter emitter = new SseEmitter(1800000L); // 30 minutes timeout
        emitters.computeIfAbsent(recipientId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(recipientId, emitter));
        emitter.onTimeout(() -> removeEmitter(recipientId, emitter));
        emitter.onError(e -> removeEmitter(recipientId, emitter));

        return emitter;
    }

    @Override
    public void sendNotification(String recipientId, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(recipientId);
        if (userEmitters != null && !userEmitters.isEmpty()) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    SseEmitter.SseEventBuilder event = SseEmitter.event().data(data);
                    if (eventName != null) {
                        event = event.name(eventName);
                    }
                    emitter.send(event);
                } catch (IOException e) {
                    removeEmitter(recipientId, emitter);
                }
            }
        }
    }

    public void sendToUser(String recipientId, Object data) {
        List<SseEmitter> userEmitters = emitters.get(recipientId);
        if (userEmitters != null && !userEmitters.isEmpty()) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().data(data));
                } catch (IOException e) {
                    removeEmitter(recipientId, emitter);
                }
            }
        }
    }

    private void removeEmitter(String recipientId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(recipientId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(recipientId);
            }
        }
    }
}

