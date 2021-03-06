package handler;

/*
 -----------------------------------------------------------------------------------
 Odeep
 Fichier     : handler.DHR1Handler.java
 Auteur(s)   : Burgbacher Lionel, Jee Mathieu, Kopp Olivier, Piller Florent,
               Silvestri Romain, Schürch Loïc
 Date        : 27.04.2018
 Compilateur : jdk 1.8.0_144
 -----------------------------------------------------------------------------------
*/

import message.MessageHandler;
import peer.PeerConnection;
import peer.PeerMessage;
import main.Client;
import Node.*;

/**
 * Classe permettant le traitement d'un message de type UPDT
 */
public class UPDTHandler implements MessageHandler{
    @Override
    public void handleMessage(Node n, PeerConnection c, PeerMessage m) {
        Client.downloadJSON(m.getIdGroup());
    }
}
