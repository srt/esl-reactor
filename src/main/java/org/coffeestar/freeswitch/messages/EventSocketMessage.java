package org.coffeestar.freeswitch.messages;

import java.io.Serializable;
import java.util.Map;

import static org.coffeestar.freeswitch.messages.Headers.CONTENT_LENGTH;
import static org.coffeestar.freeswitch.messages.Headers.CONTENT_TYPE;

/**
 *
 */
public class EventSocketMessage implements Serializable {
    private static final long serialVersionUID = 0L;
    private final Map<String, String> headers;
    private final String body;

    public EventSocketMessage(Map<String, String> headers) {
        this(headers, null);
    }

    public EventSocketMessage(Map<String, String> headers, String body) {
        if (headers == null) {
            throw new IllegalArgumentException("Headers must not be null");
        }
        this.headers = headers;
        this.body = body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return getHeader(CONTENT_TYPE);
    }

    public int getContentLength() {
        final String s = getHeader(CONTENT_LENGTH);
        if (s == null) {
            return 0;
        }
        return Integer.valueOf(s);
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public String toString() {
        return "EventSocketMessage{" +
                "headers=" + headers +
                ", body='" + body + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventSocketMessage that = (EventSocketMessage) o;

        if (!headers.equals(that.headers)) return false;
        return body != null ? body.equals(that.body) : that.body == null;

    }

    @Override
    public int hashCode() {
        int result = headers.hashCode();
        result = 31 * result + (body != null ? body.hashCode() : 0);
        return result;
    }
}
