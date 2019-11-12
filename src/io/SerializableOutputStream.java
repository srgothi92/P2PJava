package io;

import main.StartRemotePeers;
import messages.Handshake;
import messages.Message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.logging.Logger;


public class SerializableOutputStream extends DataOutputStream implements ObjectOutput {

    public SerializableOutputStream(OutputStream out) {
        super(out);
    }

    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());
    @Override
    public void writeObject (Object obj) throws IOException {

        if (obj instanceof Handshake) {
            LOGGER.info("Sending handshake object on output stream");
            ((Handshake) obj).write(this);
        }
        else if (obj instanceof Message) {

            LOGGER.info("Sending message object on output stream");
            ((Message) obj).write (this);
        }
        else if (obj instanceof Serializable) {
            throw new UnsupportedOperationException ("Message of type " + obj.getClass().getName() + " not yet supported.");
        }
        else {
            throw new UnsupportedOperationException ("Message of type " + obj.getClass().getName() + " not supported.");
        }
    }
}
