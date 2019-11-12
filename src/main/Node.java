package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import messages.*;

/**
 * This class represents a local node. It will take care of Instantiating PeerManager
 * Thread which will take care of selecting peers. It also Instantiates File manager to handle
 * Its own file.
 */

public class Node implements Runnable,Listener {
    private final String ipAddress;
    private final int port;
    private final boolean hasFile;
    private AtomicBoolean bfileCompleted = new AtomicBoolean(false);
    private PeerManager oPeerManager;
    private ArrayList<RemotePeerInfo> remotePeersInfo;
    private final AtomicBoolean bTerminate = new AtomicBoolean(false);
    private FileManager oFileManager;
    private HashMap<Integer, RemoteNode>  mapRemoteNode = new HashMap<Integer, RemoteNode>();
    public BitSet bitSet;
    private AtomicBoolean bNeighboursCompleted = new AtomicBoolean(false);
	private int peerId;
	public Timestamp timestamp = new Timestamp(System.currentTimeMillis());
	private HashMap<Integer, Boolean> mapCompletedNeig = new HashMap<Integer, Boolean>();
	private RemoteNode oLocalNode = null;
    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());

    public Node(int peerId, String address, int port, boolean hasFile, ArrayList<RemotePeerInfo> peerInfo) {
    	this.peerId = peerId;
        ipAddress = address;
        this.port = port;
        this.hasFile = hasFile;
        this.remotePeersInfo = peerInfo;
        init();
    }

    private void init() {
    	oFileManager = new FileManager(peerId);
    	oFileManager.registerListener(this);
    	LOGGER.info("Value of HasFile" + hasFile);
        if(hasFile){
            LOGGER.info("Setting all bitsize for the node who has complete file");
            oFileManager.splitFile();
            oFileManager.setAllPieces();
        }
        this.oPeerManager = new PeerManager(peerId, this.remotePeersInfo);
        this.oPeerManager.register(this);
        Thread peerManagerThread = new Thread(oPeerManager);
        peerManagerThread.setName(oPeerManager.getClass().getName());
        peerManagerThread.start();
        LOGGER.info("Start PeerManager ");

    }
    
    public void createRemoteConnection() {
    	//Try till all the peers are connected.
        while(mapRemoteNode.size() < remotePeersInfo.size() - 1) {
			for (RemotePeerInfo pInfo : remotePeersInfo) {
				Socket connection = null;
				try {
					// Should not connect to itself and already connected peer
					if (!(pInfo.getPeerId() == peerId) && !(mapRemoteNode.containsKey(pInfo.getPeerId()))) {

						connection = new Socket(pInfo.getIpAddress(), pInfo.getPort());
						LOGGER.info("[ " +timestamp+ " ]: Peer " +peerId+ " makes a connection to Peer " +pInfo.getPeerId() );
						RemoteNode oRemoteNode = new RemoteNode(peerId, pInfo.getPeerId(), pInfo.getIpAddress(), pInfo.getPort(), oFileManager, oPeerManager, connection, false);
						new Thread(oRemoteNode).start();
						LOGGER.info("Remote node peerid : " + pInfo.getPeerId() + " started");
						mapRemoteNode.put(pInfo.getPeerId(), oRemoteNode);
						// IF peer already has a file than update the map of completed neighbors
						if(pInfo.hasFile()) {
						   bfileCompleted.set(true);
						}
					}
				} catch (IOException e) {
					if (connection != null) {
						try {
							connection.close();
						} catch (IOException ex) {
							LOGGER.log(Level.INFO, ex.getMessage(), ex);
						}
					}
					LOGGER.log(Level.INFO, e.getMessage(), e);
				}
			}
			try{
				Thread.sleep(5);
			}
			catch(InterruptedException ex){
				LOGGER.log(Level.INFO, ex.getMessage(), ex);
			}
			LOGGER.info("Total Connected Peers: " + mapRemoteNode.size() + "Total Peers to connect: " + remotePeersInfo.size());

		}
    }
    
    private void startLocalNode(ServerSocket localSocket) throws IOException {
    	if(oLocalNode == null) {
    		oLocalNode = new RemoteNode(peerId, peerId, ipAddress, port, oFileManager, oPeerManager, localSocket.accept(), true);
			new Thread(oLocalNode).start();
    	}
    }
    /**
     * 1.Opens a socket connection on port
     * 2.Instantiates the peer manager to select the k preferred neighbours and one optimistically unchoked neighbour.
     * 3.Maintains a list of its own bitset
     * 4.Establishes connection to remote peers
     * 5.Informs remote peers about the new piece it received by sending haveRequest.
     */
    @Override
    public void run() {
    	try {
    		ServerSocket connection = new ServerSocket(port);
    		LOGGER.info("Local node started " +peerId + "and port " + port);
    		
    		while (!bTerminate.get()) {
                try {
                    startLocalNode(connection);
                    Thread.sleep(1000);
                } catch (Exception e) {
                	LOGGER.log(Level.INFO, e.getMessage(), e);
                }
            }
    	}
    	catch(IOException e) {
			LOGGER.log(Level.INFO, e.getMessage(), e);
    	}
    }

	@Override
	synchronized public void chokePeers(ArrayList<RemotePeerInfo> peerList) {
		for(RemotePeerInfo peer : peerList) {
			if(mapRemoteNode.containsKey(peer.getPeerId())) {
				RemoteNode oRemoteNode = mapRemoteNode.get(peer.getPeerId());
				LOGGER.info("Add choke message to queue for peer : "+peer.getPeerId());
				oRemoteNode.sendMessage(new Choke());
			}
		}
	}

	@Override
	synchronized public void unchokePeers(ArrayList<RemotePeerInfo> peerList) {
		for(RemotePeerInfo peer : peerList) {
			if(mapRemoteNode.containsKey(peer.getPeerId())) {
				RemoteNode oRemoteNode = mapRemoteNode.get(peer.getPeerId());
                LOGGER.info("Add unchoke message to queue for peer : "+peer.getPeerId());
				oRemoteNode.sendMessage(new Unchoke());
			}
		}
	}
	@Override
	synchronized public void allNeighboursCompleted(){
		bNeighboursCompleted.set(true);
		LOGGER.info("Neighbours Completed: " + bNeighboursCompleted + " Files Completed: " + bfileCompleted);
		if(bNeighboursCompleted.get() && bfileCompleted.get()){
			stopNode();
		}
	}

	
	private void stopNode() {
		LOGGER.info("All NeighBours have completed Download");
        LOGGER.info("Terminating");
		bTerminate.set(true);
		System.exit(0);
	}

	@Override
	synchronized public void localCompletedDownload() {
		bfileCompleted.set(true);
		LOGGER.info("Node self completed");
		notifyAllPeers();
		if(bNeighboursCompleted.get() && bfileCompleted.get()){
			stopNode();
		}
	}

	synchronized private void  notifyAllPeers() {
		for(Map.Entry<Integer, RemoteNode> entry : mapRemoteNode.entrySet()) {
			LOGGER.info("Sending message Completed to peer id : " +entry.getKey());
			entry.getValue().sendCompleted(new Completed());
		}
	}

	@Override
	synchronized public void pieceArrived(int pieceId) {
		LOGGER.info("Size of mapRemoteNode during piece arrived" + mapRemoteNode.size());
		for(Map.Entry<Integer, RemoteNode> entry : mapRemoteNode.entrySet()) {
            LOGGER.info("Add Have message to queue for peer : " +entry.getKey());
			entry.getValue().sendMessage(new Have(pieceId));
			// Send Not interested message to remote if we already have all the piece which remote has
			LOGGER.info("File completed state when piece arrived : " + oFileManager.isFileCompleted());
			if(!oPeerManager.isInteresting(entry.getKey(), oFileManager.getReceivedPiece()) || oFileManager.isFileCompleted()) {
                LOGGER.info("Add NotInterested message to queue for peer : " +entry.getKey());
				entry.getValue().sendMessage(new NotInterested());
			}
		}
	}

	@Override
	public void peerCompleted(int remotePeerId) {
		mapCompletedNeig.put(remotePeerId, true);
		if(mapCompletedNeig.size() == mapRemoteNode.size() && bfileCompleted.get()) {
			stopNode();
		} else {
			LOGGER.info("Neighbours Completed: " + mapCompletedNeig.size() + " Files Completed: " + bfileCompleted);
		}
		
	}
}

