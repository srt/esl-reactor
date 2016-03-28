package org.coffeestar.freeswitch;

import java.io.Serializable;
import java.util.Map;

/**
 *
 */
public class EventSocketMessage implements Serializable {
    private static final long serialVersionUID = 0L;
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_LENGTH = "Content-Length";
    private final Map<String, String> headers;
    private final String body;

    public EventSocketMessage(Map<String, String> headers) {
        this(headers, null);
    }

    public EventSocketMessage(Map<String, String> headers, String body) {
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

}
