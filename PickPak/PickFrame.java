package PickPak;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import arduino.*;

import java.util.ArrayList;

public class PickFrame extends JFrame implements ActionListener {

    private int aantalBestellingen = 0;
    private boolean aanHetKalibreren = false;
    private boolean stop = false;
    private int picknr = 0;
    ArrayList<Item> bestelling;

    private PickPak pickpak;

    private JTable tabel;

    private static final class Lock {
    }
    private final Object lock = new Lock();

    private Thread t;

    public static boolean running = true;

    private GeavanceerdDialoog jdGeavanceerd;

    private int BPPalgoritme;

    private JTextField jtfFile;
    private JLabel jlFile;
    private JButton jbbevestig, jbStop, geavanceerd;

    private GridPanel gridPanel;
    private DozenPanel dozenPanel;

    private JPanel p;

    private Arduino arduinoKraan, arduinoSchijf;

    public PickFrame(PickPak pickpak) {
        setTitle("GUI");
        setSize(1920, 1080);
        setLayout(new FlowLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        p = new JPanel(new GridBagLayout());

        GridBagConstraints d = new GridBagConstraints();
        GridBagConstraints c = new GridBagConstraints();

        this.pickpak = pickpak;

        jlFile = new JLabel("Bestelling: ");
        add(jlFile);

        jtfFile = new JTextField(10);
        jtfFile.setText("bestelling.xml");
        add(jtfFile);

        jbbevestig = new JButton("Start");
        jbbevestig.addActionListener(this);
        add(jbbevestig);
        jbbevestig.setEnabled(true);

        jbStop = new JButton("Afbreken");
        jbStop.setEnabled(false);
        jbStop.addActionListener(this);

//        jbStop.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                stop = true;
//            }
//        });
        add(jbStop);

        geavanceerd = new JButton("Geavanceerd");
        geavanceerd.addActionListener(this);
        add(geavanceerd);

        tabel = pickpak.maakTabel();

        add(tabel);
        add(new JScrollPane(tabel));
        tabel.setPreferredScrollableViewportSize(tabel.getPreferredSize());
        tabel.setFillsViewportHeight(true);

        tabel.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        TableColumnModel colModel=tabel.getColumnModel();
        colModel.getColumn(0).setMaxWidth(40);
        colModel.getColumn(2).setMaxWidth(60);
        colModel.getColumn(3).setMaxWidth(100);
        colModel.getColumn(4).setMaxWidth(80);

        gridPanel = new GridPanel(pickpak);

        d.gridx = 0;
        d.gridy = 0;

        p.add(gridPanel, d);

        dozenPanel = new DozenPanel(pickpak);

        c.gridx = 300;
        c.gridy = 0;

        p.add(dozenPanel, c);

        add(p, BorderLayout.SOUTH);

        setVisible(true);

        t = new Thread() {

            @Override
            public void run() {

                jbbevestig.setEnabled(false);

                aantalBestellingen++;
                if (aantalBestellingen > 1) { // aanpassen!

                    pickpak.resetRobots();

                    p.paintImmediately(0, 0, 1920, 1080);

                }

                tekenRoute(jtfFile.getText());

                tabel = pickpak.maakTabel(true);

                pickBestelling();

                return;
            }
        };

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == jbbevestig) {
            if (checkRobotConnection()) {
                if (!jtfFile.getText().equals("")) {
                    if (!pickpak.checkBestrelling(jtfFile.getText())) {
                        JOptionPane.showMessageDialog(null, "Kan bestand niet lezen");
                    }
                    else {
                        t.start();
                        jbStop.setEnabled(true);
                    }
                }
                else {
                    JOptionPane.showMessageDialog(null, "Geef een bestelling op.");
                }

            }
            else {
                JOptionPane.showMessageDialog(this, "Niet verbonden met de pick- of inpakrobot.");
            }

        } else if (e.getSource() == geavanceerd) {
            if (jdGeavanceerd == null) {
                jdGeavanceerd = new GeavanceerdDialoog(this, pickpak);
            } else {
                jdGeavanceerd.setVisible(true);
            }
            jdGeavanceerd.setVisible(false);
            BPPalgoritme = jdGeavanceerd.getBPPalgoritme();
            arduinoKraan = jdGeavanceerd.getArduinoKraan();
            arduinoSchijf = jdGeavanceerd.getArduinoSchijf();

            if (jdGeavanceerd.connected()) {
                jbbevestig.setEnabled(true);
                jbStop.setEnabled(true);
            } else {
                jbbevestig.setEnabled(false);
                jbStop.setEnabled(false);
            }
            //jdGeavanceerd.dispose();

        } else if (e.getSource() == jbStop) {
            if (jbStop.getText().equals("Afbreken")) {
                jbStop.setText("Reset");

                arduinoKraan.serialWrite('f');

                t.stop();

            } else if (jbStop.getText().equals("Reset")) {
                jbStop.setText("Afbreken");
                jbStop.setEnabled(false);
                
                arduinoKraan.serialWrite("c00");
                arduinoSchijf.serialWrite("c1");
                
                char c = '.';
                do{
                    c = arduinoKraan.serialRead().charAt(0);
                }while(c != 'q');
                
                
                try{
                    arduinoKraan.serialWrite('l');
                    Thread.sleep(500);
                    arduinoKraan.serialWrite('s');
                    arduinoKraan.serialWrite('d');
                    Thread.sleep(500);
                    arduinoKraan.serialWrite('s');
                }catch(Exception ex){
                    
                }
                
                
                

                

                try {
                    Thread.sleep(3000);
                } catch (Exception ex) {

                }

                pickpak.resetRobots();

            }

        }

        repaint();
    }

