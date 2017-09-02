/**
 * JRelStarflight realizes the special relativistic and warp flight 
 * through the Hipparcos star field.
 * 
 * Copyright (c) 2011, 2017, Thomas Mueller
 * 
 * @author   Thomas Mueller
 * @version  1.1
 */
package tauzero7.java.relavis.relstar;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;
import java.util.Locale;

import javax.media.opengl.awt.GLCanvas;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

@SuppressWarnings("serial")
public class JRelStarFlight extends JFrame {

    private GLRenderer renderer;
    private MyMouseAdapter mouseAdapter;

    private Timer mTimer;
    private int mDelay = 40;
    private double mCurrPos = 0.0f;

    private javax.media.opengl.awt.GLCanvas glCanvas;
    private javax.swing.JComboBox<String> jComboBoxST;
    private javax.swing.JComboBox<String> jComboBoxCam;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JLabel jLabelDist;
    private javax.swing.JLabel jLabelVel;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItemAbout;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JFormattedTextField jTextFieldDist;
    private javax.swing.JFormattedTextField jTextFieldVel;
    private javax.swing.JToggleButton jToggleButtonPlay;

    public JRelStarFlight() {
        Locale englishLoc = new Locale("en_US");
        Locale.setDefault(englishLoc);
        
        // to prevent glcanvas drawing over the jmenubar
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        initComponents();
        setTitle("JRelStarFlight");

        renderer = new GLRenderer(glCanvas.getWidth(), glCanvas.getHeight());
        glCanvas.addGLEventListener(renderer);

        mouseAdapter = new MyMouseAdapter(glCanvas, renderer, this);
        glCanvas.addMouseListener(mouseAdapter);
        glCanvas.addMouseMotionListener(mouseAdapter);

        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mCurrPos += renderer.getBeta() * Defs.timeStep;
                jTextFieldDist.setText(String.format("%.2f", mCurrPos));
                renderer.setCurrentPosition(mCurrPos);
                glCanvas.repaint();
            }
        };

        glCanvas.requestFocusInWindow();
        mTimer = new Timer(mDelay, taskPerformer);
    }

    private void close() {
        mTimer.stop();
        super.dispose();
        System.out.println("Bye bye");
        System.exit(0);
    }

    public void dispose() {
        close();
    }

    /*
     * Init GUI components
     */
    private void initComponents() {
        glCanvas = new javax.media.opengl.awt.GLCanvas();
        jLabelVel = new javax.swing.JLabel();
        jLabelDist = new javax.swing.JLabel();
        jToggleButtonPlay = new javax.swing.JToggleButton();
        jButtonReset = new javax.swing.JButton();
        jTextFieldVel = new javax.swing.JFormattedTextField();
        jTextFieldDist = new javax.swing.JFormattedTextField();
        jMenuBar = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemAbout = new javax.swing.JMenuItem();
        jMenuItemExit = new javax.swing.JMenuItem();

        // glCanvas.setMinimumSize(new Dimension(1200, 500));
        // glCanvas.setMaximumSize(new Dimension(1200, 500));
        // glCanvas.setPreferredSize(new Dimension(1200, 500));

        jComboBoxST = new JComboBox<String>(new String[] { "Special relativity", "Warp" });
        jComboBoxST.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                renderer.setSpacetime(jComboBoxST.getSelectedIndex());
                renderer.setMouseMotion(0);
                setVelocity(renderer.getBeta());
                glCanvas.repaint();
            }
        });

        jLabelVel.setText("velocity:");
        jLabelDist.setText("distance to origin:");

        jComboBoxCam = new JComboBox<String>(new String[] { "Fullsky camera", "Pinhole camera" });
        jComboBoxCam.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                renderer.setCamera(jComboBoxCam.getSelectedIndex());
                glCanvas.repaint();
            }
        });

        jToggleButtonPlay.setText("Play");
        jToggleButtonPlay.setPreferredSize(new Dimension(80, 25));
        jToggleButtonPlay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (mTimer.isRunning()) {
                    mTimer.stop();
                    jToggleButtonPlay.setText("Play");
                } else {
                    mTimer.start();
                    jToggleButtonPlay.setText("Pause");
                }
            }
        });

        jButtonReset.setText("Reset");
        jButtonReset.setPreferredSize(new Dimension(80, 25));
        jButtonReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mCurrPos = 0.0;
                jTextFieldVel.setText("0.000");
                jTextFieldDist.setText("0.000");
                renderer.setMotion(0.0);
                renderer.setCurrentPosition(mCurrPos);
                glCanvas.repaint();
            }
        });

        jTextFieldVel
                .setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(new DecimalFormat("#,##0.000"))));
        jTextFieldVel.setText("0.000");
        jTextFieldVel.setPreferredSize(new Dimension(100, 25));
        jTextFieldVel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                double actBeta = renderer.setMotion(Double.parseDouble(jTextFieldVel.getText()));
                jTextFieldVel.setText(String.format("%.3f", actBeta));
                glCanvas.repaint();
            }
        });

        jTextFieldDist
                .setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(new DecimalFormat("#,##0.000"))));
        jTextFieldDist.setText("0.000");
        jTextFieldDist.setPreferredSize(new Dimension(100, 25));
        jTextFieldDist.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                mCurrPos = Double.parseDouble(jTextFieldDist.getText());
                renderer.setCurrentPosition(mCurrPos);
                glCanvas.repaint();
            }
        });

        jMenuItemAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
        jMenuItemAbout.setText("About");
        jMenuItemAbout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                //JOptionPane.showMessageDialog(null,
                        //"JRelStarFlight, Version 1.0\n\nCopyright (c) 2011, Thomas Mueller\nVisualization Research Center (VISUS)\nAllmandring 19, 70569 Stuttgart, Germany\n\nEmail: Thomas.Mueller@visus.uni-stuttgart.de",
                        //"About JRelStarFlight", JOptionPane.INFORMATION_MESSAGE);
                JOptionPane.showMessageDialog(null,
                "JRelStarFlight, Version 1.1\n\nCopyright (c) 2011,2017 Thomas Müller\nHaus der Astronomie\nKönigstuhl 17, 69117 Heidelberg, Germany\n\nEmail: tmueller@mpia.de"
                        + "\n\nformerly: Visualization Research Center (VISUS)\nAllmandring 19, 70569 Stuttgart",
                "About JRelStarFlight", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        jMenuItemExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                close();
            }
        });

        jMenuFile.setText("File");
        jMenuFile.setMnemonic(KeyEvent.VK_F);
        jMenuFile.add(jMenuItemAbout);
        jMenuFile.add(jMenuItemExit);
        jMenuBar.add(jMenuFile);
        setJMenuBar(jMenuBar);

        JPanel toolsPanel = new JPanel();
        //SpringLayout springLayout = new SpringLayout();
        // toolsPanel.setLayout(springLayout);

        toolsPanel.add(jComboBoxST);
        toolsPanel.add(jComboBoxCam);
        toolsPanel.add(jLabelVel);
        toolsPanel.add(jTextFieldVel);
        toolsPanel.add(jLabelDist);
        toolsPanel.add(jTextFieldDist);
        toolsPanel.add(jButtonReset);
        toolsPanel.add(jToggleButtonPlay);

        JPanel cPanel = new JPanel();
        cPanel.setLayout(new BorderLayout());
        cPanel.add(glCanvas);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        contentPane.add(cPanel);
        contentPane.add(toolsPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        // setLocationRelativeTo(null);
        // setResizable(false);
        setSize(1200, 700);
    }

    public void setVelocity(double beta) {
        jTextFieldVel.setText(String.format("%.2f", beta));
    }

    /*
     * main method
     */
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new JRelStarFlight().setVisible(true);
            }
        });
    }

}

