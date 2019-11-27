package application.gui;

import javax.swing.*;
import java.io.*;
import java.net.*;

public class server {

    private JPanel serverPanel;
    private JTextArea logPanel;
    private JLabel clientOneStatus;
    private JLabel clientTwoStatus;
    private ServerSocket serverSocket;
    private Socket socket;

    int peerWaiting;
    int serverPort;

    public server(int serverPort) {
        JFrame janela = new JFrame("Cinapsys Servidor");
        janela.setContentPane(serverPanel);
        janela.setVisible(true);
        janela.pack();
        janela.setSize(480, 720);
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.serverPort = serverPort;
        serverSocket = null;
        socket = null;
        peerWaiting = 0;

        logPanel.append("Servidor iniciado." + "\r\n");

        try {
            serverSocket = new ServerSocket(serverPort);
        } catch (IOException e) {
            logPanel.append("ERRO: " + e + "\r\n");
        }

        new ServerMaitre().start();

    }
    class ServerMaitre extends Thread {
        int port;
        String address;

        public ServerMaitre () {
            port = 0;
            address = null;
        }

        public void run() {
            try {
                serverSocket = new ServerSocket(serverPort);

            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    logPanel.append("ERRO: Erro de I/O - " + e + "\r\n");
                    break;
                }

                try {
                    DataInputStream auth = new DataInputStream(socket.getInputStream());
                    String authData = auth.readUTF();

                    if (authData.startsWith("CINAPSYS CLIENT CONN")) {
                        int port = Integer.parseInt(authData.substring(21));
                        String address = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().toString().substring(1);
                        logPanel.append("Conexao recebida: " + address + ":" + port + "\r\n");
                        DataOutputStream response = new DataOutputStream(socket.getOutputStream());

                        if (peerWaiting == 0) {
                            response.writeUTF("WELCOME" + "\r\n" + "ONE" + "\r\n" + "WAIT");
                            logPanel.append("Solicitacao enviada a " + address + ":" + port + " - esperar conexao" + "\r\n");
                            clientOneStatus.setText("Em espera");
                            socket.close();
                            peerWaiting = 1;
                            this.port = port;
                            this.address = address;
                        } else if (peerWaiting == 1) {
                            response.writeUTF("WELCOME" + "\r\n" + "TWO" + "\r\n" + "CONNECT" + "\r\n"
                                    + this.address + "\r\n" +
                                    this.port);
                            logPanel.append("Solicitacao enviada a " + address + ":" + port + " - conectar a outro par" + "\r\n");
                            clientOneStatus.setText("Conectado");
                            clientTwoStatus.setText("Conectado");
                            socket.close();
                            peerWaiting = -1;
                            this.port = 0;
                            this.address = null;
                        } else if (peerWaiting == 2) {
                            response.writeUTF("WELCOME" + "\r\n" + "ONE" + "\r\n" + "CONNECT" + "\r\n"
                                    + this.address + "\r\n" +
                                    this.port);
                            logPanel.append("Solicitacao enviada a " + address + ":" + port + " - conectar a outro par" + "\r\n");
                            clientOneStatus.setText("Conectado");
                            clientTwoStatus.setText("Conectado");
                            socket.close();
                            peerWaiting = -1;
                            this.port = 0;
                            this.address = null;
                        } else if (peerWaiting == -1) {
                            response.writeUTF("SORRY" + "\r\n" + "FULL");
                            logPanel.append("Cliente tentou conectar com servidor cheio.");
                            socket.close();

                        }

                    } else if (authData.startsWith("CINAPSYS CLIENT RECONN 1")) {
                        clientTwoStatus.setText("Sem Informação");
                        int port = Integer.parseInt(authData.substring(25));
                        String address = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().toString().substring(1);
                        logPanel.append("Conexao recebida: " + address + ":" + port + "\r\n");
                        DataOutputStream response = new DataOutputStream(socket.getOutputStream());

                        response.writeUTF("WELCOME" + "\r\n" + "ONE" + "\r\n" + "WAIT");
                        logPanel.append("Reconexao solicitada por " + address + ":" + port + " - esperar conexao" + "\r\n");
                        clientOneStatus.setText("Em espera");

                        peerWaiting = 1;
                        this.port = port;
                        this.address = address;

                        new ConnectionHolder(socket).start();
                        socket = null;

                    } else if (authData.startsWith("CINAPSYS CLIENT RECONN 2")) {
                        clientOneStatus.setText("Sem Informação");
                        int port = Integer.parseInt(authData.substring(25));
                        String address = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().toString().substring(1);
                        logPanel.append("Conexao recebida: " + address + ":" + port + "\r\n");
                        DataOutputStream response = new DataOutputStream(socket.getOutputStream());

                        response.writeUTF("WELCOME" + "\r\n" + "TWO" + "\r\n" + "WAIT");
                        logPanel.append("Reconexao solicitada por " + address + ":" + port + " - esperar conexao" + "\r\n");
                        clientTwoStatus.setText("Em espera");

                        peerWaiting = 2;
                        this.port = port;
                        this.address = address;

                        new ConnectionHolder(socket).start();
                        socket = null;

                    } else {
                        logPanel.append("Cliente tentou conectar com dado de autenticacao incorreto." + "\r\n");
                    }


                } catch (IOException e) {
                    logPanel.append("ERRO: Erro de I/O - " + e + "\r\n");
                }


            }
        }

    }

    class ConnectionHolder extends Thread {

        Socket holdingSocket;

        public ConnectionHolder(Socket holdingSocket) {
            this.holdingSocket = holdingSocket;
        }

        public void run() {

            try {
                DataInputStream unholdResponse = new DataInputStream(holdingSocket.getInputStream());
                String unholdData = unholdResponse.readUTF();
                clientOneStatus.setText("Conectado");
                clientTwoStatus.setText("Conectado");
                holdingSocket.close();
                peerWaiting = -1;
            } catch (SocketException e) {
                if (peerWaiting == 1) {
                    peerWaiting = 0;
                    clientOneStatus.setText("Sem Informação");

                } else if (peerWaiting == 2) {
                    peerWaiting = 0;
                    clientTwoStatus.setText("Sem Informação");

                }
            } catch (Exception e) {
                logPanel.append("ERRO: " + e + "\r\n");
            }

        }

    }


}