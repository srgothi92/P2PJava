package io;

import main.StartRemotePeers;
import messages.Handshake;
import messages.Message;
import messages.Type;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.util.logging.Logger;


public class SerializableInputStream extends DataInputStream implements ObjectInput {

    private boolean isHandshakeDone = false;

    public SerializableInputStream(InputStream in) {
        super(in);
    }

    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());
    @Override
    public Object readObject() throws ClassNotFoundException, IOException {
        if (isHandshakeDone) {
            final int length = readInt();
            final int payloadLength = length - 1; // subtract 1 for the message type
            Message message = Message.getInstance(payloadLength, Type.valueOf (readByte()));
            message.read(this);
            return message;
        }
        else {
            Handshake handshake = new Handshake();
            LOGGER.info("Reading handshake object on input stream");
            handshake.read(this);
            isHandshakeDone = true;
            return handshake;
        }
    }
}
