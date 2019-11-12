package messages;

import java.util.BitSet;

/**
 * This class extends Message and provides bitfield as payload for bitfiled type request.
 */
public class Bitfield extends Message {

    Bitfield (byte[] bitfield) {
        super (Type.BitField, bitfield);
    }

    public Bitfield (BitSet bitset) {
        super (Type.BitField, bitset.toByteArray());
    }

    public BitSet getBitSet() {
        return BitSet.valueOf (_payload);
    }
}
