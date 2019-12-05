package application.gui;

import javax.swing.*;
import java.awt.event.*;
import java.net.URL;

public class init {
    private JRadioButton clientSelector;
    private JRadioButton serverSelector;
    private JButton startButton;
    private JTextField serverPortForServerField;
    private JTextField serverAddressField;
    private JTextField serverPortForClientField;
    private JTextField peerPortField;
    private JPanel initPanel;
    application.gui.client clientOne;
    application.gui.server server;

    public init() {
        JFrame janela = new JFrame("Cinapsys");
        janela.setContentPane(initPanel);
        janela.setVisible(true);
        janela.pack();
        janela.setSize(720,480);
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        URL iconURL = getClass().getResource("/resources/init_icon.png");
        ImageIcon icon = new ImageIcon(iconURL);
        janela.setIconImage(icon.getImage());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(janela);
        } catch (Exception e) {}

        ButtonGroup modeSelector = new ButtonGroup();
        modeSelector.add(clientSelector);
        modeSelector.add(serverSelector);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (modeSelector.isSelected(clientSelector.getModel())) {
                    String serverAddress = serverAddressField.getText();
                    int serverPort = 0;
                    int peerPort = 0;
                    boolean readyToStart = true;
                    try {
                        serverPort = Integer.parseInt(serverPortForClientField.getText());
                    } catch (NumberFormatException se) {
                        JOptionPane.showMessageDialog(null, "A porta do servidor não é um número. Digite um número de porta válido e tente novamente.");
                        readyToStart = false;
                    }
                    try {
                        peerPort = Integer.parseInt(peerPortField.getText());
                    } catch (NumberFormatException se) {
                        JOptionPane.showMessageDialog(null, "A porta para o par não é um número. Digite um número de porta válido e tente novamente.");
                        readyToStart = false;
                    }

                    if (readyToStart && serverPort == peerPort) {
                        JOptionPane.showMessageDialog(null, "As portas do servidor e para o par não podem ser iguais.  Altere a porta de um dos pares e tente novamente.");
                        readyToStart = false;
                    }

                    if (readyToStart) {
                        clientOne = new application.gui.client(serverPort, peerPort, serverAddress);
                        janela.dispose();
                    }

                } else if (modeSelector.isSelected(serverSelector.getModel())) {
                    int serverPort = 0;
                    boolean readyToStart = true;

                    try {
                        serverPort = Integer.parseInt(serverPortForServerField.getText());
                    } catch (NumberFormatException se) {
                        JOptionPane.showMessageDialog(null, "A porta do servidor não é um número. Digite um número de porta válido e tente novamente.");
                        readyToStart = false;
                    }

                    if (readyToStart) {
                        janela.dispose();
                        server = new application.gui.server(serverPort);
                    }

                }

            }
        });

    }

}
