package server;

import peer.PeerMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServerRelay {

    protected HashMap<String,BufferedOutputStream > peapleInServ = new HashMap<String, BufferedOutputStream>();

    public static void main(String[] args) {
        ServerRelay m = new ServerRelay();
        m.serveClient();
    }

    public void serveClient(){
        new Thread(new Receptioniste()).start();
    }


    private class Receptioniste implements Runnable{
        boolean serverStopped = false;
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try{
                serverSocket = new ServerSocket(80);
            }catch(IOException e){
                System.out.println(e.getMessage());
            }

            while(!serverStopped){
                System.out.println("En attente de connection");
                try {
                    Socket client = serverSocket.accept();
                    client.getInetAddress();
                    new Thread(new ServeurWorker(client)).start();
                }catch(IOException e){
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private class ServeurWorker implements Runnable {

        Socket clientToSever;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        byte[] bufferIn = new byte[4096];
        byte[] bufferOut = new byte[4096];

        public ServeurWorker(Socket clientSocket) {
            try {
                this.clientToSever = clientSocket;
                in = new BufferedInputStream(clientSocket.getInputStream());
                out = new BufferedOutputStream(clientSocket.getOutputStream());
                out.flush();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        }

        @Override
        public void run() {
            greetings();
            do {
                redirectMessage(in);
            }while(true); // rajouter condition de fin avec les messages
        }

        private void greetings() {
            try {
                System.out.println("Appellte de greetings");
                    in.read(bufferIn);
                    PeerMessage pm = new PeerMessage(bufferIn);
                    String sender = pm.getIdFrom();
                    peapleInServ.put(sender, out);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        void redirectMessage(BufferedInputStream in) {
            System.out.println("Appelle de redirect");
            String sender  = "";
            try {
                    while((in.read(bufferIn) != -1)){
                        PeerMessage pm = new PeerMessage(bufferIn);
                        sender = pm.getIdTo();
                        bufferOut = bufferIn.clone();
                        if (peapleInServ.containsKey(sender)) {
                            new Thread(new writeToClient(peapleInServ.get(sender), bufferOut)).start();
                        }

                    }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private class writeToClient implements Runnable {
        byte[] buff;
        BufferedOutputStream out = null;

        public writeToClient(BufferedOutputStream out, byte[] buff) {
            this.buff = buff.clone();
            this.out = out;
        }

        @Override
        public void run() {
            try {
                System.out.println("Ecriture de" + buff.toString());
                out.write(this.buff, 0, this.buff.length);
                out.flush();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }


}
