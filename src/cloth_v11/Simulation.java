/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cloth_v11;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.*;
import java.awt.Color;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

/**
 * @author Jack Reardon
 * @created Jun 30, 2014
 * @edited Jun 30, 2014
 */
public class Simulation {

    // Interaction
    InteractionHandler interactionHandler;

    // Simulation
    SimpleUniverse universe;
    BranchGroup objRoot;
    double[][] cameraPositions; // array of double arrays (each sub array is
    // length 3) defining the camera positions attached to each key '0'-'9'
    Transform3D cameraPosition;
    Point3d currentCameraPosition, currentFocusPosition,
            targetCameraPosition, targetFocusPosition,
            tempPosition;
    int cameraAdjustPositionCount, cameraAdjustRotationCount,
            cameraAdjustCountLimit;
    double cameraAdjustRate;
    boolean willSetCameraPositionShortcut; // For setting shortcut keys
    Vector3d up;

    Cloth c;

    public Simulation(String title) {
        interactionHandler = new InteractionHandler(title, this);

        Initialise();

        LoadCameraPositions();
        // Set camera position to the default one
        SetCamera(1);

        Run();
    }

    // Initialisation called by the applet
    private void Initialise() {
        universe = new SimpleUniverse();
        universe.getViewer().getJFrame(0).setBounds(0, 0, 500, 500);
        objRoot = new BranchGroup();

        // Where all the action takes place
        CreateSceneGraph();

        SetLighting();

        objRoot.compile();

        universe.addBranchGraph(objRoot);
    }

    // Creates the cloth scene graph
    private void CreateSceneGraph() {
        //AddPurturbedCloth();
        AddStandardCloth();
        //AddSmallCloth();
        //AddMediumCloth();

        //AddSphere();
    }

    private void AddStandardCloth() {
        c = new Cloth();
        c.AddToBG(objRoot);
    }

    private void AddPurturbedCloth() {
        c = new Cloth();
        c.CreatePurturbation(2, 4, 0.05f, 0, 0.01f);
        c.AddToBG(objRoot);
    }

    private void AddSmallCloth() {
        Point3f position = new Point3f(-1, 2, -1);
        float internodeStableDistance = 1f;
        c = new Cloth(2, 2, position, internodeStableDistance, 1, 0.01f);
        c.AddToBG(objRoot);
    }

    private void AddMediumCloth() {
        Point3f position = new Point3f(-1, 2, -1);
        float internodeStableDistance = 0.5f;
        c = new Cloth(4, 4, position, internodeStableDistance, 1, 0.01f);
        c.AddToBG(objRoot);
    }

    private void AddSphere() {
        Sphere sphere = new Sphere(1f);
        TransformGroup transformationGroup = new TransformGroup();
        Transform3D transform3D = new Transform3D();

        Vector3d transformVector = new Vector3d(0, 0, 0);
        transform3D.setTranslation(transformVector);
        transformationGroup.setTransform(transform3D);
        transformationGroup.addChild(sphere);

        objRoot.addChild(transformationGroup);
    }

    // Set the lighting
    private void SetLighting() {
        Color3f light1Color = new Color3f(.1f, 1.4f, .1f); // green light
        BoundingSphere bounds
                = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
        Vector3f light1Direction = new Vector3f(4.0f, -7.0f, -12.0f);
        DirectionalLight light1
                = new DirectionalLight(light1Color, light1Direction);
        light1.setInfluencingBounds(bounds);
        objRoot.addChild(light1);

        Background background = new Background(new Color3f(Color.LIGHT_GRAY));
        background.setApplicationBounds(bounds);
        objRoot.addChild(background);
    }

    private void LoadCameraPositions() {
        try {
            cameraPositions = interactionHandler.LoadCameraPositions();
            interactionHandler.SetCameraPositions(cameraPositions);
        } catch (IOException ex) {
            System.out.println("Could not load camera positions");
        }
    }

    private void SetCamera(int position) {
        cameraPosition = new Transform3D();
        targetCameraPosition = new Point3d(cameraPositions[position]);
        currentCameraPosition = new Point3d(targetCameraPosition);
        targetFocusPosition = new Point3d(new double[]{0, 0, 0});
        currentFocusPosition = new Point3d(targetFocusPosition);
        up = new Vector3d(0, 1, 0);

        cameraAdjustCountLimit = 20;
        cameraAdjustRate = 1.0 / (double) cameraAdjustCountLimit;

        cameraPosition.lookAt(currentCameraPosition, currentFocusPosition, up);
        cameraPosition.invert();
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(cameraPosition);
    }

    private void Run() {
        interactionHandler.Run();
    }

    public void Update() {
        c.Update();
    }

