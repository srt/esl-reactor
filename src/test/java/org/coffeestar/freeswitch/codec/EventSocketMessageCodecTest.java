package org.coffeestar.freeswitch.codec;

import org.coffeestar.freeswitch.messages.EventSocketMessage;
import org.junit.Before;
import org.junit.Test;
import reactor.io.buffer.Buffer;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class EventSocketMessageCodecTest {
    private EventSocketMessageCodec codec;
    private EventSocketMessage message;
    private Function<Buffer, EventSocketMessage> decoder;

    @Before
    public void setUp() throws Exception {
        codec = new EventSocketMessageCodec();
        message = null;
        decoder = codec.decoder(m -> message = m);
    }

    @Test
    public void testIndexOfDoubleLineFeed() {
        final Buffer buffer = Buffer.wrap("Hello\n\nWorld\n\n");
        assertThat(buffer.indexOf((byte) 0x0A), is(6));
        assertThat(codec.indexOfDoubleLineFeed(buffer), is(7));
    }

    @Test
    public void testDecodeMap() {
        final Buffer buffer = Buffer.wrap("ZContent-Type: command/reply\n" +
                "Reply-Text: +OK event listener enabled plain\n" +
                "Foo: Foo: Bar").position(1);

        final Map<String, String> map = codec.decodeMap(buffer, UTF_8.newDecoder());
        assertThat(map, hasEntry("Content-Type", "command/reply"));
        assertThat(map, hasEntry("Reply-Text", "+OK event listener enabled plain"));
        assertThat(map, hasEntry("Foo", "Foo: Bar"));
        assertThat(map.entrySet(), hasSize(3));
    }

    @Test
    public void canDecodeMessagesWithoutBody() {
        final Buffer buffer = Buffer.wrap("Content-Type: auth/request\n" +
                "\n" +
                "Content-Type: command/reply\n" +
                "Reply-Text: +OK accepted\n" +
                "\n" +
                "Content-Type: command/reply\n" +
                "Reply-Text: +OK event listener enabled plain\n" +
                "\n");
        decoder.apply(buffer);
        assertThat(message.getContentType(), is("auth/request"));
        decoder.apply(buffer);
        assertThat(message.getContentType(), is("command/reply"));
        assertThat(message.getHeader("Reply-Text"), is("+OK accepted"));
        decoder.apply(buffer);
        assertThat(message.getContentType(), is("command/reply"));
        assertThat(message.getHeader("Reply-Text"), is("+OK event listener enabled plain"));
    }

    @Test
    public void canDecodeMessagesWithBody() {
        final Consumer<EventSocketMessage> consumer = m -> message = m;
        final Function<Buffer, EventSocketMessage> decoder = codec.decoder(consumer);
        final Buffer buffer = Buffer.wrap("Content-Length: 544\n" +
                "Content-Type: text/event-plain\n" +
                "\n" +
                "Event-Name: API\n" +
                "Core-UUID: 0949fea6-c28a-4617-8971-4b2eedda20a7\n" +
                "FreeSWITCH-Hostname: 0ee129f361c7\n" +
                "FreeSWITCH-Switchname: 0ee129f361c7\n" +
                "FreeSWITCH-IPv4: 172.17.0.13\n" +
                "FreeSWITCH-IPv6: %3A%3A1\n" +
                "Event-Date-Local: 2016-03-25%2023%3A08%3A58\n" +
                "Event-Date-GMT: Fri,%2025%20Mar%202016%2023%3A08%3A58%20GMT\n" +
                "Event-Date-Timestamp: 1458947338267698\n" +
                "Event-Calling-File: switch_loadable_module.c\n" +
                "Event-Calling-Function: switch_api_execute\n" +
                "Event-Calling-Line-Number: 2537\n" +
                "Event-Sequence: 199908\n" +
                "API-Command: sofia_contact\n" +
                "API-Command-Argument: */1502%40reucon.com\n" +
                "\n" +
                "Content-Length: 545\n" +
                "Content-Type: text/event-plain\n" +
                "\n" +
                "Event-Name: API\n" +
                "Core-UUID: 0949fea6-c28a-4617-8971-4b2eedda20a7\n" +
                "FreeSWITCH-Hostname: 0ee129f361c7\n" +
                "FreeSWITCH-Switchname: 0ee129f361c7\n" +
                "FreeSWITCH-IPv4: 172.17.0.13\n" +
                "FreeSWITCH-IPv6: %3A%3A1\n" +
                "Event-Date-Local: 2016-03-25%2023%3A08%3A58\n" +
                "Event-Date-GMT: Fri,%2025%20Mar%202016%2023%3A08%3A58%20GMT\n" +
                "Event-Date-Timestamp: 1458947338267698\n" +
                "Event-Calling-File: switch_loadable_module.c\n" +
                "Event-Calling-Function: switch_api_execute\n" +
                "Event-Calling-Line-Number: 2537\n" +
                "Event-Sequence: 199909\n" +
                "API-Command: sofia_contact\n" +
                "API-Command-Argument: */brian%40reucon.com\n" +
                "\n");
        decoder.apply(buffer);
        assertThat(message.getContentType(), is("text/event-plain"));
        assertThat(message.getContentLength(), is(544));
        System.out.println(message);
        decoder.apply(buffer);
        assertThat(message.getContentType(), is("text/event-plain"));
        assertThat(message.getContentLength(), is(545));
        System.out.println(message);
    }
}