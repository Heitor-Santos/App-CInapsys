package application.gui;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Vector;

public class client {
    private JPanel clientPanel;
    private JTextField messageField;
    private JButton sendButton;
    private JTextArea messagesArea;
    private JPanel topBar;
    private JLabel chatName;
    private JPanel actionPanel;
    private JButton callButton;
    private JLabel infoData;
    private Socket serverSocket;
    private Socket peerSocket;
    String address;
    int serverPort;
    int peerPort;
    int serverID;
    boolean isPeerOnline;
    boolean isRTPSessionRunning;
    Vector<String> messageQueue;
    Thread rtpSenderThread;
    Thread rtpReceiverThread;

    public client(int serverPort, int peerPort, String address) {
        JFrame janela = new JFrame("Cinapsys Cliente");
        janela.setContentPane(clientPanel);
        janela.setVisible(true);
        janela.pack();
        janela.setSize(480,720);
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.isPeerOnline = false;
        this.isRTPSessionRunning = false;
        this.address = address;
        this.serverPort = serverPort;
        this.peerPort = peerPort;
        this.messageQueue = new Vector();
        this.rtpSenderThread = null;
        this.rtpReceiverThread = null;
        this.serverID = 0;

        sendButton.setEnabled(false);
        callButton.setEnabled(false);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PeerMessageSender("CINAPSYS MESSAGE: " + messageField.getText()).start();
            }
        });

        callButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isRTPSessionRunning) {
                    new PeerMessageSender("CINAPSYS INTERNAL: STOP RTP SERVER").start();
                    new PeerRTPSwitcher(false).start();
                } else {
                    new PeerMessageSender("CINAPSYS INTERNAL: START RTP SERVER").start();
                    new PeerRTPSwitcher(true).start();
                }
            }
        });

        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER){
                    sendButton.doClick();
                }
            }
        });

        new ServerMessageSender(false).start();

    }

    class PeerMessageSender extends Thread {
        String message;

        public PeerMessageSender(String message) {
                this.message = message;
        }

        public void run() {
            try {
                if (isPeerOnline) {
                    DataOutputStream saida = new DataOutputStream(peerSocket.getOutputStream());
                    saida.writeUTF(message);
                    if (message.startsWith("CINAPSYS MESSAGE: ")) {
                        messagesArea.append("ENVIADO: " + message.substring(18) + "\r\n");
                        messageField.setText("");
                    }
                } else {
                    messageQueue.add(message);
                    if (message.startsWith("CINAPSYS MESSAGE: ")) {
                        messagesArea.append("ESPERANDO PARA ENVIAR: " + message.substring(18) + "\r\n");
                        messageField.setText("");
                    }
                }
            } catch (BindException ae) {
                infoData.setText("Erro");
                messagesArea.append("ERRO: endereco em uso\r\n");
                sendButton.setEnabled(false);
                callButton.setEnabled(false);
            } catch (Exception ae) {
                infoData.setText("Erro");
                messagesArea.append("ERRO: " + ae + "\r\n");
                sendButton.setEnabled(false);
                callButton.setEnabled(false);
            }
        }
    }

    class PeerMessageReceiver extends Thread {

        public PeerMessageReceiver() {}

        public void run() {
            while (true) {
                try {
                    DataInputStream entrada = new DataInputStream(peerSocket.getInputStream());
                    String message = entrada.readUTF();

                    if (message.startsWith("CINAPSYS MESSAGE: ")) {
                        message = message.substring(18);
                        messagesArea.append("RECEBIDO: " + message + "\r\n");
                    } else if (message.startsWith("CINAPSYS INTERNAL: ")) {
                        message = message.substring(19);
                        if (message.equals("START RTP SERVER") && !isRTPSessionRunning) {
                            new PeerRTPSwitcher(true).start();
                        } else if (message.equals("STOP RTP SERVER")) {
                            new PeerRTPSwitcher(false).start();
                        }
                    }

                } catch (SocketException e) {
                    messagesArea.append("INFO: Par desconectado.\r\n");
                    infoData.setText("Desconectado");
                    isPeerOnline = false;
                    callButton.setEnabled(false);
                    if (isRTPSessionRunning) {
                        new PeerRTPSwitcher(false).start();
                    }

                    new ServerMessageSender(true).start();
                    break;
                } catch (Exception e) {
                    infoData.setText("Erro");
                    messagesArea.append("ERRO: " + e + "\r\n");
                    sendButton.setEnabled(false);
                    callButton.setEnabled(false);
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    class ServerMessageSender extends Thread {
        boolean isReconnection;

        public ServerMessageSender(boolean isReconnection) {
            this.isReconnection = isReconnection;
        }

        public void run() {
            if (!isReconnection) {
                try {
                    serverSocket = new Socket(address, serverPort);
                    infoData.setText("Conectando a servidor...");
                    DataOutputStream auth = new DataOutputStream(serverSocket.getOutputStream());
                    auth.writeUTF("CINAPSYS CLIENT CONN " + peerPort);
                    infoData.setText("Esperando servidor...");

                    DataInputStream response = new DataInputStream(serverSocket.getInputStream());
                    String responseData = response.readUTF();

                    ServerSocket peerWaiterSocket = null;

                    String[] responseParts = responseData.split("\\r?\\n");

                    if (!responseParts[1].equals("FULL")) {
                        if (responseParts[1].equals("ONE")) {
                            serverID = 1;
                        } else if (responseParts[1].equals("TWO")) {
                            serverID = 2;
                        }
                        chatName.setText("Cinapsys Cliente " + serverID);

                        if (responseParts[2].equals("WAIT")) {
                            infoData.setText("Esperando par...");
                            peerWaiterSocket = new ServerSocket(peerPort);
                            peerSocket = peerWaiterSocket.accept();
                            DataOutputStream unhold = new DataOutputStream(serverSocket.getOutputStream());
                            unhold.writeUTF("UNHOLD");

                        } else if (responseParts[2].equals("CONNECT")) {
                            infoData.setText("Conectando a " + responseParts[3] + ":" + responseParts[4]);
                            peerSocket = new Socket(responseParts[3], Integer.parseInt(responseParts[4]));
                        }

                        infoData.setText("Conectado");
                        isPeerOnline = true;

                        sendButton.setEnabled(true);
                        callButton.setEnabled(true);

                        new PeerMessageReceiver().start();

                    } else {
                        infoData.setText("Servidor cheio");
                        messagesArea.append("INFO: Servidor negou a conexão porque está cheio.\r\n");
                    }

                    serverSocket.close();


                } catch (ConnectException e) {
                    infoData.setText("Erro");
                    messagesArea.append("ERRO: Nao foi possivel chegar ao destino.\r\n");
                    sendButton.setEnabled(false);
                    callButton.setEnabled(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    infoData.setText("Erro");
                    messagesArea.append("ERRO: " + e + "\r\n");
                    sendButton.setEnabled(false);
                    callButton.setEnabled(false);
                }
            } else {
                try {
                    serverSocket.close();
                    peerSocket.close();

                    serverSocket = new Socket(address, serverPort);
                    infoData.setText("Conectando a servidor...");
                    DataOutputStream auth = new DataOutputStream(serverSocket.getOutputStream());
                    auth.writeUTF("CINAPSYS CLIENT RECONN " + serverID + " " + peerPort);
                    infoData.setText("Esperando servidor...");

                    DataInputStream response = new DataInputStream(serverSocket.getInputStream());
                    String responseData = response.readUTF();

                    ServerSocket peerWaiterSocket = null;

                    String[] responseParts = responseData.split("\\r?\\n");

                    chatName.setText("Cinapsys Cliente " + serverID);

                    if (responseParts[2].equals("WAIT")) {
                        // flush se demorar demais
                        infoData.setText("Esperando par...");
                        peerWaiterSocket = new ServerSocket(peerPort);
                        peerSocket = peerWaiterSocket.accept();
                    }

                    DataOutputStream unhold = new DataOutputStream(serverSocket.getOutputStream());
                    unhold.writeUTF("UNHOLD");
                    serverSocket.close();

                    infoData.setText("Conectado");
                    messagesArea.append("INFO: Par reconectado." + "\r\n");
                    isPeerOnline = true;

                    sendButton.setEnabled(true);
                    callButton.setEnabled(true);

                    new PeerMessageReceiver().start();

                    for (int i = 0; i < messageQueue.size(); i++) {
                        new PeerMessageSender(messageQueue.elementAt(i)).start();
                    }
                    messageQueue.clear();

                } catch (ConnectException e) {
                    infoData.setText("Erro");
                    messagesArea.append("ERRO: Nao foi possivel chegar ao destino.\r\n");
                    sendButton.setEnabled(false);
                    callButton.setEnabled(false);
                } catch (Exception e) {
                    infoData.setText("Erro");
                    e.printStackTrace();
                    messagesArea.append("ERRO: " + e + "\r\n");
                    sendButton.setEnabled(false);
                    callButton.setEnabled(false);
                }

            }
        }
    }

    class PeerRTPSwitcher extends Thread {
        boolean switcher;

        public PeerRTPSwitcher(boolean switcher) {
            this.switcher = switcher;
        }

        public void run() {
            if (switcher) {
                //rtpSenderThread = new RTPSenderThread().start();
                //rtpReceiverThread = new RTPReceiverThread().start();
                infoData.setText("Em chamada");
                callButton.setText("Finalizar chamada");
                isRTPSessionRunning = true;
            } else {
                rtpSenderThread = null;
                rtpReceiverThread = null;
                isRTPSessionRunning = false;
                callButton.setText("Iniciar chamada");
                if (isPeerOnline) {
                    infoData.setText("Conectado");
                }
            }
        }
    }

    class PeerRTPSender extends Thread {
        // TODO
        // Copiar implementação de pacotes do outro projeto
        public PeerRTPSender () {}

    }

    class PeerRTPReceiver extends Thread {
        // TODO
        // Implementar a partir de leitor de RTP
        public PeerRTPReceiver () {}

    }


}

