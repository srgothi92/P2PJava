package messages;

import io.Serializable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import main.StartRemotePeers;

/**
 * Base Class for creating all types of Messages
 */
public class Message implements Serializable {

    private int _length;
    private final Type _type;
    protected byte[] _payload;
    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());

    protected Message (Type type) {
        this (type, null);
    }

    protected Message (Type type, byte[] payload) {
        _length = (payload == null ? 0 : payload.length)
                + 1; // for the _type
        _type = type;
        _payload = payload;
    }

    public Type getType() {
        return _type;
    }

    @Override
    public void read (DataInputStream in) throws IOException {
        if ((_payload != null) && (_payload.length) > 0) {
            in.readFully(_payload, 0, _payload.length);
        }
    }

    @Override
    public void write (DataOutputStream out) throws IOException {
        out.writeInt (_length);
        out.writeByte (_type.getValue());
        if ((_payload != null) && (_payload.length > 0)) {
            out.write (_payload, 0, _payload.length);
        }
    }

    public static Message getInstance (int length, Type type) throws ClassNotFoundException, IOException {
        switch (type) {
            case Choke:
                return new Choke();

            case Unchoke:
                return new Unchoke();

            case Interested:
                return new Interested();

            case NotInterested:
                return new NotInterested();

            case Have:
                return new Have (new byte[length]);

            case BitField:
                return new Bitfield (new byte[length]);

            case Request:
                return new Request (new byte[length]);

            case Piece:
                return new Piece (new byte[length]);
            case Completed:
                return new Completed ();
            default:
                throw new ClassNotFoundException ("message type not handled: " + type.toString());
        }
    }
}
