package peer;
/*
 -----------------------------------------------------------------------------------
 Odeep
 Fichier     : handler.DHR1Handler.java
 Auteur(s)   : Burgbacher Lionel, Jee Mathieu, Kopp Olivier, Piller Florent,
               Silvestri Romain, Schürch Loïc
 Date        : 15.03.2018
 Compilateur : jdk 1.8.0_144
 -----------------------------------------------------------------------------------
*/

import com.sun.media.sound.InvalidFormatException;
import Node.Node;
/*
 -----------------------------------------------------------------------------------
 Odeep
 Fichier     : peer.PeerHandler.java
 Auteur(s)   : Schürch Loïc
 Date        : 15.03.2018
 Compilateur : jdk 1.8.0_144
 -----------------------------------------------------------------------------------
*/

import java.net.Socket;

/**
 * Classe permettant la gestion des reception de message
 */
public class PeerHandler implements Runnable {

    private Thread activity;
    Socket clientSocket;
    private Node node;

    public PeerHandler(Node node, Socket socket) {
        this.node = node;
        clientSocket = socket;
        activity = new Thread(this);
        activity.start();
    }

    public void run() {

        PeerConnection connection = new PeerConnection(clientSocket);

        PeerMessage message = null;
        try {
            message = connection.receiveMessage();
        } catch(InvalidFormatException e) {}

        //handle message
        node.getMapMessage().get(message.getType()).handleMessage(node,connection, message); //gerer erreur possible


        //close connexion
        connection.close();
    }

}
