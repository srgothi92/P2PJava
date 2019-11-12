package main;

import io.SerializableOutputStream;

import java.io.IOException;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import messages.Message;
import messages.Request;

public class RequestTimer extends TimerTask {
    private final Request reqMsg;
    private final FileManager oFileManager;
    private final  SerializableOutputStream out;
    private final int remotePeerId;
    private final Message oMsg;

    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());

    RequestTimer (Request request, FileManager oFileManager, SerializableOutputStream out, Message message, int remotePeerId) {
        super();
        reqMsg = request;
        this.oFileManager = oFileManager;
        this.out = out;
        this.remotePeerId = remotePeerId;
        oMsg = message;
    }

    @Override
    public void run() {
        if (oFileManager.hasPart(reqMsg.getPieceIndex())) {
        	LOGGER.info("Not rerequesting piece " + reqMsg.getPieceIndex()
                    + " to peer " + remotePeerId);
        }
        else {
        	LOGGER.info("Rerequesting piece " + reqMsg.getPieceIndex()
                    + " to peer " + remotePeerId);
            try {
            	out.writeObject(oMsg);
            } catch (IOException ex) {
            	LOGGER.log(Level.INFO, ex.getMessage(), ex);
            }

        }
    }
}