    private void tekenRoute(String f) {
        try {
            bestelling = null;
            bestelling = pickpak.leesBestelling(f);

            if (bestelling == null) {
                System.out.println("Bestelling is null\n...");
            } else {
                ArrayList<Integer> route = pickpak.voerTSPuit(bestelling);

                pickpak.voerBPPuit(route, BPPalgoritme);
            }

        } catch (Exception ex) {
            System.out.println("Er ging iets mis\n...");
        }
    }

    public boolean checkRobotConnection() {
        try {
            arduinoKraan.getSerialPort();
            arduinoSchijf.getSerialPort();
            return true;
        } catch (NullPointerException npe) {
            return false;
        }
    }

    private void reconnect() {
        try {
            arduinoKraan.openConnection();
            arduinoSchijf.openConnection();
        } catch (Exception ex) {

        }

    }

    public void pickBestelling() {
        if (pickpak.route == null) {
            return;
        } else {

            for (int it = 1; it < pickpak.route.size() - 1; it++) {


                pickpak.draaiSchijf(it, arduinoSchijf);

                char s = '.';
                do {
                    try {
                        s = arduinoSchijf.serialRead().charAt(0);
                    } catch (Exception ex) {

                    }
                } while (s != 'd'); // schijf draaien

                //System.out.println(s);
                //while(){
                // wacht op signaal
                // }
                p.paintImmediately(0, 0, 1920, 1080);

                pickpak.beweegKraan(it, arduinoKraan);

                System.out.println("KRAAN BEWOGEN\n...");

                p.paintImmediately(0, 0, 1920, 1080);

                arduinoKraan.serialWrite('p'); //push

                System.out.println("GEPUSHT\n...");

                pickpak.setPush(true);

                p.paintImmediately(0, 0, 1920, 1080);

                char t = '.';
                do {
                    try {
                        t = arduinoSchijf.serialRead().charAt(0);
                    } catch (Exception ex) {

                    }
                } while (t != 'p'); //sensor

                //try {
                //    Thread.sleep(1000);
                //} catch (Exception ex) {
                //}
                System.out.println(t);

                pickpak.setPush(false);

                p.paintImmediately(0, 0, 1920, 1080);

                pickpak.werkDoosInhoudBij(it);

                p.paintImmediately(0, 0, 1920, 1080);

            }
            arduinoKraan.serialWrite("c00");
            arduinoSchijf.serialWrite("c1");

            p.paintImmediately(0, 0, 1920, 1080);

            jbbevestig.setEnabled(true);
        }
    }
}
