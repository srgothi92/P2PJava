package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This Class will take care of Managing file parts and merging parts
 * to a file when all parts are available.
 */
public class Destination {

    private final File  partsDir;
    private static final String partsPath = "files/parts/";
    private final File fileToBeShared;
    private final static Logger LOGGER = Logger.getLogger(StartRemotePeers.class.getName());

    public Destination(int peerId, String fileName){
        partsDir = new File("./peer_" + peerId + "/" + partsPath + fileName);
        LOGGER.info("Parts Directory is " +partsDir);


        boolean bDirCreated =  partsDir.mkdirs();
        LOGGER.info("parts Created : " + bDirCreated);
        fileToBeShared = new File(partsDir.getParent() + "/../"  +fileName);
        LOGGER.info("File : " +fileToBeShared);
    }
    
    private byte[] getByteArrayFromFile(File file){
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            byte[] fileBytesArray = new byte[(int) file.length()];
            int bytesRead = fileInputStream.read(fileBytesArray, 0, (int) file.length());
            fileInputStream.close();
            assert (bytesRead == fileBytesArray.length);
            assert (bytesRead == (int) file.length());
            return fileBytesArray;
        } catch (IOException e) {
        	LOGGER.log(Level.INFO, e.getMessage(), e);
        }
        finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                }
                catch (IOException ex) {
                	LOGGER.log(Level.INFO, ex.getMessage(), ex);
                }
            }
        }
        return null;

    }

    public byte[][] getAllPartsAsByteArrays(){
        File[] files = partsDir.listFiles (new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
            	
                return true;
            }
        });
        byte[][] ba = new byte[files.length][getPartAsByteArray(1).length];
        for (File file : files) {
            ba[Integer.parseInt(file.getName())] = getByteArrayFromFile(file);
        }
        return ba;
    }

    public byte[] getPartAsByteArray(int partId) {
        File file = new File(partsDir.getAbsolutePath() + "/" + partId);
        return getByteArrayFromFile(file);
    }

    public void writeByteArrayAsFilePart(byte[] part, int partId){
        FileOutputStream fileOutStream;
        File filePart = new File(partsDir.getAbsolutePath() + "/" + partId);
        try {
            fileOutStream = new FileOutputStream(filePart);
            fileOutStream.write(part);
            fileOutStream.flush();
            fileOutStream.close();
        } catch (IOException e) {
        	LOGGER.log(Level.INFO, e.getMessage(), e);
        } 
    }

    public void splitFile(int partSize){
        SplitFile oSplitFile = new SplitFile();
        LOGGER.info("File : " +fileToBeShared );
        oSplitFile.process(fileToBeShared, partSize, partsDir);

    }

    public void mergeFile(int numParts) {
        FileOutputStream fileOutStream;
        FileInputStream fileInputStream;
        byte[] fileBytes;
        int bytesRead = 0;
        List<File> partsList = new ArrayList<>();
        for (int i = 0; i < numParts; i++) {
            partsList.add(new File(partsDir.getPath() + "/" + i));
        }
        try {
            fileOutStream = new FileOutputStream(fileToBeShared);
            for (File parts : partsList) {
                fileInputStream = new FileInputStream(parts);
                fileBytes = new byte[(int) parts.length()];
                bytesRead = fileInputStream.read(fileBytes, 0, (int) parts.length());
                assert (bytesRead == fileBytes.length);
                assert (bytesRead == (int) parts.length());
                fileOutStream.write(fileBytes);
                fileOutStream.flush();
                fileBytes = null;
                fileInputStream.close();
                fileInputStream = null;
            }
            fileOutStream.close();
            fileOutStream = null;
        } catch (Exception ex) {
        	LOGGER.log(Level.INFO, ex.getMessage(), ex);
        }
    }
}
