package messages;

/**
 * This class extends Message and provides Interested type request to notify peers they have piece that node wants.
 */
public class Completed extends Message {

    public Completed() {
        super (Type.Completed);
    }
}
