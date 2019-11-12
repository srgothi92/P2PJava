package messages;

/**
 * This class extends Message and provides request type request to request particular piece from peers.
 */
public class Request extends MessageWithPayload {

    Request (byte[] pieceIdx) {
        super (Type.Request, pieceIdx);
    }

    public Request (int pieceIdx) {
        this (getPieceIndexBytes (pieceIdx));
    }
}
