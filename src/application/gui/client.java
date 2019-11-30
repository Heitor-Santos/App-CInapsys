package application.gui;

import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Vector;
import common.CompressInputStream;

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
    ServerSocket peerWaiterSocket;
    private Socket serverSocket;
    private Socket peerSocket;
    private DatagramSocket rtpSocket;
    String address;
    int serverPort;
    int peerPort;
    int serverID;
    boolean isPeerOnline;
    boolean isRTPSessionRunning;
    Vector<String> messageQueue;

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
                    new PeerRTPSender().start();
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

        try {
            peerWaiterSocket = new ServerSocket(peerPort);
        } catch (BindException e) {
            messagesArea.append("AVISO: " + e + "\r\n");
            messagesArea.append("Provavelmente você está executando duas instâncias de cliente na mesma máquina com a mesma porta. Poderão ocorrer efeitos indesejáveis." + "\r\n" + "\r\n");
        } catch (IOException e) {
            infoData.setText("Erro");
            messagesArea.append("ERRO: " + e + "\r\n");
            sendButton.setEnabled(false);
            callButton.setEnabled(false);
        }

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

                } catch (SocketException | EOFException e) {
                    messagesArea.append("INFO: Par desconectado. As mensagens seguintes serão enviadas quando ele se conectar novamente.\r\n");
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
                    infoData.setText("Conectando a servidor...");
                    serverSocket = new Socket(address, serverPort);
                    DataOutputStream auth = new DataOutputStream(serverSocket.getOutputStream());
                    auth.writeUTF("CINAPSYS CLIENT CONN " + peerPort);
                    infoData.setText("Esperando servidor...");

                    DataInputStream response = new DataInputStream(serverSocket.getInputStream());
                    String responseData = response.readUTF();

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
                    messagesArea.append("ERRO: Não foi possivel chegar ao destino.\r\n");
                    sendButton.setEnabled(false);
                    callButton.setEnabled(false);
                } catch (Exception e) {
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

                    String[] responseParts = responseData.split("\\r?\\n");

                    chatName.setText("Cinapsys Cliente " + serverID);

                    if (responseParts[2].equals("WAIT")) {
                        infoData.setText("Esperando par...");
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
                // new PeerRTPSender().start();
                new PeerRTPReceiver().start();
                infoData.setText("Em chamada");
                callButton.setText("Finalizar chamada");
                isRTPSessionRunning = true;
            } else {
                isRTPSessionRunning = false;
                callButton.setText("Iniciar chamada");
                if (isPeerOnline) {
                    infoData.setText("Conectado");
                }
            }
        }
    }

    class PeerRTPSender extends Thread {

        AudioInputStream recordingAudioStream;
        CompressInputStream rtpAudioStream;

        long timestamp;
        long seqNumber;
        byte[] ssrc = {61,74,95,3};
        int payloadSize;
        InetAddress IPServer;

        public PeerRTPSender () {
            payloadSize = 160;
            timestamp = 0;
            seqNumber = 0;
        }

        public void run () {
            try {
                // Abre porta UDP
                rtpSocket = new DatagramSocket(peerPort);
                IPServer = peerSocket.getInetAddress();

                // Prepara e inicializa o stream de áudio do microfone
                AudioFormat recordingFormat = new AudioFormat(8000, 8, 1, true, false); // descobrir qual funciona
                AudioFormat rtpAudioFormat = new AudioFormat(AudioFormat.Encoding.ULAW, 8000f, 8, 1, 1, 8000f, false);

                for (int i = 0; i < AudioSystem.getTargetEncodings(AudioFormat.Encoding.PCM_SIGNED).length; i++)
                    System.out.println(AudioSystem.getTargetEncodings(AudioFormat.Encoding.PCM_SIGNED)[i]);

                DataLine.Info info = new DataLine.Info(TargetDataLine.class, recordingFormat);
                if (!AudioSystem.isLineSupported(info)) {
                    messagesArea.append("ERRO: Não foi possível iniciar a chamada. Microfone não suportado." + "\r\n");
                    new PeerMessageSender("CINAPSYS INTERNAL: STOP RTP SERVER").start();
                    new PeerRTPSwitcher(false).start();
                }

                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(recordingFormat);
                line.start();
                recordingAudioStream = new AudioInputStream(line);
                // rtpAudioStream = AudioSystem.getAudioInputStream(rtpAudioFormat, recordingAudioStream);
                rtpAudioStream = new CompressInputStream(recordingAudioStream);

                // Envia pacotes a cada 20 milissegundos
                while (isRTPSessionRunning) {
                    sendRTPPacket();
                    Thread.sleep(20);
                }

                // Finaliza o stream de áudio do microfone
                line.stop();
                line.close();
                rtpSocket.close();

            } catch (Exception e) {
                infoData.setText("Erro");
                messagesArea.append("ERRO: Interrupção durante chamada - " + e + "\r\n");
                sendButton.setEnabled(false);
                callButton.setEnabled(false);
            }
        }

        public void sendRTPPacket() {

            // Cria um novo pacote RTP
            byte[] rtpPacket = new byte[12 + payloadSize];

            rtpPacket[0] = (byte) 0x80; // Versão 2, CC 0
            rtpPacket[1] = (byte) 0; // Marker 0, Payload Type 0 (uLaw)

            seqNumber = ++seqNumber & 0xFFFF; // Incrementa o número de sequência e limita em 16 bits

            // Adiciona o número de sequência no pacote
            rtpPacket[2] = (byte) ((seqNumber & 0xFF00) >> 8);
            rtpPacket[3] = (byte) ((seqNumber & 0x00FF));

            // Incrementa o timestamp pelo tamanho do payload
            timestamp = (timestamp + payloadSize) & 0xFFFFFFFF; // Incrementa o timestamp e limita em 32 bits

            // Adiciona o timestamp no pacote
            rtpPacket[4] = (byte) ((timestamp & 0xFF000000) >> 24);
            rtpPacket[5] = (byte) ((timestamp & 0x00FF0000) >> 16);
            rtpPacket[6] = (byte) ((timestamp & 0x0000FF00) >> 8);
            rtpPacket[7] = (byte) ((timestamp & 0x000000FF));

            // Adiciona o SSRC no pacote
            rtpPacket[8] = ssrc[0];
            rtpPacket[9] = ssrc[1];
            rtpPacket[10] = ssrc[2];
            rtpPacket[11] = ssrc[3];

            try {
                // carrega um novo pedaço do stream de áudio
                int n = 0;
                n = rtpAudioStream.read(rtpPacket, 12, payloadSize);
                // adiciona zero no final, caso o stream esteja incompleto
                if (n > 0) {
                    if (n < payloadSize) {
                        for (; n < payloadSize; n++) {
                            rtpPacket[n + 12] = (byte) 0xff;
                        }
                    }
                }

            } catch (Exception e) {
                infoData.setText("Erro");
                messagesArea.append("ERRO: Interrupção durante chamada - " + e + "\r\n");
                sendButton.setEnabled(false);
                callButton.setEnabled(false);
            }


            // Cria um pacote UDP com o rtpPacket
            DatagramPacket sendPacket = new DatagramPacket(rtpPacket, rtpPacket.length, IPServer, peerPort);

            try {
                // Envia o pacote
                rtpSocket.send(sendPacket);
            } catch (Exception e) {
                infoData.setText("Erro");
                messagesArea.append("ERRO: Interrupção durante chamada - " + e + "\r\n");
                sendButton.setEnabled(false);
                callButton.setEnabled(false);
            }
        }

    }

    class PeerRTPReceiver extends Thread {
        // TODO
        // Implementar a partir de leitor de RTP
        public PeerRTPReceiver () {}

        public void run () {

        }

    }



}