    public void UpdateVisual() {
        c.UpdateVisual();

        if (cameraAdjustPositionCount > 0) {
            AdjustCurrentCameraPosition();
        }

        if (cameraAdjustRotationCount > 0) {
            AdjustCurrentCameraRotation();
        }
    }

    public void RevertCameraPosition(int cameraPositionIndex) {
        if (willSetCameraPositionShortcut) {
            // Update the camera position shortcut
            cameraPositions[cameraPositionIndex]
                    = new double[]{currentCameraPosition.x,
                        currentCameraPosition.y, currentCameraPosition.z};
            
            try {
                interactionHandler.SetCameraPositions(cameraPositions);
            } catch (IOException ex) {
                Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            willSetCameraPositionShortcut = false;
        } else {
            SetCamera(cameraPositionIndex);
        }
    }

    public void AdjustCameraPosition(char direction) {
        switch (direction) {
            case 'u':
                targetCameraPosition.y += interactionHandler.GetSensitivity();
                cameraAdjustPositionCount = cameraAdjustCountLimit;
                break;
            case 'd':
                targetCameraPosition.y -= interactionHandler.GetSensitivity();
                cameraAdjustPositionCount = cameraAdjustCountLimit;
                break;
            case 'l':
                targetCameraPosition.x -= interactionHandler.GetSensitivity();
                cameraAdjustPositionCount = cameraAdjustCountLimit;
                break;
            case 'r':
                targetCameraPosition.x += interactionHandler.GetSensitivity();
                cameraAdjustPositionCount = cameraAdjustCountLimit;
                break;
            case 'f':
                tempPosition = new Point3d(targetFocusPosition);
                tempPosition.sub(targetCameraPosition);
                tempPosition.scale(interactionHandler.GetSensitivity());
                targetCameraPosition.add(tempPosition);

                cameraAdjustPositionCount = cameraAdjustCountLimit;
                break;
            case 'b':
                tempPosition = new Point3d(targetFocusPosition);
                tempPosition.sub(targetCameraPosition);
                tempPosition.scale(interactionHandler.GetSensitivity());
                targetCameraPosition.sub(tempPosition);

                cameraAdjustPositionCount = cameraAdjustCountLimit;
                break;
            case '0':
                SetCamera(0);
                break;
            default:
                // Error
                break;
        }
        AdjustCurrentCameraPosition();
        willSetCameraPositionShortcut = false;
    }

    public void WillSetCameraPositionShortcut() {
        willSetCameraPositionShortcut = true;
        Arit.NL("ASFDasdfasdf");
    }

    // Smoothly moves the camera towards the target position
    private void AdjustCurrentCameraPosition() {
        if (cameraAdjustPositionCount > 0) {
            tempPosition = new Point3d(currentCameraPosition);
            tempPosition.sub(targetCameraPosition, currentCameraPosition);
            tempPosition.scale(cameraAdjustRate);
            currentCameraPosition.add(tempPosition);

            cameraPosition.lookAt(currentCameraPosition, currentFocusPosition, up);
            cameraPosition.invert();
            universe.getViewingPlatform().getViewPlatformTransform().setTransform(cameraPosition);
            cameraAdjustPositionCount--;
        }
    }

    public void AdjustCameraRotation(char direction) {
        switch (direction) {
            case 'l':
                tempPosition = new Point3d(currentFocusPosition);
                tempPosition.sub(currentCameraPosition);
                tempPosition = Arit.RotatePointAboutY(tempPosition, interactionHandler.GetSensitivity());
                targetFocusPosition.add(targetCameraPosition, tempPosition);

                cameraAdjustRotationCount = cameraAdjustCountLimit;
                break;
            case 'r':
                tempPosition = new Point3d(currentFocusPosition);
                tempPosition.sub(currentCameraPosition);
                tempPosition = Arit.RotatePointAboutY(tempPosition, -interactionHandler.GetSensitivity());
                targetFocusPosition.add(targetCameraPosition, tempPosition);

                cameraAdjustRotationCount = cameraAdjustCountLimit;
                break;
            default:
                // Error
                break;
        }
        AdjustCurrentCameraRotation();
    }

    // Smoothly moves the camera towards the target position
    private void AdjustCurrentCameraRotation() {
        if (cameraAdjustRotationCount > 0) {
            tempPosition = new Point3d(currentFocusPosition);
            tempPosition.sub(targetFocusPosition);
            tempPosition.scale(cameraAdjustRate);
            currentFocusPosition.add(tempPosition);

            cameraPosition.lookAt(currentCameraPosition, currentFocusPosition, up);
            cameraPosition.invert();
            universe.getViewingPlatform().getViewPlatformTransform().setTransform(cameraPosition);
            cameraAdjustRotationCount--;
        }
    }

}
