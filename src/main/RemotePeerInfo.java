package main;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * This Class maintains states of Remote Peers
 */
public class RemotePeerInfo {
    private final String peerId;
    private final String ipAddress;
    private final String port;
    private final boolean hasFile;
    private AtomicInteger bytesDownloaded;
    private Boolean bChoked = true;
    public BitSet bitSet;
    private final AtomicBoolean isInterested;



    public RemotePeerInfo(String pId, String pAddress, String pPort, boolean hasFile) {
        peerId = pId;
        ipAddress = pAddress;
        port = pPort;
        this.hasFile = hasFile;
        bytesDownloaded = new AtomicInteger (0);
        bitSet = new BitSet();
        isInterested = new AtomicBoolean (false);
    }

    public int getPeerId() {
        return Integer.parseInt(peerId);
    }

    public int getPort() {
        return Integer.parseInt(port);
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public boolean hasFile() {
        return hasFile;
    }

    public boolean isInterested() {
        return isInterested.get();
    }

    public void setInterested(Boolean state) {
        isInterested.set (state);
    }

    public void setNotIterested() {
        isInterested.set (false);
    }
    
    public void resetDownloadedBytes() {
    	bytesDownloaded.set(0);
    }
    
    public int getDownLoadSpeed() {
    	// Download speed is proportional with bytesDownloaded for a giver period of time
    	return bytesDownloaded.get();
    }
    
    public AtomicInteger getDownLoadedBytes() {
    	// Download speed is proportional with bytesDownloaded for a giver period of time
    	return bytesDownloaded;
    }
    
    public void setChoked(Boolean bChoke) {
    	bChoked = bChoke;
    }
    
    public Boolean isChoked() {
    	return bChoked;
    }
}
