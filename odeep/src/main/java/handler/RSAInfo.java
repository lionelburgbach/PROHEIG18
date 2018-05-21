package handler;

/*
 -----------------------------------------------------------------------------------
 Odeep
 Fichier     : handler.RSAInfo.java
 Auteur(s)   : Kopp Olivier
 Date        : 27.04.2018
 Compilateur : jdk 1.8.0_144
 -----------------------------------------------------------------------------------
*/

import peer.PeerMessage;
import util.CipherUtil;

import java.security.*;

/**
 * Classe contenant les informations utiles au chiffrement RSA
 */
public class RSAInfo {

    //pair de clés
    private KeyPair kp;
    //clé public pouvant être envoyée sur le réseau
    private byte[] publicKey;

    public RSAInfo() {
    }

    /**
     * génére une paire de clé RSA
     */
    public void setKeys() {
        kp = CipherUtil.GenerateRSAKey();
        publicKey = CipherUtil.publicKeyToByte(kp.getPublic());
    }

    /**
     * Fonction utilisé lors de la reception du message contenant un secret chiffré avec RSA.
     * @param m message recu
     * @return le secret déchiffré
     */
    public byte[] getFinalKey(PeerMessage m) {
        byte[] message = m.getMessageContent();
        return CipherUtil.RSADecrypt(kp.getPrivate(), message);
    }

    public byte[] getPublicKey() {
        return publicKey;
    }
}
