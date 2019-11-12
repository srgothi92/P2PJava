package main;

import io.SerializableInputStream;
import io.SerializableOutputStream;

import java.io.IOException;
import java.net.Socket;
import java.util.BitSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import messages.Bitfield;
import messages.Handshake;
import messages.Message;
import messages.Request;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class will allow the local peer to download piece from the remote peer when unchoked by the remote peer.
 */
public class RemoteNode implements Runnable {
    private final int peerId;
    private final String ipAddress;
    private final int port;
    private final MessageManager oMsgManager;
    private Socket connection;
    private PeerManager oPeerManager;
    private FileManager oFileManager;
    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());
    private AtomicBoolean  isHandShakeDone = new AtomicBoolean(false);
    private final SerializableOutputStream out;
    private final SerializableInputStream in;
    private Boolean bIsLocal = false;
    private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();
	private int localPeerId;
    public RemoteNode(int localPeerId, int peerId, String ipAddress, int port, FileManager oFileManager, PeerManager oPeerManager, Socket remoteConnection, Boolean bIsLocal) throws IOException {
    	this.localPeerId = localPeerId;
        this.peerId = peerId;
        this.oFileManager = oFileManager;
        this.oPeerManager = oPeerManager;
        this.ipAddress = ipAddress;
        this.port = port;
        this.connection = remoteConnection;
        this.bIsLocal = bIsLocal;
		oMsgManager = new MessageManager(localPeerId, peerId, oPeerManager, oFileManager);
	    out = new SerializableOutputStream(connection.getOutputStream());
		in = new SerializableInputStream(connection.getInputStream());
    }

    /**
     * When unchoked, the following steps are performed:
     * 1.randomly select a piece that is not available with the local peer and
     * is not requested from any other peer and should be available with the remote peer.
     * 2.Request this piece from the remote peer
     * 3.When piece is available, repeat Step 1 until choked or not interested in the piece available with the remote peer.
     */
    @Override
    public void run() {
    	try {
    		// Start The Thread to continuously look for any message that needs to be send to Remote Node
    		new TrasnmitMessage().start();
        	// Send handshake
            LOGGER.info("Start handshake for peerid : " +peerId);
						out.writeObject(new Handshake(localPeerId));
    		LOGGER.info("Waiting for handshake for peerId : " +peerId);
    		Handshake rcvdHandshake = (Handshake) in.readObject();
    		//bHandShakeReceived = true;
    		LOGGER.info("Received handshake from peerId : " +peerId);
    		if(!bIsLocal && rcvdHandshake.getPeerId() != peerId){
    		    throw new Exception("Handshake unsuccessful : Incorrect peer id " +rcvdHandshake.getPeerId()+ " was received. Correct peer id is :" +peerId );
            }
    		isHandShakeDone.set(true);
    		// Send Bit Set
    		BitSet bitset = oFileManager.getReceivedPiece();
    		LOGGER.info("Sending BitField" + bitset + " to remote");
    		sendToSocket(oMsgManager.handle(rcvdHandshake));
    		// Start the Thread to continuously look for message that has arrived
    		new ReceiveMessage().start();
    	} catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
    	}
        catch (Exception e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
        }
    }
    
    public void sendMessage(Message message){
    	LOGGER.info("Add message " + message.getClass().getName() + " to queue");
    	messageQueue.add(message);
    }
    
    private synchronized void sendToSocket(Message message) throws IOException {
        if (message != null) {
        	LOGGER.info("Sending message" + message.getType() + "to peer" + peerId);
            out.writeObject(message);
            switch (message.getType()) {
                case Request: {
                    new java.util.Timer().schedule(
                            new RequestTimer((Request) message, oFileManager, out, message, peerId),
                            Config.UnchokingInterval * 2000
                    );
                }
            }
        }
    }
    
    public synchronized void sendCompleted(Message message) {
    	try {
    		LOGGER.info("Sending message Completed to peer" + peerId);
    		out.writeObject(message);
    	} catch (IOException e) {
    		LOGGER.log(Level.INFO, e.getMessage(), e);
    	}
    }
    
    private class TrasnmitMessage extends Thread {
    	 private boolean isRemoteChoked = true;
    	 @Override
         public void run() {
             while (true) {
                 try {
                	 // Blocking Queue wait for the queue to become non-empty when retrieving an element,
                	 //and wait for space to become available in the queue when storing an element.
                	 // So we can run this thread in infinite loop without putting thread.sleep
                     final Message message = messageQueue.take();
                     if (isHandShakeDone.get()) {
                         switch (message.getType()) {
                             case Choke: {
                                 if (!isRemoteChoked) {
                                     isRemoteChoked = true;
                                     sendToSocket(message);
                                 }
                                 break;
                             }

                             case Unchoke: {
                                 if (isRemoteChoked) {
                                     isRemoteChoked = false;
                                     sendToSocket(message);
                                 }
                                 break;
                             }

                             default:
                                 sendToSocket(message);
                         }
                     } else {
                         LOGGER.info("Cannot send message" + message.getType() + " because Handshaking is not done");
                     }
                 } catch (IOException | InterruptedException ex) {
                	 LOGGER.log(Level.INFO, ex.getMessage(), ex);
                	 break;
                 }
             }
         }
    }
    
    private class ReceiveMessage extends Thread {
   	 @Override
        public void run() {
            while (true) {
                try {
                	Message oMsg = oMsgManager.readMessage((Message) in.readObject());
                	if(oMsg != null){
                		LOGGER.info("Sending message " + oMsg.getType() + "to peer id: " + peerId);
                		sendToSocket(oMsg);
                    }
                } catch (IOException | ClassNotFoundException ex) {
                	LOGGER.log(Level.INFO, ex.getMessage(), ex);
                	break;
                }
            }
        }
   }
}
