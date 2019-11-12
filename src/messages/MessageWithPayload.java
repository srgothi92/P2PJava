package messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;


public class MessageWithPayload extends Message 
{
    protected MessageWithPayload (Type type, byte[] payload) {
        super (type, payload);
    }

    public int getPieceIndex() {
        return ByteBuffer.wrap(Arrays.copyOfRange(_payload, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    protected static byte[] getPieceIndexBytes (int pieceIdx) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pieceIdx).array();
    }
}
