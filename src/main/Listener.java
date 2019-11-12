package main;

import java.util.ArrayList;

public interface Listener {	
	public void chokePeers(ArrayList<RemotePeerInfo> peerList);
	public void unchokePeers(ArrayList<RemotePeerInfo> peerList);
	public void allNeighboursCompleted();
	public void peerCompleted(int remotePeerId);
	public void localCompletedDownload();
    public void pieceArrived (int partIdx);
}