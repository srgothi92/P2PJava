package messages;

/**
 * This class extends Message and provides Choke type request for choking nodes.
 */
public class Choke extends Message {

    public Choke() {
        super (Type.Choke);
    }
}
