package messages;

import java.util.Arrays;

/**
 * This class extends Message and provides piece type request to send content to peers.
 */
public class Piece extends MessageWithPayload {

    Piece (byte[] payload) {
        super (Type.Piece, payload);
    }

    public Piece (int pieceIdx, byte[] content) {
        super (Type.Piece, join (pieceIdx, content));
    }

    public byte[] getContent() {
        if ((_payload == null) || (_payload.length <= 4)) {
            return null;
        }
        return Arrays.copyOfRange(_payload, 4, _payload.length);
    }
    /**
     * Concat 4 byte piece index and piece for payload
     */
    private static byte[] join (int pieceIdx, byte[] pieceBytes) {
        byte[] payload = new byte[4 + (pieceBytes == null ? 0 : pieceBytes.length)];
        System.arraycopy(getPieceIndexBytes (pieceIdx), 0, payload, 0, 4);
        System.arraycopy(pieceBytes, 0, payload, 4, pieceBytes.length);
        return payload;
    }
}
