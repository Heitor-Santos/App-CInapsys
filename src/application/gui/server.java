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

            while (true) {
                try {
                    socket = serverSocket.accept();
                } catch (BindException e) {
                    logPanel.append("ERRO: " + e + "\r\n");
                    logPanel.append("Verifique se você já não existe uma instância de servidor ou cliente aberta na mesma porta." + "\r\n" + "\r\n");
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
                        logPanel.append("Conexão recebida: " + address + ":" + port + "\r\n");
                        DataOutputStream response = new DataOutputStream(socket.getOutputStream());

                        if (peerWaiting == 0) {
                            response.writeUTF("WELCOME" + "\r\n" + "ONE" + "\r\n" + "WAIT");
                            logPanel.append("Solicitação enviada a " + address + ":" + port + " (1) - esperar conexão" + "\r\n");
                            clientOneStatus.setText("Em espera");
                            peerWaiting = 1;
                            this.port = port;
                            this.address = address;
                            new ConnectionHolder(socket).start();

                        } else if (peerWaiting == 1) {
                            response.writeUTF("WELCOME" + "\r\n" + "TWO" + "\r\n" + "CONNECT" + "\r\n"
                                    + this.address + "\r\n" +
                                    this.port);
                            logPanel.append("Solicitação enviada a " + address + ":" + port + " (2) - conectar a outro par" + "\r\n");
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
                            logPanel.append("Solicitação enviada a " + address + ":" + port + " (1) - conectar a outro par" + "\r\n");
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
                        logPanel.append("Cliente 2 desconectado."  + "\r\n");
                        clientTwoStatus.setText("Desconectado");
                        int port = Integer.parseInt(authData.substring(25));
                        String address = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().toString().substring(1);
                        logPanel.append("Conexão recebida: " + address + ":" + port + "\r\n");
                        DataOutputStream response = new DataOutputStream(socket.getOutputStream());

                        response.writeUTF("WELCOME" + "\r\n" + "ONE" + "\r\n" + "WAIT");
                        logPanel.append("Reconexão solicitada por " + address + ":" + port + " (1) - esperar conexao" + "\r\n");
                        clientOneStatus.setText("Em espera");

                        peerWaiting = 1;
                        this.port = port;
                        this.address = address;

                        new ConnectionHolder(socket).start();
                        socket = null;

                    } else if (authData.startsWith("CINAPSYS CLIENT RECONN 2")) {
                        logPanel.append("Cliente 1 desconectado."  + "\r\n");
                        clientOneStatus.setText("Desconectado");
                        int port = Integer.parseInt(authData.substring(25));
                        String address = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().toString().substring(1);
                        logPanel.append("Conexão recebida: " + address + ":" + port + "\r\n");
                        DataOutputStream response = new DataOutputStream(socket.getOutputStream());

                        response.writeUTF("WELCOME" + "\r\n" + "TWO" + "\r\n" + "WAIT");
                        logPanel.append("Reconexão solicitada por " + address + ":" + port + " (2) - esperar conexão" + "\r\n");
                        clientTwoStatus.setText("Em espera");

                        peerWaiting = 2;
                        this.port = port;
                        this.address = address;

                        new ConnectionHolder(socket).start();
                        socket = null;

                    } else {
                        logPanel.append("Cliente tentou conectar com dado de autenticação incorreto." + "\r\n");
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
                logPanel.append("Conexão entre clientes realizada." + "\r\n");
                peerWaiting = -1;
            } catch (SocketException e) {
                if (peerWaiting == 1) {
                    peerWaiting = 0;
                    logPanel.append("Cliente 1 desconectado." + "\r\n");
                    clientOneStatus.setText("Desconectado");

                } else if (peerWaiting == 2) {
                    peerWaiting = 0;
                    logPanel.append("Cliente 2 desconectado." + "\r\n");
                    clientTwoStatus.setText("Desconectado");

                }
            } catch (Exception e) {
                logPanel.append("ERRO: " + e + "\r\n");
            }

        }

    }


}