/**
 * Mouse adaptor class definition
 * 
 * @author Thomas Mueller.
 */
class MyMouseAdapter extends MouseAdapter implements MouseMotionListener {

    private GLCanvas canvas = null;
    private GLRenderer renderer = null;
    private JRelStarFlight relstar = null;

    private int button = MouseEvent.NOBUTTON;
    private int[] mouseLastPos = { 0, 0 };

    MyMouseAdapter(GLCanvas canvas, GLRenderer renderer, JRelStarFlight relstar) {
        this.canvas = canvas;
        this.renderer = renderer;
        this.relstar = relstar;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX() - mouseLastPos[0];
        int y = e.getY() - mouseLastPos[1];

        if (button == MouseEvent.BUTTON1) {
            renderer.setMouseRot(x, 0);
            canvas.repaint();
        } else if (button == MouseEvent.BUTTON2) {
            renderer.setMouseRot(0, y);
            canvas.repaint();
        } else if (button == MouseEvent.BUTTON3) {
            renderer.setMouseMotion(-y);
            relstar.setVelocity(renderer.getBeta());
            canvas.repaint();
        }
        super.mouseDragged(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        button = e.getButton();
        mouseLastPos[0] = e.getX();
        mouseLastPos[1] = e.getY();
        renderer.setMouseRelease();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        button = MouseEvent.NOBUTTON;
        this.canvas.repaint();
        super.mouseReleased(e);
    }
}
