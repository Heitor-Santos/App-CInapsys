package application.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class init {
    private JRadioButton clientSelector;
    private JRadioButton serverSelector;
    private JButton startButton;
    private JTextField serverPortForServerField;
    private JTextField serverAddressField;
    private JTextField serverPortForClientField;
    private JTextField peerPortOneField;
    private JCheckBox twoLocalClientsCheckBox;
    private JTextField peerPortTwoField;
    private JPanel initPanel;
    application.gui.client clientOne;
    application.gui.client clientTwo;
    application.gui.server server;

    public init() {
        JFrame janela = new JFrame("Cinapsys");
        janela.setContentPane(initPanel);
        janela.setVisible(true);
        janela.pack();
        janela.setSize(720,480);
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Windows

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            SwingUtilities.updateComponentTreeUI(janela);
        } catch (Exception e) {

        }


        ButtonGroup modeSelector = new ButtonGroup();
        modeSelector.add(clientSelector);
        modeSelector.add(serverSelector);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (modeSelector.isSelected(clientSelector.getModel())) {
                    String serverAddress = serverAddressField.getText();
                    int serverPort = 0;
                    int peerPortOne = 0;
                    int peerPortTwo = 0;
                    boolean readyToStart = true;
                    try {
                        serverPort = Integer.parseInt(serverPortForClientField.getText());
                    } catch (NumberFormatException se) {
                        JOptionPane.showMessageDialog(null, "A porta do servidor não é um número. Digite um número de porta válido e tente novamente.");
                        readyToStart = false;
                    }
                    try {
                        peerPortOne = Integer.parseInt(peerPortOneField.getText());
                    } catch (NumberFormatException se) {
                        JOptionPane.showMessageDialog(null, "A porta para o par 1 não é um número. Digite um número de porta válido e tente novamente.");
                        readyToStart = false;
                    }

                    if (twoLocalClientsCheckBox.isSelected()) {
                        try {
                            peerPortTwo = Integer.parseInt(peerPortTwoField.getText());
                            if (readyToStart && peerPortOne == peerPortTwo) {
                                JOptionPane.showMessageDialog(null, "O par 1 e 2 não podem ter porta igual. Altere a porta de um dos pares e tente novamente.");
                                readyToStart = false;
                            }
                        } catch (NumberFormatException se) {
                            JOptionPane.showMessageDialog(null, "A porta para o par 2 não é um número. Digite um número de porta válido e tente novamente.");
                            readyToStart = false;
                        }
                    }

                    if (readyToStart) {
                        clientOne = new application.gui.client(serverPort, peerPortOne, serverAddress);
                        if (twoLocalClientsCheckBox.isSelected()) {
                             clientTwo = new application.gui.client(serverPort, peerPortTwo, serverAddress);
                        }
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
        twoLocalClientsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (twoLocalClientsCheckBox.isSelected()) {
                    peerPortTwoField.setEnabled(true);
                } else {
                    peerPortTwoField.setEnabled(false);
                }
            }
        });
    }

}
