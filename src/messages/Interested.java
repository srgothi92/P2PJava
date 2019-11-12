package messages;

/**
 * This class extends Message and provides Interested type request to notify peers they have piece that node wants.
 */
public class Interested  extends Message {

    public Interested() {
        super (Type.Interested);
    }
}
