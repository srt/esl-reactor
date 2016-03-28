package org.coffeestar.freeswitch.messages;

import java.io.Serializable;

/**
 *
 */
public class EventSocketCommand implements Serializable {
    private static final long serialVersionUID = 0L;
    private String command;

    public EventSocketCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "EventSocketCommand{" +
                "command='" + command + '\'' +
                '}';
    }
}
