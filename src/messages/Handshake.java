package messages;

import io.Serializable;
import main.StartRemotePeers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.logging.Logger;


/**
 * This class extends Message and provides Handshake type request during initial connection.
 */
public class Handshake implements Serializable {
    private final static String handshakeHeader = "P2PFILESHARINGPROJ";
    private final byte[] zeroBits = new byte[10];
    private byte[] peerId = new byte[4];
    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());

    public Handshake() {
    }

    public Handshake (int peerId) {
    	this.peerId =  (ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(peerId).array());
    }

//    private Handshake (byte[] peerId){
//        int i = 0;
//        //TODO: int to byte
//        for (byte b : peerId) {
//            this.peerId[i++] = b;
//        }
//    }

    @Override
    public void write(DataOutputStream outStream) throws IOException {
        byte[] handshakeHeaderBytes = handshakeHeader.getBytes(Charset.forName("US-ASCII"));
        outStream.write (handshakeHeaderBytes, 0, handshakeHeaderBytes.length);
        outStream.write(zeroBits, 0, zeroBits.length);
        outStream.write(peerId, 0, peerId.length);
        LOGGER.info("Completed writing the handshake object on input stream");
    }

    @Override
    public void read (DataInputStream inputStream) throws IOException {
        // Read and check handshake header
        byte[] rcvhandshakeHeader = new byte[handshakeHeader.length()];
        byte[] receivedZeroBits = new byte[zeroBits.length];
        byte[] receivedPeerId = new byte[peerId.length];
        LOGGER.info("Read finally the handshake object on input stream");
        inputStream.read(rcvhandshakeHeader, 0, handshakeHeader.length());
        LOGGER.info("Completed reading the handshake object on input stream");
        LOGGER.info("Received Handshake header : " + new String(rcvhandshakeHeader, "US-ASCII"));
        if (!handshakeHeader.equals(new String(rcvhandshakeHeader, "US-ASCII"))) {
            throw new ProtocolException ("Invalid handshake header");
        }

        // Read and check zero bits
        inputStream.read(receivedZeroBits, 0, zeroBits.length);
        LOGGER.info("Received ZeroBits : " + new String(receivedZeroBits, "US-ASCII"));
        if (!Arrays.equals(receivedZeroBits, zeroBits)) {
            throw new ProtocolException ("Invalid zero bits");
        }
        LOGGER.info("Received Peer Id : " + receivedPeerId.toString());
        // Read and check peer id
        
        if (inputStream.read(receivedPeerId, 0, peerId.length) != peerId.length) {
            throw new ProtocolException ("Invalid peer id " + peerId);
        } else {
        	peerId = receivedPeerId;
        }

    }

    public int getPeerId() {
        return ByteBuffer.wrap(peerId).order(ByteOrder.BIG_ENDIAN).getInt();
    }

}
