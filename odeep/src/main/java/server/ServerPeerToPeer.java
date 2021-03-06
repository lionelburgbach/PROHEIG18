package server;
/*
 -----------------------------------------------------------------------------------
 Odeep
 Fichier     : handler.DHR1Handler.java
 Auteur(s)   : Burgbacher Lionel, Jee Mathieu, Kopp Olivier, Piller Florent,
               Silvestri Romain, Schürch Loïc
 Date        : 05.04.2018
 Compilateur : jdk 1.8.0_144
 -----------------------------------------------------------------------------------
*/

import message.MessageType;
import peer.PeerMessage;
import util.CipherUtil;
import util.DatabaseUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Serveur multithreadé servant de relai entre les différents clients de notre application
 */
public class ServerPeerToPeer {

    private DatabaseUtil databaseUtil;
    private static Object o = new Object();
    private static String ROOT_GROUPS_DIRECTORY = "groupsConfigs";
    private HashMap<String, String> clientIPPrivee = new HashMap<String, String>();
    private HashMap<String, Socket> peopleInServ = new HashMap<String, Socket>();

    public static void main(String[] args) {
        server.ServerPeerToPeer m = new server.ServerPeerToPeer();
        m.serveClient();
    }


    public void serveClient() {
        databaseUtil = new DatabaseUtil();
        try {
            databaseUtil.initConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        new Thread(new Receptionist()).start();
    }


    private class Receptionist implements Runnable {
        boolean serverStopped = false;

        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(8080);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!serverStopped) {
                try {
                    Socket client = serverSocket.accept();
                    new Thread(new ServerWorker(client)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ServerWorker implements Runnable {
        private byte[] redirectBuffer = new byte[PeerMessage.BLOCK_SIZE];
        Socket clientToSever;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        boolean running = true;
        byte[] bufferIn = new byte[PeerMessage.BLOCK_SIZE];

        public ServerWorker(Socket clientSocket) {
            try {
                this.clientToSever = clientSocket;
                in = new BufferedInputStream(clientSocket.getInputStream());
                out = new BufferedOutputStream(clientSocket.getOutputStream());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            PeerMessage pm = null;
            try {

                while (running) {
                    int read = 0;
                    while (read != PeerMessage.BLOCK_SIZE) {
                        read += in.read(bufferIn, read, bufferIn.length - read);
                    }
                    pm = new PeerMessage(bufferIn);
                    String type = pm.getType();

                    switch (type) {
                        case MessageType.HELO:
                            greetings(pm);
                            break;
                        case MessageType.EXIT:
                            bye(pm);
                            break;
                        case MessageType.USRV:
                            validationUsername(pm);
                            break;
                        case MessageType.INFO:
                            giveInfoToSender(pm);
                            break;
                        case MessageType.INVI:
                        case MessageType.INVK:
                        case MessageType.RFIL:
                        case MessageType.DHR1:
                        case MessageType.DHS2:
                        case MessageType.DHS1:
                        case MessageType.SFIL:
                        case MessageType.SMES:
                        case MessageType.UPDT:
                        case MessageType.PGET:
                            redirectBuffer = bufferIn.clone();
                            redirect(pm);
                            break;

                        // Création d'un nouveau groupe
                        case MessageType.NEWG:
                            int ajout = DatabaseUtil.addGroupIfNotExists(pm.getIdGroup());


                            if (ajout == 1) {
                                out.write((new PeerMessage(pm.getType(), pm.getIdGroup(), pm.getIdFrom(), pm.getIdTo(), "true".getBytes())).getFormattedMessage());
                                out.flush();

                            } else {
                                out.write((new PeerMessage(pm.getType(), pm.getIdGroup(), pm.getIdFrom(), pm.getIdTo(), "false".getBytes())).getFormattedMessage());
                                out.flush();
                            }

                            break;

                        // Stocker le fichier config reçu sur le serveur
                        case MessageType.UPLO:

                            //On récupère la taille du JSON a download

                            byte[] input = CipherUtil.erasePadding(pm.getMessageContent(), PeerMessage.PADDING_START);
                            FileOutputStream fout = new FileOutputStream(new File("./" + ROOT_GROUPS_DIRECTORY + "/" + pm.getIdGroup()));

                            int size = input.length;

                            fout.write(input, 0, size);
                            fout.flush();
                            fout.close();

                            break;

                        // Envoie le fichier config à la personne voulue
                        case MessageType.DOWN:

                            RandomAccessFile json = new RandomAccessFile("./groupsConfigs/" + pm.getIdGroup(), "r");

                            byte[] bufferJson = new byte[(int) json.length()];
                            json.readFully(bufferJson);


                            PeerMessage jsonPm = new PeerMessage(pm.getType(), pm.getIdGroup(), pm.getIdFrom(), pm.getIdTo(), bufferJson);

                            out.write(jsonPm.getFormattedMessage());
                            out.flush();

                            break;

                        default:
                            break;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                bye(pm);
            }
        }

        /**
         * Méthode qui teste l'unicité du nom de l'utilisateur
         *
         * @param pm PeerMessage contant les inforamtions
         */

        private void validationUsername(PeerMessage pm) {
            int validation = DatabaseUtil.addUserIfNotExists(pm.getIdFrom());
            try {
                if (validation == 1) {
                    out.write(new PeerMessage(pm.getType(), pm.getIdGroup(), pm.getIdFrom(), pm.getIdTo(), "true".getBytes()).getFormattedMessage());
                } else {
                    out.write(new PeerMessage(pm.getType(), pm.getIdGroup(), pm.getIdFrom(), pm.getIdTo(), "false".getBytes()).getFormattedMessage());
                }
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Méthode de qui rajoute la personne entrée dans les personnes connectées
         *
         * @param pm PeerMessage contenant les informations
         */
        private void greetings(PeerMessage pm) {
            byte[] b = CipherUtil.erasePadding(pm.getMessageContent(), PeerMessage.PADDING_START);

            peopleInServ.put(pm.getIdFrom(), clientToSever);
            clientIPPrivee.put(pm.getIdFrom(), new String(b));
        }

        /**
         * Méthode qui renvoie l'adresse IP privée d'idTo
         *
         * @param pm PeerMessage contenant les informations
         */
        void giveInfoToSender(PeerMessage pm) {

            if (clientIPPrivee.containsKey(pm.getIdTo())) {
                try {

                    out.write(new PeerMessage(pm.getType(), pm.getIdGroup(), pm.getIdFrom(), pm.getIdTo(), clientIPPrivee.get(pm.getIdTo()).getBytes()).getFormattedMessage());
                    out.flush();

                } catch (IOException e) {

                }
            } else {
                try {

                    out.write(new PeerMessage(pm.getType(), pm.getIdGroup(), pm.getIdFrom(), pm.getIdTo(), "".getBytes()).getFormattedMessage());
                    out.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Méthode qui redirige les PeerMessage
         *
         * @param pm le PeerMessage à rediriger
         */
        void redirect(PeerMessage pm) {

            if (peopleInServ.containsKey(pm.getIdTo())) {
                try {
                    synchronized (o) {
                        BufferedOutputStream toOut = new BufferedOutputStream(peopleInServ.get(pm.getIdTo()).getOutputStream());
                        toOut.write(redirectBuffer);
                        toOut.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (!pm.getType().equals(MessageType.INVI)) {
                try {
                    PeerMessage nok = new PeerMessage(MessageType.DISC, pm.getIdGroup(), pm.getIdFrom(), pm.getIdTo(), ("").getBytes());

                    out.write(nok.getFormattedMessage());
                    out.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        /**
         * Enlève l'utilisateur des personnes connectées au serveur
         *
         * @param pm PeerMessage contenant les informations
         */
        void bye(PeerMessage pm) {
            if (peopleInServ.containsKey(pm.getIdFrom())) {
                peopleInServ.remove(pm.getIdFrom());
                if (clientIPPrivee.containsKey(pm.getIdFrom())) {
                    clientIPPrivee.remove(pm.getIdFrom());
                }
            }
            running = false;
            try {
                out.close();
                in.close();
                clientToSever.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
