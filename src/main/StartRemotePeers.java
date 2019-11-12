package main;
/*
 *                     CEN5501C Project2
 * This is the program starting remote processes.
 * This program was only tested on CISE SunOS environment.
 * If you use another environment, for example, linux environment in CISE
 * or other environments not in CISE, it is not guaranteed to work properly.
 * It is your responsibility to adapt this program to your running environment.
 */

import java.io.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

/*
 * The StartRemotePeers class begins remote peer processes.
 * It reads configuration file PeerInfo.cfg and starts remote peer processes.
 * You must modify this program a little bit if your peer processes are written in C or C++.
 * Please look at the lines below the comment saying IMPORTANT.
 */
public class StartRemotePeers {

    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());
    public Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    public static void getPeerConfiguration(ArrayList<RemotePeerInfo> peerInfoVector)
    {
        String st;
        int i1;

        try {
            BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
            while((st = in.readLine()) != null) {
            	st = st.trim();
                if ((st.length() <= 0) || (st.startsWith ("#"))) {
                    continue;
                }
                String[] tokens = st.split("\\s+");
                peerInfoVector.add(new RemotePeerInfo(tokens[0], tokens[1], tokens[2], tokens[3].equals("1")));
            }

            in.close();
        }
        catch (Exception ex) {
            LOGGER.log(Level.INFO, ex.getMessage(), ex);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException{
        // TODO Auto-generated method stub
        boolean append = true;
        LOGGER.setLevel(Level.ALL);
        try {
            //StartRemotePeers myStart = new StartRemotePeers();
        	Config.initialize();
        	ArrayList<RemotePeerInfo> peerInfoList = new ArrayList<RemotePeerInfo>();
        	getPeerConfiguration(peerInfoList);

            // get current path
            String path = System.getProperty("user.dir");

            final int peerId = Integer.parseInt(args[0]);

            FileHandler handler = new FileHandler("log_peer_" + peerId + ".log", append);
            LOGGER.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
            String address = "localhost";
            int port = 0;
            boolean hasFile = false;

            // start clients at remote hosts
            for (RemotePeerInfo pInfo : peerInfoList) {

                //System.out.println("Start remote peer " + pInfo.getPeerId() +  " at " + pInfo.getIpAddress() );
                //LOGGER.info("Start remote peer " + pInfo.getPeerId() +  " at " + pInfo.getIpAddress());

                // *********************** IMPORTANT *************************** //
                // If your program is JAVA, use this line.
                //Runtime.getRuntime().exec("ssh " + pInfo.getIpAddress() + " cd " + path + "; java StartRemotePeers " + pInfo.getPeerId());
                if(pInfo.getPeerId() == peerId) {
                	hasFile = pInfo.hasFile();
                	port = pInfo.getPort();
                	//address = pInfo.getIpAddress();
                }
                // If your program is C/C++, use this line instead of the above line.
                //Runtime.getRuntime().exec("ssh " + pInfo.peerAddress + " cd " + path + "; ./peerProcess " + pInfo.peerId);
            }
            //System.out.println("Starting all remote peers has done." );
            //LOGGER.info("Starting all remote peers has done.");

            Node localNode = new Node (peerId, address, port, hasFile, peerInfoList);
            Thread nodeThread = new Thread (localNode);
            nodeThread.setName("Local Node" + peerId);
            nodeThread.start();
            //connect to Remote
            localNode.createRemoteConnection();

        }
        catch (Exception ex) {
            LOGGER.log(Level.INFO, ex.getMessage(), ex);
        }
    }

}
