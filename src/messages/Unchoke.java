package messages;

/**
 * This class extends Message and provides UnChoke type request for un-choking nodes.
 */
public class Unchoke extends Message {

    public Unchoke() {
        super (Type.Unchoke);
    }
}
