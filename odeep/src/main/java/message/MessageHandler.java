package message;
import peer.*;
import Node.*;/*
 -----------------------------------------------------------------------------------
 Odeep
 Fichier     : message.MessageHandler.java
 Auteur(s)   : Schürch Loïc
 Date        : 15.03.2018
 Compilateur : jdk 1.8.0_144
 -----------------------------------------------------------------------------------
*/

public interface MessageHandler {

    public abstract void handleMessage(FileSharingNode n, PeerConnection c, PeerMessage m);
}
