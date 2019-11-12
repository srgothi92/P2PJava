package main;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This Class Splits the file into parts to share it with peers.
 */

public class SplitFile {
    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());
    public void process(File inputFile, int partSize, File partsDir){

        FileInputStream fileInputStream;
        String partsName;
        FileOutputStream fileOutStream;
        int fileSize = (int) inputFile.length();
        LOGGER.info("File read correctly and File size is " +fileSize);
        int count = 0, read = 0, readLength = partSize;
        byte[] partBytesArray;
        try {
            fileInputStream = new FileInputStream(inputFile);
            while (fileSize > 0) {
                if (fileSize <= 5) {
                    readLength = fileSize;
                }
                partBytesArray = new byte[readLength];
                read = fileInputStream.read(partBytesArray, 0, readLength);
                fileSize -= read;
                assert (read == partBytesArray.length);
                count++;
                partsName = partsDir + "/" + Integer.toString(count - 1);
                fileOutStream = new FileOutputStream(new File(partsName));
                fileOutStream.write(partBytesArray);
                fileOutStream.flush();
                fileOutStream.close();
                partBytesArray = null;
                fileOutStream = null;
            }
            fileInputStream.close();
        } catch (IOException ex) {
        	LOGGER.log(Level.INFO, ex.getMessage(), ex);
        }
    }


}
