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

        logPanel.append("Servidor iniciado." + "\r\n");

        try {
            serverSocket = new ServerSocket(serverPort);
        } catch (IOException e) {
            logPanel.append("ERRO: " + e + "\r\n");
        }

        new ServerMaitre().start();

    }
    class ServerMaitre extends Thread {
        int peerWaiting;
        int port;
        String address;

        public ServerMaitre () {
            peerWaiting = 0;
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
                        } else {
                            response.writeUTF("WELCOME" + "\r\n" + "TWO" + "\r\n" + "CONNECT" + "\r\n"
                                    + this.address + "\r\n" +
                                    this.port);
                            logPanel.append("Solicitacao enviada a " + address + ":" + port + " - conectar a outro par" + "\r\n");
                            clientOneStatus.setText("Conectado");
                            clientTwoStatus.setText("Conectado");
                            socket.close();
                            peerWaiting = 0;
                            this.port = 0;
                            this.address = null;
                        }

                    } else {
                        logPanel.append("Cliente tentou conectar com dado de autenticacao incorreto." + "\r\n");
                    }


                } catch (IOException e) {
                    logPanel.append("ERRO: Erro de I/O - " + e + "\r\n");
                }


            }
        }

    }
/*    class ConnectionCreatorThread extends Thread {

        protected Socket socket;
        WaitingStatus waitingStatus;

        public ConnectionCreatorThread(Socket clientSocket, WaitingStatus waitingStatus) {
            this.socket = clientSocket;
            this.waitingStatus = waitingStatus;

        }

        public void run() {
            try {
                DataInputStream auth = new DataInputStream(socket.getInputStream());
                String authData = auth.readUTF();

                if (authData.startsWith("FIGOCHAT CLIENT CONN")) {
                    int port = Integer.parseInt(authData.substring(21));
                    String address = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().toString().substring(1);
                    System.out.println("Conexao recebida: " + address + ":" + port);
                    DataOutputStream response = new DataOutputStream(socket.getOutputStream());

                    if (!waitingStatus.getWaitingStatus()) {
                        response.writeUTF("WAIT FOR CONNECTION");
                        System.out.println("Solicitacao enviada a " + address + ":" + port + " - esperar conexao");
                        socket.close();
                        waitingStatus.waitForTwo(port, address);
                    } else {
                        response.writeUTF("CONNECT" + "\r\n"
                                + waitingStatus.getAddress() + "\r\n" +
                                waitingStatus.getPort());
                        System.out.println("Solicitacao enviada a " + address + ":" + port + " - conectar a outro par");
                        socket.close();
                        waitingStatus.stopWaiting();
                    }

                } else {
                    System.out.println("Dado de autenticacao incorreto.");
                }


            } catch (IOException e) {
                System.out.println("Erro de I/O: " + e);
            }
        }

    }
    */


}