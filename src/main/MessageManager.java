package main;

import java.util.BitSet;
import java.util.logging.Logger;
import java.sql.Timestamp;

import messages.Bitfield;
import messages.Handshake;
import messages.Have;
import messages.Interested;
import messages.Message;
import messages.NotInterested;
import messages.Piece;
import messages.Request;


/**
 * This Class takes care of parsing bytes from peers as messages to take appropriate action
 * according to the protocol
 */

public class MessageManager {
	private boolean bChoked;
	private final FileManager oFileManager;
	private final PeerManager oPeerManager;
	private final int remotePeerId;
	private final int localPeerId;
	private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());
	public Timestamp timestamp = new Timestamp(System.currentTimeMillis());
	MessageManager(int localPeerId, int remotePeerId, PeerManager oPeerManager,
			FileManager oFileManager) {
		bChoked = true;
		this.localPeerId = localPeerId;
		this.remotePeerId = remotePeerId;
		this.oFileManager = oFileManager;
		this.oPeerManager = oPeerManager;
		LOGGER.info("MessageManager Initialized with local peer ID " + localPeerId + "and remote peer id " + remotePeerId);
	}
	
	 public Message handle(Handshake handshake) {
	        BitSet bitset = oFileManager.getReceivedPiece();
	        if (!bitset.isEmpty()) {
	            return (new Bitfield(bitset));
	        }
	        return null;
	    }

	public Message readMessage(Message msg) {
		LOGGER.info("Received message " + msg.getType());
		switch (msg.getType()) {

		case BitField: {
			Bitfield bitfield = (Bitfield) msg;
			BitSet bitset = bitfield.getBitSet();
			LOGGER.info("Received message BitField from peer id : " +remotePeerId + " and cardinality is " + bitset.cardinality());
			oPeerManager.bitfieldArrived(remotePeerId, bitset);
            // Compare with local bitfield
			bitset.andNot(oFileManager.getReceivedPiece());
			if (bitset.isEmpty()) {
				LOGGER.info("Send message not Interested to peer id : " +remotePeerId );
				return new NotInterested();

			} else {
				// intersection is not empty so remote has the part not available by node
				LOGGER.info("Send message Interested to peer id : " +remotePeerId );
			}
			return new Interested();

		}
		case Interested: {

			LOGGER.info("[ " +timestamp+ " ]: Peer " +localPeerId+ "received the 'interested' message from " +remotePeerId );
			oPeerManager.updateInterestedState(remotePeerId, true);
			return null;
		}
		case NotInterested: {
			LOGGER.info("[ " +timestamp+ " ]: Peer " +localPeerId+ "received the 'not interested' message from " +remotePeerId );
			oPeerManager.updateInterestedState(remotePeerId, false);
			return null;
		}
		case Choke: {
			LOGGER.info("[ " +timestamp+ " ]: Peer " +localPeerId+ "is choked by " +remotePeerId );
			bChoked = true;
			return null;
		}
		case Unchoke: {
			LOGGER.info("[ " +timestamp+ " ]: Peer " +localPeerId+ "is unchoked by " +remotePeerId );
			bChoked = false;
			return requestPiece();
		}
		case Have: {
			Have have = (Have) msg;
			final int pieceId = have.getPieceIndex();
			LOGGER.info("[ " +timestamp+ " ]: Peer " +localPeerId+ "received the 'have' message from " +remotePeerId+
					" for the piece " +pieceId);
			// update the remote bitfield in remoteInfo
			oPeerManager.haveArrived(remotePeerId, pieceId);
			// check if we already have the available piece from remote
			if (oFileManager.getReceivedPiece().get(pieceId)) {
				LOGGER.info("Send message NotInterested to peer id : " +remotePeerId );
				return new NotInterested();
			} else {
				LOGGER.info("Send message Interested to peer id : " +remotePeerId );
				return new Interested();
			}
		}
		case Request: {
			LOGGER.info("Received message Request from peer id : " +remotePeerId );
			Request request = (Request) msg;
			// check if remote node is one of our preferred peer or optimistically unchoked node
			if (oPeerManager.checkPereferedPeer(remotePeerId)) {
				LOGGER.info("Find Piece: " + request.getPieceIndex()+ " for peer id : " +remotePeerId );
				byte[] piece = oFileManager.getPiece(request.getPieceIndex());
				LOGGER.info("Got Piece: " + piece+ " to peer id : " +remotePeerId );
				if (piece != null) {
					LOGGER.info("Create Piece Request with piece: " + piece+ " to peer id : " +remotePeerId );
					return new Piece(request.getPieceIndex(), piece);
				}
			}
			return null;
		}
		case Piece: {
			Piece piece = (Piece) msg;
			int pieceId = piece.getPieceIndex();
			LOGGER.info("[ " +timestamp+ " ]: Peer " +localPeerId+ " has downloaded the piece " +pieceId+
					" from " +remotePeerId+ ". Now the number of pieces it has is " +oFileManager.getNumberOfReceivedPieces());
			oFileManager.addPiece(pieceId, piece.getContent());
			// update download bytes for remote to help in selection of preferred peers
			oPeerManager.updateRemoteDwndedBytes(remotePeerId,
					piece.getContent().length);
			// Request only if I have not completed dowloading the file
			if(!oFileManager.isFileCompleted()) {
				return  requestPiece();
			}
			return new NotInterested();
		}
		case Completed:{
			LOGGER.info("Received message Completed from peer id : " +remotePeerId );
			oPeerManager.setNeighbourCompleted(remotePeerId);
		}
		}

		return null;
	}

	private Message requestPiece() {
		if (!bChoked) {
			int pieceId = oFileManager.getPieceToRequest(oPeerManager
					.getRemoteBitFied(remotePeerId));
			if (pieceId >= 0) {
				//LOGGER.info("Send message Request to peer id : " +remotePeerId );
				return new Request(pieceId);
			}
		}
		return null;
	}

}
