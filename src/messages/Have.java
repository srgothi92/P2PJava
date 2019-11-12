package messages;

/**
 * This class extends Message and provides Have type request for updating peers of newly received piece.
 */
public class Have extends MessageWithPayload {

    Have (byte[] pieceIdx) {
        super (Type.Have, pieceIdx);
    }

    public Have (int pieceIdx) {
        this (getPieceIndexBytes (pieceIdx));
    }
}
