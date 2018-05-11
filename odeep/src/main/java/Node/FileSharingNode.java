package Node;

import message.MessageType;
import org.bouncycastle.crypto.InvalidCipherTextException;
import peer.PeerConnection;
import peer.PeerInformations;
import peer.PeerMessage;
import util.CipherUtil;

import java.io.*;
import java.net.Socket;

public class FileSharingNode extends Node {

    public FileSharingNode(PeerInformations myInfos) {
        super(myInfos);
    }

    /**
     * methode permettant l'envoie d'un fichier entre membre d'un meme groupe
     *
     * @param file        fichier a envoyer
     * @param groupID
     * @param destination
     * @throws IOException
     */
    public void sendFileToPeer(File file, String groupID, PeerInformations destination) throws IOException {
        RandomAccessFile f = null;
        byte[] key = null;
        try {
            f = new RandomAccessFile("./" + groupID + "/key", "r");
            key = new byte[(int) f.length()];
            f.readFully(key);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileInputStream fis = new FileInputStream(file);
        int index = 0;
        PeerConnection c = new PeerConnection(destination);
        String filename = file.getName();
        long fileSize = file.length();
        String fileInfo = filename + ":" + Long.toString(fileSize);
        byte[] cipherFileInfo = CipherUtil.AESEncrypt(fileInfo.getBytes(), key);


        c.sendMessage(new PeerMessage(MessageType.SFIL, groupID, this.getNodePeer().getID(), destination.getID(), index, cipherFileInfo));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        index++;
        byte[] mes = new byte[PeerMessage.MESSAGE_CONTENT_SIZE];
        for (int i = 0; i < (fileSize / PeerMessage.MESSAGE_CONTENT_SIZE) + 1; i++, index++) {
            int size = fis.read(mes, i * PeerMessage.MESSAGE_CONTENT_SIZE, PeerMessage.MESSAGE_CONTENT_SIZE);
            byte[] cipherMes = CipherUtil.AESEncrypt(mes, key);
            c.sendMessage(new PeerMessage(MessageType.SFIL, groupID, this.getNodePeer().getID(), destination.getID(), index, cipherMes));
        }
    }

    /**
     * cette fonction retourne la liste des personne presentes dans un groupe possedant un certain fichier
     *
     * @param filename fichier recherché
     * @param groupID  nom du groupe
     * @return liste de peerInformation
     */
    public PeerInformations[] getFileLocation(String filename, String groupID) {
        return null;
    }


    /**
     * cette fonction envoie une requete afin de determiner si un pair possède effectivement un fichier
     *
     * @param filename    nom du fichier
     * @param groupID     groupe dans lequel la requete est effectuée
     * @param destination pair vers lequel la requete est envoyée
     *                    remarque : le pair qui recoit cette requete cherchera le fichier uniquement dans le dossier du groupe passé en paramètre
     */
    public void requestFile(String filename, String groupID, PeerInformations destination) {
        if (filename == null || groupID == null || destination == null) {
            throw new NullPointerException();
        }
        byte buffer[] = new byte[4032];
        buffer = filename.getBytes();
        PeerInformations[] peerHavingFile = getFileLocation(filename, groupID);
        for (PeerInformations peer : peerHavingFile) {
            sendToPeer(new PeerMessage(MessageType.RFIL.toString(), groupID, this.getNodePeer().getID(), peer.getID(), buffer), destination);
        }
    }

    /**
     * Demande a un pair d'envoyer un fichier a travers un groupe donné
     *
     * @param filename    nom du fichier
     * @param groupID     nom du groupe
     * @param destination pair destinataire de la requete
     */
    public void askForFile(String filename, String groupID, PeerInformations destination) {
        if (filename == null || groupID == null || destination == null) {
            throw new NullPointerException();
        }
        byte buffer[] = new byte[4032];
        buffer = filename.getBytes();
        sendToPeer(new PeerMessage(MessageType.FGET.toString(), groupID, this.getNodePeer().getID(), destination.getID(), buffer), destination);
    }

    public void test() throws IOException {
        File[] filesList = new File(".").listFiles();
        for (File f : filesList) {
            if (f.isFile()) {
                System.out.println(f.getName());
            }
        }
        sendFileToPeer(new File("./testfile"), "0", new PeerInformations("test", "127.0.0.1", 80));

    }


}
