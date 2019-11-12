package main;


import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;


/**
 * This class takes care of the received pieces and adds all the pieces to
 * check whether the file is completed or not.
 */

public class FileManager {

    private BitSet receivedPiece;
    private final BitSet requestedPiece;
    private final long timeout;
    private Destination destination;
    private final ArrayList<Listener> listeners = new ArrayList<Listener>();
    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());
    
   

    FileManager (int localPeerId) {
        receivedPiece = new BitSet (Config.BitSetSize);
        requestedPiece = new BitSet (Config.BitSetSize);
        destination = new Destination(localPeerId, Config.FileName);
        timeout = Config.UnchokingInterval*2 ;
    }


    public synchronized void addPiece(int pieceIdx, byte[] part) {
        final boolean isNewPiece = !receivedPiece.get(pieceIdx);
        receivedPiece.set (pieceIdx);
        LOGGER.info("Received part: " + pieceIdx + " it a newPiece " + isNewPiece);
        if (isNewPiece) {
            destination.writeByteArrayAsFilePart(part, pieceIdx);
            for (Listener listener : listeners) {
            	// Notify neighbors of the newly available piece by sending Have message.
                listener.pieceArrived (pieceIdx);
            }
        }
        boolean bFileCompleted = isFileCompleted();
        LOGGER.info("File Completed Status" + bFileCompleted);
        if (bFileCompleted) {
            destination.mergeFile(receivedPiece.cardinality());
            for (Listener listener : listeners) {
                listener.localCompletedDownload();
            }
        }
    }

    public synchronized BitSet getReceivedPiece() {

        return (BitSet) receivedPiece.clone();
    }
    

    public synchronized void setAllPieces()
    {
        for (int i = 0; i < Config.BitSetSize; i++) {
            receivedPiece.set(i, true);
        }
    }

    public synchronized int getNumberOfReceivedPieces() {

        return receivedPiece.cardinality();
    }


    public void registerListener (Listener listener) {

        listeners.add (listener);
    }

    byte[] getPiece (int partId) {

        byte[] piece = destination.getPartAsByteArray(partId);
        return piece;
    }

    public void splitFile(){

        destination.splitFile((int) Config.PieceSize);
    }


    public boolean isFileCompleted() {
    	LOGGER.info("BitSetSize during file Completed check" + Config.BitSetSize);
        for (int i = 0; i < Config.BitSetSize; i++) {
        	LOGGER.info("cardinality " + receivedPiece.cardinality());
            if (!receivedPiece.get(i)) {
                return false;
            }
        }
        return true;
    }
    
    synchronized int getPieceToRequest(BitSet requestabableParts) {
        requestabableParts.andNot(requestedPiece);
        if (!requestabableParts.isEmpty()) {
            final int partId = ThreadLocalRandom.current().nextInt(0, Config.BitSetSize + 1);
            requestedPiece.set(partId);

            new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        synchronized (requestedPiece) {
                            requestedPiece.clear(partId);
                        }
                    }
                },
                    timeout
            );
            return partId;
        }
        return -1;
    }


	synchronized public boolean hasPart(int pieceIndex) {
        return receivedPiece.get(pieceIndex);
    }
}
