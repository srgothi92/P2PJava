package main;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class will provide k preferred neighbours and one optimistically unchoked neighbour for every unchoking interval.
 */
public class PeerManager implements Runnable {
    private ArrayList<Listener> nodeListeners = new ArrayList<Listener>();
    private ArrayList<RemotePeerInfo> peerInfoList;
    private Integer localPeerId;
    private ArrayList<RemotePeerInfo> preferredPeerList = new ArrayList<RemotePeerInfo>();
	private OptimisticPeerUnchoker oOptimisticPeerUnchoker;
	public Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());
    public PeerManager(Integer localPeerId, ArrayList<RemotePeerInfo> peerList) {
        this.peerInfoList = peerList;
        this.localPeerId = localPeerId;
        this.oOptimisticPeerUnchoker = new OptimisticPeerUnchoker();
    }
    
    public void register(Listener lis) { nodeListeners.add(lis); }
    public void unregister(Listener lis) {nodeListeners.remove(lis); }
    
    synchronized private ArrayList<RemotePeerInfo> getInterestedPeers(){
    	ArrayList<RemotePeerInfo> interesetedPeers = new ArrayList<RemotePeerInfo>();
    	for(RemotePeerInfo peer : this.peerInfoList ) {
    		// check interested and ignore self
    		if(peer.isInterested() && peer.getPeerId() != localPeerId) {
    			interesetedPeers.add(peer);
    		}
    	}
		return interesetedPeers;
    }
    
    synchronized private ArrayList<RemotePeerInfo> getPreferredPeers(ArrayList<RemotePeerInfo> interesetedPeers){
    	if(Config.NumberOfPreferredNeighbors < interesetedPeers.size()) {
    		return new ArrayList<RemotePeerInfo>(interesetedPeers.subList(0, Config.NumberOfPreferredNeighbors));
    	} else {
    		return interesetedPeers;
    	}
    }
    
    synchronized private ArrayList<RemotePeerInfo> getPrefPeersToChoke( ArrayList<RemotePeerInfo> oldPreferedPeer,  ArrayList<RemotePeerInfo> newPreferredPeer) {
    	// Get the List of Peers which are not there in new Preferred Neighbors list
    	ArrayList<RemotePeerInfo> peerToChoke = new ArrayList<RemotePeerInfo>();
    	for(RemotePeerInfo oldPeer : oldPreferedPeer ) {
    		boolean commonPeer = false;
    		for(RemotePeerInfo newPeer : newPreferredPeer) {
        		if(newPeer.getPeerId() == oldPeer.getPeerId()) {
        			commonPeer = true;
        			break;
        		}
        	}
    		if(commonPeer == false) {
    			oldPeer.setChoked(true);
    			peerToChoke.add(oldPeer);
    		}
    	}
    	return peerToChoke;
    }
    
    synchronized private ArrayList<RemotePeerInfo> getPrefPeersToUnChoke(ArrayList<RemotePeerInfo> newPreferredPeer) {
    	// Get the List of Peers which are choked in new Preferred Neighbors list
    	ArrayList<RemotePeerInfo> peerToUnChoke = new ArrayList<RemotePeerInfo>();
    	for(RemotePeerInfo newPeer : newPreferredPeer) {
    		if(newPeer.isChoked()) {
    			newPeer.setChoked(false);
    			peerToUnChoke.add(newPeer);
    		}
    	}
    	return peerToUnChoke;
    }
    
    synchronized private ArrayList<RemotePeerInfo> getAllChokedPeers() {
    	ArrayList<RemotePeerInfo> chokedPeerList = new ArrayList<RemotePeerInfo>();
    	for(RemotePeerInfo peer : this.peerInfoList ) {
    		// check choked and ignore self
    		if(peer.isChoked() && peer.getPeerId() != localPeerId) {
    			chokedPeerList.add(peer);
    		}
    	}
    	return chokedPeerList;
    }
    
    synchronized private void resetDownloadedBytes() {
    	for(RemotePeerInfo peer : this.peerInfoList ) {
    		peer.resetDownloadedBytes();
    	}
    }
    
    private void logPeerList(ArrayList<RemotePeerInfo > peerList) {
    	for(RemotePeerInfo peer : peerList ) {
			LOGGER.info("PeerID: "+ peer.getPeerId());
		}
    }

    /**
     * Every unchoking interval, perform following steps :
     * calculate downloading speed in previous interval
     * get list of interested neighbours
     * select top k according to download speed and unchoke the choked ones among them
     * Every optimistic unchoking interval, select a random choked and interested neighbour and unchoke it.
     */
    @Override
    public void run() {
    	//Assume all are choked in beginning
    	oOptimisticPeerUnchoker.setChokedPeers(peerInfoList);
    	oOptimisticPeerUnchoker.start();
    	while(true) {
    		try {
				Thread.sleep(Config.UnchokingInterval * 1000 );
			} catch (InterruptedException e) {
				LOGGER.log(Level.INFO, e.getMessage(), e);
			}
    	    LOGGER.info("Start selecting new set of preferred peers");
            // Get List of Interested peers
    		ArrayList<RemotePeerInfo> peerList = getInterestedPeers();
    		LOGGER.info("Interested peers : ");
    		logPeerList(peerList);
    		// Sort peer list according to download speed
    		Collections.sort(peerList, new Comparator<RemotePeerInfo>() {
                @Override
                public int compare(RemotePeerInfo rP1, RemotePeerInfo rP2) {
                    // decreasing order
                    return (rP1.getDownLoadSpeed() - rP2.getDownLoadSpeed());
                }
            });
    		synchronized (this) {
    		// Reset the downloadbytes
    		resetDownloadedBytes();
            // select top k preferred peers from interested peerList
    		ArrayList<RemotePeerInfo> newPreferredPeerList  = getPreferredPeers(peerList);
    		LOGGER.info("[ " +timestamp+ " ]: Peer " +localPeerId+ "has the preferred neighbors " + newPreferredPeerList );
    		logPeerList(newPreferredPeerList);
            //LOGGER.info("newPreferredPeerList peers : " +newPreferredPeerList);
    		ArrayList<RemotePeerInfo> peerToChoke = getPrefPeersToChoke(preferredPeerList, newPreferredPeerList);
    		//LOGGER.info("Peers to choke  : " +peerToChoke);
    	    ArrayList<RemotePeerInfo> peerToUnChoke = getPrefPeersToUnChoke(newPreferredPeerList);
    	    //LOGGER.info("Peers to unchoke  : " + peerToUnChoke);
    	    preferredPeerList = newPreferredPeerList;
    	    //Notify node of the updated peer list to choke and unchoke remote peers
    	    for (Listener listener : nodeListeners) {
    	    	//LOGGER.info("Start Choking and Unchoking Peers");
                listener.chokePeers(peerToChoke);
                listener.unchokePeers(peerToUnChoke);
            }
			this.oOptimisticPeerUnchoker.setChokedPeers(getAllChokedPeers());
    		}
    	}
    }
    
    synchronized public Boolean isInteresting(int remotePeerId, BitSet localBitset) {
    	for(RemotePeerInfo peer : this.peerInfoList ) {
    		if(peer.getPeerId() == remotePeerId) {
    	    	BitSet remoteBitset = (BitSet) peer.bitSet.clone();
    	    	LOGGER.info("Remote peer :" + remotePeerId + "cardinality is " + remoteBitset.cardinality());
    	    	LOGGER.info("Local :" + this.localPeerId + "cardinality is " + localBitset.cardinality());
    	    	remoteBitset.andNot(localBitset);
    	    	return !remoteBitset.isEmpty();
    		}
    	}
    	return false;
    }
    
    synchronized public void updateInterestedState(int remotePeerId, boolean state) {
    	for(RemotePeerInfo peer : this.peerInfoList ) {
    		if(peer.getPeerId() == remotePeerId) {
    			peer.setInterested(state);
    		}
    	}
	}
    /**
     * Update the BitField of Remote peer to keep a track of the piece that remote has.
     * @param remotePeerId
     * @param pieceId
     */
    synchronized public void haveArrived(int remotePeerId, int pieceId) {
		Boolean remoteCompleted = false;
		for(RemotePeerInfo peer : this.peerInfoList ) {
			if(peer.getPeerId() == remotePeerId) {
				LOGGER.info("Remote peer" + peer.getPeerId() + "cardinality is " + peer.bitSet.cardinality() );
				peer.bitSet.set(pieceId);
			}
    	}
		checkNeighBoursCompleted();
	}

    synchronized public Boolean checkPereferedPeer(int remotePeerId) {
		for(RemotePeerInfo peer : this.preferredPeerList ) {
			LOGGER.info("Check Preferred peer: " + peer.getPeerId() + " equals to remotePeerId");
    		if(peer.getPeerId() == remotePeerId) {
    			return true;
    		}
    	}
		return false;
	}

    synchronized public void updateRemoteDwndedBytes(int remotePeerId, int bytesDowloaded) {
		for(RemotePeerInfo peer : this.peerInfoList ) {
			if(peer.getPeerId() == remotePeerId) {
				peer.getDownLoadedBytes().addAndGet(bytesDowloaded);
			}
    	}
	}
    
    synchronized private void checkNeighBoursCompleted() {
    	// Notify Node that Peer has completed
    	int count = 0;
    	boolean bAllNegihboursCompleted = true;
    	for (RemotePeerInfo peer : peerInfoList) {
    		if (peer.bitSet.cardinality() < Config.BitSetSize-1) {    	    	
    			LOGGER.info("Peer " + peer.getPeerId() + " has not completed yet");
    			bAllNegihboursCompleted = false;
    		}else {
    		   count++;
    		}
    	}
    	if(!bAllNegihboursCompleted) {
    		LOGGER.info("Neighbours Completed Count " + count +" Total Negihbours: " + peerInfoList.size());
    		return;
    	}
    	for (Listener listener : nodeListeners) {
    		listener.allNeighboursCompleted();
    	}
    }

    synchronized public BitSet getRemoteBitFied(int remotePeerId) {
		for(RemotePeerInfo peer : this.peerInfoList ) {
			if(peer.getPeerId() == remotePeerId) {
				return (BitSet) peer.bitSet.clone();
			}
    	}
		// Return Empty
		return new BitSet();
	}

    synchronized public void bitfieldArrived(int remotePeerId, BitSet bitset) {
		for(RemotePeerInfo peer : this.peerInfoList ) {
			if(peer.getPeerId() == remotePeerId) {
				peer.bitSet  = bitset;
			}
    	}
		checkNeighBoursCompleted();
	}

	synchronized public void setNeighbourCompleted(int remotePeerId) {
		for (Listener listener : nodeListeners) {
			listener.peerCompleted(remotePeerId);
       }
	}

	class OptimisticPeerUnchoker extends Thread {
        private final Vector<RemotePeerInfo> chokedPeers = new Vector<RemotePeerInfo>();
        synchronized void setChokedPeers(ArrayList<RemotePeerInfo> chokedPeersList) {
        	chokedPeers.clear();
        	chokedPeers.addAll(chokedPeersList);
        }
        
        synchronized RemotePeerInfo findPeerToUnchoke() {
        	RemotePeerInfo randomChokedPeer = null;
        	int count = 0;
    		do {
    			randomChokedPeer = chokedPeers.get(new Random().nextInt(chokedPeers.size()));
    			count++;
    		} while(randomChokedPeer !=null && randomChokedPeer.isInterested() && count < chokedPeers.size());
    		return randomChokedPeer;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(Config.OptimisticUnchokingInterval*1000);
                } catch (InterruptedException ex) {
                	LOGGER.log(Level.INFO, ex.getMessage(), ex);
                }

                synchronized (this) {
                	if (chokedPeers.size() > 0) {
                		ArrayList<RemotePeerInfo> optUnchokePeer = new ArrayList<RemotePeerInfo>();
                		// Randomly select a neighbour to optimistically unchoke
                		RemotePeerInfo randomChokedPeer = findPeerToUnchoke();
                		if(randomChokedPeer!= null){
                		//	randomChokedPeer.setChoked(false);
                			optUnchokePeer.add(randomChokedPeer);
                			LOGGER.info("[ " +timestamp+ " ]: Peer " +localPeerId+ " has the optimistically unchoked neighbor " +randomChokedPeer.getPeerId());
//                			for (Listener listener : nodeListeners) {
//                				//LOGGER.info("Optimistical Unchoked Peer selected : " + randomChokedPeer.getPeerId());
//                				listener.unchokePeers(optUnchokePeer);
//                			}
                		}
                	}
                }
                
            }
        }
    }
}
