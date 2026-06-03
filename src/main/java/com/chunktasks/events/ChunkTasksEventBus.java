package com.chunktasks.events;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Singleton
@Slf4j
public class ChunkTasksEventBus {

    @Inject
    private ClientThread clientThread;

    private final Map<String, List<Consumer<Object>>> subscribers = new HashMap<>();

    public void subscribe(ChunkTasksEventType eventType, Consumer<Object> handler)
    {
        subscribers
                .computeIfAbsent(eventType.displayText(), key -> new ArrayList<>())
                .add(handler);
    }

    public void publish(ChunkTasksEventType eventType, Object data)
    {
        log.warn("publishing");
        List<Consumer<Object>> handlers = subscribers.get(eventType.displayText());

        if (handlers == null)
        {
            log.warn("no handlers found");
            return;
        }

        for (Consumer<Object> handler : handlers)
        {
            log.warn("handler found!");
            clientThread.invokeLater(() -> handler.accept(data));
        }
    }
}
