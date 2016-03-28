package org.coffeestar.freeswitch.codec;

import org.coffeestar.freeswitch.messages.EventSocketCommand;
import org.coffeestar.freeswitch.messages.Headers;
import org.coffeestar.freeswitch.messages.EventSocketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.io.buffer.Buffer;
import reactor.io.buffer.StringBuffer;
import reactor.io.codec.BufferCodec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@code Codec} for decoding data into {@link EventSocketMessage EventSocketMessages}.
 */
public class EventSocketMessageCodec extends BufferCodec<EventSocketMessage, EventSocketCommand> {
    private static final byte LF = (byte) 0x0A;
    private static final byte SPACE = (byte) 0x20;
    private static final byte COLON = (byte) 0x3A;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final EventSocketMessageFactory eventSocketMessageFactory = new EventSocketMessageFactory();

    public EventSocketMessageCodec() {
        super(null, (Supplier<CharsetDecoder>) ISO_8859_1::newDecoder);
    }

    protected int canDecodeNext(Buffer buffer, Object context) {
        final int endOfHeaders = indexOfDoubleLineFeed(buffer);
        if (endOfHeaders == -1) {
            return endOfHeaders;
        }
        final int headerLength = endOfHeaders - buffer.position();

        buffer.snapshot();
        buffer.limit(endOfHeaders - 2);
        final Map<String, String> headers = decodeMap(buffer, (CharsetDecoder) context);
        buffer.reset();

        final int contentLength = findContentLength(headers);

        final int result;
        if (contentLength == 0) {
            result = endOfHeaders;
        } else {
            final int messageLength = headerLength + contentLength;
            result = messageLength <= buffer.remaining() ? messageLength + buffer.position() : -1;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("canDecodeNext returns {}, position: {}, limit:{}, remaining: {}", result, buffer.position(), buffer.limit(), buffer.remaining());
        }

        return result;
    }

    @Override
    protected EventSocketMessage decodeNext(Buffer buffer, Object context) {
        final CharsetDecoder charsetDecoder = (CharsetDecoder) context;

        if (logger.isTraceEnabled()) {
            logger.trace("decodeNext called, position: {}, limit:{}, remaining: {}, state: {}", buffer.position(), buffer.limit(), buffer.remaining());
            logger.trace("Buffer content: '{}'", decodeString(buffer.duplicate(), charsetDecoder));
        }

        final int endOfHeaders = indexOfDoubleLineFeed(buffer);
        if (endOfHeaders == -1) {
            return null;
        }

        final int limit = buffer.limit();
        buffer.limit(endOfHeaders - 2);
        final Map<String, String> headers = decodeMap(buffer, charsetDecoder);
        // restore limit
        buffer.limit(limit);
        // skip double line feed
        buffer.skip(2);

        final int contentLength = findContentLength(headers);
        if (contentLength == 0) {
            final EventSocketMessage message = eventSocketMessageFactory.buildWithoutBody(headers);
            logger.trace("decodeNext returns a new EventSocketMessage without body: {}", message);
            return message;
        }

        // This should not happen because canDecodeNext() makes sure the full message is available
        if (contentLength > buffer.remaining()) {
            throw new IllegalStateException("contentLength > buffer.remaining()");
        }

        buffer.limit(buffer.position() + contentLength);
//        final byte[] body = decodeBytes(buffer);
        final String body = decodeString(buffer, charsetDecoder);
        // restore limit
        buffer.limit(limit);

        final EventSocketMessage message = eventSocketMessageFactory.buildWithBody(headers, body);
        logger.trace("decodeNext returns a new EventSocketMessage with body: {}", message);
        return message;
    }

    /**
     * Search the buffer and find the position of the first occurrence of two LF bytes.
     *
     * @return the position in the buffer or {@code -1} if not found
     */
    int indexOfDoubleLineFeed(Buffer buffer) {
        final ByteBuffer byteBuffer = buffer.byteBuffer();
        int position = byteBuffer.position();
        int limit = byteBuffer.limit();
        int pos = -1;
        byte previousByte = 0x00;
        while (byteBuffer.hasRemaining()) {
            final byte currentByte = byteBuffer.get();
            if (currentByte == LF && previousByte == LF) {
                pos = byteBuffer.position();
                break;
            }
            previousByte = currentByte;
        }
        byteBuffer.limit(limit);
        byteBuffer.position(position);
        return pos;
    }

    private byte[] decodeBytes(Buffer buffer) {
        final ByteBuffer byteBuffer = buffer.byteBuffer();
        byte[] b = new byte[byteBuffer.remaining()];
        byteBuffer.get(b);
        return b;
    }

    private String decodeString(Buffer buffer, CharsetDecoder charsetDecoder) {
        return decodeString(buffer.byteBuffer(), charsetDecoder);
    }

    private String decodeString(ByteBuffer byteBuffer, CharsetDecoder charsetDecoder) {
        try {
            return charsetDecoder.decode(byteBuffer).toString();
        } catch (CharacterCodingException e) {
            throw new IllegalStateException(e);
        }
    }

    Map<String, String> decodeMap(Buffer buffer, CharsetDecoder charsetDecoder) {
        final Map<String, String> result = new HashMap<>();
        final ByteBuffer byteBuffer = buffer.byteBuffer();

        ByteBuffer nameBuffer = byteBuffer.duplicate();
        ByteBuffer valueBuffer = null;

        while (byteBuffer.hasRemaining()) {
            final byte currentByte = byteBuffer.get();

            if (valueBuffer == null && currentByte == COLON) {
                nameBuffer.limit(byteBuffer.position() - 1);
                // skip next byte if it is a space
                if (byteBuffer.hasRemaining()) {
                    final byte b = byteBuffer.get();
                    if (b != SPACE) {
                        byteBuffer.position(byteBuffer.position() - 1);
                    }
                }
                valueBuffer = byteBuffer.duplicate();
            }

            if (valueBuffer != null && (currentByte == LF || !byteBuffer.hasRemaining())) {
                if (currentByte == LF) {
                    valueBuffer.limit(byteBuffer.position() - 1);
                } else {
                    valueBuffer.limit(byteBuffer.position());
                }

                result.put(decodeString(nameBuffer, charsetDecoder), decodeString(valueBuffer, charsetDecoder));

                nameBuffer = byteBuffer.duplicate();
                valueBuffer = null;
            }
        }

        return result;
    }

    /**
     * Checks a map of headers for a Content-Length header and returns its value if found.
     *
     * @param headers the map of headers to check
     * @return the value of the Content-Length header or {@code 0} if not found.
     * @throws IllegalStateException if the value is not a number
     */
    private int findContentLength(Map<String, String> headers) throws IllegalStateException {
        final String contentLengthString = headers.get(Headers.CONTENT_LENGTH);
        int contentLength = 0;
        if (contentLengthString != null) {
            try {
                contentLength = Integer.valueOf(contentLengthString);
            } catch (NumberFormatException e) {
                throw new IllegalStateException(String.format("Invalid Content-Length header: '%s' is not a number", contentLengthString));
            }
        }
        return contentLength;
    }

    @Override
    public Buffer apply(EventSocketCommand out) {
        return encode(out.getCommand(), UTF_8.newEncoder());
    }

    private Buffer encode(String s, CharsetEncoder charsetEncoder) {
        try {
            final ByteBuffer bb = charsetEncoder.encode(CharBuffer.wrap(s));
            return new StringBuffer().append(bb).append(LF).append(LF).flip();
        } catch (CharacterCodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
