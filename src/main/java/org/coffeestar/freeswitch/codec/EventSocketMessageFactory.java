package org.coffeestar.freeswitch.codec;

import org.coffeestar.freeswitch.EventSocketMessage;

import java.util.Map;

import static java.util.Collections.unmodifiableMap;

/**
 * Factory to create {@link EventSocketMessage EventSocketMessages}.
 */
class EventSocketMessageFactory {
    EventSocketMessage buildWithoutBody(Map<String, String> headers) {
        return new EventSocketMessage(unmodifiableMap(headers));
    }

    EventSocketMessage buildWithBody(Map<String, String> headers, String body) {
        return new EventSocketMessage(unmodifiableMap(headers), body);
    }
}
