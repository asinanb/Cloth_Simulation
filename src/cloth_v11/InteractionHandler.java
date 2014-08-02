/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cloth_v11;

import java.awt.Dimension;
import static java.awt.Toolkit.getDefaultToolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.Timer;

/**
 *
 * @author Jack Reardon
 * @created Jun 30, 2014
 * @edited Jun 30, 2014
 */
public class InteractionHandler extends JFrame implements ActionListener, KeyListener, MouseWheelListener {

    private Simulation simulationReference;

    private Dimension panelDimensions, panelPosition, screenDimensions;
    private int screenOffset = 50;
    
    private static final String cameraPositionFileName = "camera_positions.txt";
    private static final String defaultCameraPosition = "0, 10, 3";

    // Sensitivity to user inputs
    private float sensitivity = 0.2f;
    private Timer calculationTimer, visualTimer;
    private static final int calculationTimerFrequency = 3, visualTimerFrequency = 100;

    public InteractionHandler(String title, Simulation simulationReference) {
        super(title);

        AssignDefaultAttributes();

        this.addKeyListener(this);
        this.addMouseWheelListener(this);
        this.setFocusable(true);
        this.setVisible(true);

        this.simulationReference = simulationReference;
    }

    private void AssignDefaultAttributes() {

        panelDimensions = new Dimension(500, 200);
        panelPosition = DeterminePanelPosition();

        this.setSize(panelDimensions.width, panelDimensions.height);
        this.setLocation(panelPosition.width, panelPosition.height);
    }

    // Put the panel in the middle of the screen
    private Dimension DeterminePanelPosition() {
        screenDimensions = getDefaultToolkit().getScreenSize();
        return new Dimension(
                (screenDimensions.width - panelDimensions.width) / 2,
                (screenDimensions.height - panelDimensions.height - screenOffset));
    }
    
    public double[][] LoadCameraPositions() throws FileNotFoundException, IOException {
        File file = new File(cameraPositionFileName);
        if (file.exists()) {
            FileReader reader = new FileReader(file);
            BufferedReader textReader = new BufferedReader(reader);
            
            String line;
            String[] lines = new String[10];
            
            double[][] cameraPositions = new double[10][3];
            // There should be 10 lines in this file
            for (int count = 0; count < 10; count ++) {
                line = textReader.readLine();
                if (line == null) {
                    SetAllCamerasSame(lines);
                    count = 10; // exit the loop
                } else {
                    lines[count] = line;
                }
            }
            textReader.close();
            
            for (int count = 0; count < 10; count++) {
                cameraPositions[count] = ReadCameraPosition(lines[count]);
            }
            
            return cameraPositions;
            
        } else {
            Arit.NL("camera_positions.txt does not exist");
        }
        return null;
    }
    
    // Returns camera positions in the String form "double, double, double" to
    // array of double form
    private double[] ReadCameraPosition(String cameraPositionAsString) {
        double[] cameraPosition = new double[3];
        String[] cameraCoordinates = cameraPositionAsString.split(", ");
        for (int count = 0; count < 3; count++) {
            cameraPosition[count] =
                    Double.parseDouble(cameraCoordinates[count]);
        }
        
        return cameraPosition;
    }
    
    private void SetAllCamerasSame(String[] cameraPositionsAsStrings) {
        for (int count = 0; count < 10; count++) {
            cameraPositionsAsStrings[count] = defaultCameraPosition;
        }
    }
    
    public void SetCameraPositions(double[][] cameraPositions) throws IOException {
        File file = new File(cameraPositionFileName);
        if (file.exists()) {
            FileWriter writer = new FileWriter(file);
            BufferedWriter textWriter = new BufferedWriter(writer);
            
            String line = "";
            for (double[] cameraPosition : cameraPositions) {
                line = cameraPosition[0] + ", "
                        + cameraPosition[1] + ", "
                        + cameraPosition[2];
                textWriter.append(line);
                textWriter.newLine();
            }
            
            textWriter.close();
        }
    }

    public float GetSensitivity() {
        return sensitivity;
    }

    public void Run() {
        calculationTimer = new Timer(calculationTimerFrequency, this);
        calculationTimer.start();

        visualTimer = new Timer(visualTimerFrequency, this);
        visualTimer.start();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if ('0' <= keyCode && keyCode <= '9') {
            simulationReference.RevertCameraPosition(keyCode - 48);
        } else {
            switch (keyCode) {
                case 38: // 'up' key
                    // Up
                    simulationReference.AdjustCameraPosition('u');
                    break;
                case 40: // 'down' key
                    // Down
                    simulationReference.AdjustCameraPosition('d');
                    break;
                case 37: // 'left' key
                    // Left
                    simulationReference.AdjustCameraPosition('l');
                    break;
                case 39: // 'right' key
                    // Right
                    simulationReference.AdjustCameraPosition('r');
                    break;
                case 44: // '<' key
                    // Forward
                    simulationReference.AdjustCameraPosition('f');
                    break;
                case 46: // '>' key
                    // Back
                    simulationReference.AdjustCameraPosition('b');
                    break;
                case '0': // '0' key
                    // Revert camera
                    simulationReference.AdjustCameraPosition('0');
                    break;
                case 'c': // 'c' key
                    // Screen capture 'c'
                    // TODO
                    break;
                case 32: // space key
                    // Pause/start the simulation
                    // TODO
                    break;
                case 83: // 's' key
                    // Set the camera position
                    // This must be followed by a number key to have any effect
                    simulationReference.WillSetCameraPositionShortcut();
                    break;
                default:
                    // Something else
                    //Arit.NL(e.getKeyCode());
                    break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if ((calculationTimer != null) && (e.getSource() == calculationTimer)) {
            simulationReference.Update();
        }

        if ((visualTimer != null) && (e.getSource() == visualTimer)) {
            simulationReference.UpdateVisual();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() == -1) {
            // Mouse wheel up - spin left
            simulationReference.AdjustCameraRotation('l');
        } else {
            // Mouse wheel down - spin right
            simulationReference.AdjustCameraRotation('r');
        }
    }

}
