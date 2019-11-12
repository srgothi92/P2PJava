package messages;

/**
 * This class extends Message and provides Not Interested type request to notify peers they dont have piece that node wants.
 */
public class NotInterested  extends Message {

    public NotInterested() {
        super (Type.NotInterested);
    }
}
