/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cloth_v11;

import java.util.ArrayList;
import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TriangleStripArray;
import javax.vecmath.Point3f;

/**
 *
 * @author Jack Reardon
 * @created Jun 30, 2014
 * @edited Jun 30, 2014
 */
public class Cloth {

    // Physicality
    private float[] nodes, // Positions
            nodesAsTriangleArray; // For graphics display, nodes arranged triangularly

    /* Triangle numbering
     n--n--n--n-..
     |0/| /| /| ..
     |/1|/ |/ |/..
     n--n--n--n-..
     |2/| /| /| ..
     */
    private float[] trianglesXMinBound, // The minimum x bound for each triangle
            trianglesYMinBound, // The minimum y bound for each triangle
            trianglesZMinBound, // The minimum z bound for each triangle
            trianglesXMinBoundSorted, // Sorted versions of the above
            trianglesYMinBoundSorted,
            trianglesZMinBoundSorted,
            trianglesXMaxBound, // The maximum x bound for each triangle
            trianglesYMaxBound, // The maximum y bound for each triangle
            trianglesZMaxBound; // The maximum z bound for each triangle
    private int[] trianglesXMinIndexOrder, // The array of indices relating
            // to 'trianglesXMinBound' specifying their order.
            trianglesYMinIndexOrder,
            trianglesZMinIndexOrder,
            trianglesXMinIndexPosition, // The array of indices mapping triangle indices to indices in trianglesXMinIndexOrder
            trianglesYMinIndexPosition,
            trianglesZMinIndexPosition;
            
    /* It works like this:
     * Triangle Index:      0  1  2  3  4  5  6  ...  (i)
     * XMin (unsorted):     5  2  3  8  9  1  4  ...  (trianglesXminBound)
     * -------------------------------------------------------------------------
     * XMin Index Order:    5  1  2  6  0  3  4  ...  (trianglesXMinIndexOrder)
     * Xmin (sorted):       1  2  3  4  5  8  9  ...  (trianglesXminBoundSorted)
     * -------------------------------------------------------------------------
     * XMin Index Position: 4  1  2  5  6  0  3  ...  (trianglesXMinIndexPosition)
     */
    private float[] forces,
            accelerations,
            velocities;
    private float[] colours;
    private int nWidth, // # nodes across - 1
            nDepth, // # nodes deep - 1
            nNodes, // Number of nodes
            nData, // 3 * nNodes (nodes coordinates with xyz split)
            nTriangles; // Number of triangles
    private Point3f position, // Position of first node in the cloth
            tempPosition; // Helper variable

    // Cloth Properties
    private float clothMass, nodeMass; // All nodes have equal mass in this version
    private float internodeStableDistance; // Measurable size between adjacent cloth nodes
    private float shearStableDistance;
    private float dt, internodeSpringConstant, shearSpringConstant,
            bendingSpringConstant;
    private ForceObject force;

    private float initial1X, initial1Y, initial1Z;
    private float initial2X, initial2Y, initial2Z;
    private float initial3X, initial3Y, initial3Z;
    private float initial4X, initial4Y, initial4Z;

    // External Properties
    private float forceDueToGravity;

    Shape3D shape;

    public Cloth() {
        AssignDefaultAttributes();

        InitialiseNodes();
    }

    public Cloth(int nWidth, int nDepth, Point3f position,
            float internodeStableDistance, float clothMass, float dt) {
        this.nWidth = nWidth;
        this.nDepth = nDepth;
        nNodes = (nWidth + 1) * (nDepth + 1);
        nTriangles = (nDepth * 2) * nWidth;

        this.position = position;
        this.internodeStableDistance = internodeStableDistance;

        internodeSpringConstant = 1000f;
        shearSpringConstant = 20f;
        bendingSpringConstant = 5f;
        SetMass(clothMass); // 1kg
        this.dt = dt;

        forceDueToGravity = -0.8f;

        InitialiseNodes();
    }

    private void AssignDefaultAttributes() {
        nWidth = 20;
        nDepth = 20;
        nNodes = (nWidth + 1) * (nDepth + 1);
        nTriangles = (nDepth * 2) * nWidth;

        position = new Point3f(-1f, 0.75f, -1f);
        internodeStableDistance = 0.067f;

        internodeSpringConstant = 1000f;
        shearSpringConstant = 20f;
        bendingSpringConstant = 5f;
        SetMass(0.1f); // 1kg
        dt = 0.01f;

        forceDueToGravity = -0.8f;
    }

    private void SetMass(float clothMass) {
        this.clothMass = clothMass;
        nodeMass = clothMass / nNodes;
    }

    // Creates and positions nodes of the cloth
    private void InitialiseNodes() {
        nNodes = (nWidth + 1) * (nDepth + 1);
        nData = 3 * nNodes;
        nodes = new float[nData];
        forces = ZeroFloatArray(nData);
        accelerations = ZeroFloatArray(nData);
        velocities = ZeroFloatArray(nData);

        CalculateShearStableDistance();

        force = new ForceObject();

        for (int x = 0; x < nWidth + 1; x++) {
            for (int z = 0; z < nDepth + 1; z++) {
                SetNode(nodes, x, z,
                        position.x + internodeStableDistance * x,
                        position.y,
                        position.z + internodeStableDistance * z);
            }
        }

        initial1X = nodes[0];
        initial1Y = nodes[1];
        initial1Z = nodes[2];

        initial2X = nodes[GetNode(nWidth, nDepth)];
        initial2Y = nodes[GetNode(nWidth, nDepth) + 1];
        initial2Z = nodes[GetNode(nWidth, nDepth) + 2];

        initial3X = nodes[GetNode(nWidth, 0)];
        initial3Y = nodes[GetNode(nWidth, 0) + 1];
        initial3Z = nodes[GetNode(nWidth, 0) + 2];

        initial4X = nodes[GetNode(0, nDepth)];
        initial4Y = nodes[GetNode(0, nDepth) + 1];
        initial4Z = nodes[GetNode(0, nDepth) + 2];

        SetNodesAsTriangleArray();

        colours = new float[nodesAsTriangleArray.length];
        
        InitialiseBoundingBoxes();
    }
    
    private void InitialiseBoundingBoxes() {
        PrepareBoundingBoxes();
        DetermineBoundingBoxes();
        OrderInAllDirections();
    }

    // Shifts the position of node x-z by the given amount
    public void CreatePurturbation(int x, int z,
            float xDistortion, float yDistortion, float zDistortion) {
        SetNode(nodes, x, z,
                nodes[GetNode(x, z)] + xDistortion,
                nodes[GetNode(x, z) + 1] + yDistortion,
                nodes[GetNode(x, z) + 2] + zDistortion);
    }

    // This is the hypotenuse of the right-angled triangle with base and height
    // both equal to internodeStableDistance
    private void CalculateShearStableDistance() {
        shearStableDistance = (float) Math.sqrt(2) * internodeStableDistance;
    }

    public void Update() {
        GeometryArray geometryArray = (GeometryArray) shape.getGeometry();
        geometryArray.updateData(new GeometryUpdater() {
            @Override
            public void updateData(Geometry geometry) {
                ResolveForces();

                UpdateVelocities();

                UpdateNodes();
            }
        });
    }

    private void ResolveForces() {
        ApplyInternodeForces();

        ApplyDampingForces();
    }

    private void ApplyInternodeForces() {
        FillWithThreeValues(forces, 0, forceDueToGravity, 0);

        // Top and bottom row (excluding corners)
        for (int x = 1; x < nWidth; x++) {
            // Internode Forces
            // Set forces between x-0 and (x+1)-0 nodes (right and left)
            SetStandardForce(x, 0, x + 1, 0,
                    internodeSpringConstant, internodeStableDistance);

            // Set forces between x-0 and x-1 nodes (up and down)
            SetStandardForce(x, 0, x, 1,
                    internodeSpringConstant, internodeStableDistance);

            // Set forces between x-nDepth and (x+1)-nDepth nodes (right and left)
            SetStandardForce(x, nDepth, x + 1, nDepth,
                    internodeSpringConstant, internodeStableDistance);

            // Shear Forces
            // Upper-Left to Bottom-Right
            SetStandardForce(x, 0, x + 1, 1,
                    shearSpringConstant, shearStableDistance);

            // Upper-Right to Bottom-Left
            SetStandardForce(x, 0, x - 1, 1,
                    shearSpringConstant, shearStableDistance);

            // Bending Forces
            SetBendingForcesAcross(x, 0);
            SetBendingForcesAcross(x, nDepth);

            SetDrag(x, 0);
            SetDrag(x, nDepth);
        }

        // Left and right columns (excluding corners)
        for (int z = 1; z < nDepth; z++) {
            // Set forces between 0-z and 1-z nodes (right and left)
            SetStandardForce(0, z, 1, z,
                    internodeSpringConstant, internodeStableDistance);

            // Set forces between 0-z and 0-(z+1) nodes (up and down)
            SetStandardForce(0, z, 0, z + 1,
                    internodeSpringConstant, internodeStableDistance);

            // Set forces between nWidth-z and nWidth-(z+1) nodes (up and down)
            SetStandardForce(nWidth, z, nWidth, z + 1,
                    internodeSpringConstant, internodeStableDistance);

            // Shear Forces
            // Upper-Left to Bottom-Right
            SetStandardForce(0, z, 1, z + 1,
                    shearSpringConstant, shearStableDistance);

            // Upper-Right to Bottom-Left
            SetStandardForce(nWidth, z, nWidth - 1, z + 1,
                    shearSpringConstant, shearStableDistance);

            // Bending Forces
            SetBendingForcesDown(0, z);
            SetBendingForcesDown(nWidth, z);

            SetDrag(0, z);
            SetDrag(nWidth, z);
        }

        // Middle area
        for (int x = 1; x < nWidth; x++) {
            for (int z = 1; z < nDepth; z++) {
                // Set forces between x-z and (x+1)-z nodes (right and left)
                SetStandardForce(x, z, x + 1, z,
                        internodeSpringConstant, internodeStableDistance);

                // Set forces between x-z and x-(z+1) nodes (up and down)
                SetStandardForce(x, z, x, z + 1,
                        internodeSpringConstant, internodeStableDistance);

                // Shear Forces
                // Upper-Left to Bottom-Right
                SetStandardForce(x, z, x + 1, z + 1,
                        shearSpringConstant, shearStableDistance);

                // Upper-Right to Bottom-Left
                SetStandardForce(x, z, x - 1, z + 1,
                        shearSpringConstant, shearStableDistance);

                // Bending forces
                SetBendingForcesAcross(x, z);

                SetBendingForcesDown(x, z);

                SetDrag(x, z);
            }
        }

        // Corners
        // 0-0
        SetStandardForce(0, 0, 1, 0,
                internodeSpringConstant, internodeStableDistance);
        SetStandardForce(0, 0, 0, 1,
                internodeSpringConstant, internodeStableDistance);
        SetStandardForce(0, 0, 1, 1,
                shearSpringConstant, shearStableDistance);

        // nWidth-0
        SetStandardForce(nWidth, 0, nWidth, 1,
                internodeSpringConstant, internodeStableDistance);
        SetStandardForce(nWidth, 0, nWidth - 1, 1,
                shearSpringConstant, shearStableDistance);

        // 0-nDepth
        SetStandardForce(0, nDepth, 1, nDepth,
                internodeSpringConstant, internodeStableDistance);

        // nWidth-nDepth
        // All relevant forces for this node already calculated
    }

    private void ApplyDampingForces() {
        // TODO
    }

    // Sets the force to the force matrix at node x-z due to acting node
    // with the given stable distance and stores result in the force object
    private void SetStandardForce(int x, int z, int xAct, int zAct,
            float springConstant, float stableDistance) {
        force.nodeThisIndex = GetNode(x, z);
        force.nodeThatIndex = GetNode(xAct, zAct);
        force.x = nodes[force.nodeThisIndex] - nodes[force.nodeThatIndex];
        force.y = nodes[force.nodeThisIndex + 1] - nodes[force.nodeThatIndex + 1];
        force.z = nodes[force.nodeThisIndex + 2] - nodes[force.nodeThatIndex + 2];
        float distance = Arit.GetLength(force.x, force.y, force.z); // Euclidean distance
        float forceMagnitude = springConstant
                * (distance - stableDistance) / 0.2f;

        force.scale = -forceMagnitude / distance;

        force.AssignForces(forces);
    }

    // Sets bending forces across the mesh (assuming nodes exist to the left
    // and right of the given node
    private void SetBendingForcesAcross(int x, int z) {
        int nodeThisIndex = GetNode(x, z);
        int nodeThatLeftIndex = GetNode(x - 1, z);
        int nodeThatRightIndex = GetNode(x + 1, z);

        float displacementLeftX = nodes[nodeThatLeftIndex] - nodes[nodeThisIndex];
        float displacementLeftY = nodes[nodeThatLeftIndex + 1] - nodes[nodeThisIndex + 1];
        float displacementLeftZ = nodes[nodeThatLeftIndex + 2] - nodes[nodeThisIndex + 2];
        float displacementLeftLength = Arit.GetLength(
                displacementLeftX, displacementLeftY, displacementLeftZ);

        float displacementRightX = nodes[nodeThatRightIndex] - nodes[nodeThisIndex];
        float displacementRightY = nodes[nodeThatRightIndex + 1] - nodes[nodeThisIndex + 1];
        float displacementRightZ = nodes[nodeThatRightIndex + 2] - nodes[nodeThisIndex + 2];
        float displacementRightLength = Arit.GetLength(
                displacementRightX, displacementRightY, displacementRightZ);

        float angle = Arit.GetAngle(
                displacementLeftX, displacementLeftY, displacementLeftZ,
                displacementRightX, displacementRightY, displacementRightZ);
        float bendingForceMagnitude = bendingSpringConstant
                * ((float) Math.PI - angle);

        float averageDisplacementX
                = displacementLeftX / displacementLeftLength
                + displacementRightX / displacementRightLength;
        float averageDisplacementY
                = displacementLeftY / displacementLeftLength
                + displacementRightY / displacementRightLength;
        float averageDisplacementZ
                = displacementLeftZ / displacementLeftLength
                + displacementRightZ / displacementRightLength;

        float actingBendingForceX
                = -averageDisplacementX * bendingForceMagnitude / 2f;
        float actingBendingForceY
                = -averageDisplacementY * bendingForceMagnitude / 2f;
        float actingBendingForceZ
                = -averageDisplacementZ * bendingForceMagnitude / 2f;

        forces[nodeThatLeftIndex] += actingBendingForceX;
        forces[nodeThatLeftIndex + 1] += actingBendingForceY;
        forces[nodeThatLeftIndex + 2] += actingBendingForceZ;

        forces[nodeThatRightIndex] += actingBendingForceX;
        forces[nodeThatRightIndex + 1] += actingBendingForceY;
        forces[nodeThatRightIndex + 2] += actingBendingForceZ;
    }

    // Sets bending forces up and down the mesh (assuming nodes exist above
    // and below the given node
    private void SetBendingForcesDown(int x, int z) {
        int nodeThisIndex = GetNode(x, z);
        int nodeThatUpIndex = GetNode(x, z - 1);
        int nodeThatDownIndex = GetNode(x, z + 1);

        float displacementUpX = nodes[nodeThatUpIndex] - nodes[nodeThisIndex];
        float displacementUpY = nodes[nodeThatUpIndex + 1] - nodes[nodeThisIndex + 1];
        float displacementUpZ = nodes[nodeThatUpIndex + 2] - nodes[nodeThisIndex + 2];
        float displacementUpLength = Arit.GetLength(
                displacementUpX, displacementUpY, displacementUpZ);

        float displacementDownX = nodes[nodeThatDownIndex] - nodes[nodeThisIndex];
        float displacementDownY = nodes[nodeThatDownIndex + 1] - nodes[nodeThisIndex + 1];
        float displacementDownZ = nodes[nodeThatDownIndex + 2] - nodes[nodeThisIndex + 2];
        float displacementDownLength = Arit.GetLength(
                displacementDownX, displacementDownY, displacementDownZ);

        float angle = Arit.GetAngle(
                displacementUpX, displacementUpY, displacementUpZ,
                displacementDownX, displacementDownY, displacementDownZ);
        float bendingForceMagnitude = bendingSpringConstant
                * ((float) Math.PI - angle);

        float averageDisplacementX
                = displacementUpX / displacementUpLength
                + displacementDownX / displacementDownLength;
        float averageDisplacementY
                = displacementUpY / displacementUpLength
                + displacementDownY / displacementDownLength;
        float averageDisplacementZ
                = displacementUpZ / displacementUpLength
                + displacementDownZ / displacementDownLength;

        float actingBendingForceX
                = -averageDisplacementX * bendingForceMagnitude / 2f;
        float actingBendingForceY
                = -averageDisplacementY * bendingForceMagnitude / 2f;
        float actingBendingForceZ
                = -averageDisplacementZ * bendingForceMagnitude / 2f;

        forces[nodeThatUpIndex] += actingBendingForceX;
        forces[nodeThatUpIndex + 1] += actingBendingForceY;
        forces[nodeThatUpIndex + 2] += actingBendingForceZ;

        forces[nodeThatDownIndex] += actingBendingForceX;
        forces[nodeThatDownIndex + 1] += actingBendingForceY;
        forces[nodeThatDownIndex + 2] += actingBendingForceZ;
    }

    private void SetDrag(int x, int z) {
        int nodeThis = GetNode(x, z);
        forces[nodeThis] -= 10 * velocities[nodeThis];
        forces[nodeThis + 1] -= 10 * velocities[nodeThis + 1];
        forces[nodeThis + 2] -= 10 * velocities[nodeThis + 2];
    }

    private void UpdateVelocities() {
        for (int x = 0; x < nWidth + 1; x++) {
            for (int z = 0; z < nDepth + 1; z++) {
                velocities[GetNode(x, z)] += forces[GetNode(x, z)] * dt;
                velocities[GetNode(x, z) + 1] += forces[GetNode(x, z) + 1] * dt;
                velocities[GetNode(x, z) + 2] += forces[GetNode(x, z) + 2] * dt;
            }
        }
    }

    // Position update
    private void UpdateNodes() {
        for (int x = 0; x < nWidth + 1; x++) {
            for (int z = 0; z < nDepth + 1; z++) {
                nodes[GetNode(x, z)] += velocities[GetNode(x, z)] * dt;
                nodes[GetNode(x, z) + 1] += velocities[GetNode(x, z) + 1] * dt;
                nodes[GetNode(x, z) + 2] += velocities[GetNode(x, z) + 2] * dt;

                /*UpdateNodesAsTriangleArray(x, z,
                 nodes[GetNode(x, z)],
                 nodes[GetNode(x, z) + 1],
                 nodes[GetNode(x, z) + 2]);*/
            }
        }

        FixCorners();
        
        ProcessBoundingBoxes();

        //CollisionDetect();
    }

    public void UpdateVisual() {
        for (int x = 0; x < nWidth + 1; x++) {
            for (int z = 0; z < nDepth + 1; z++) {
                int nodeIndex = GetNode(x, z);
                
                UpdateNodesAsTriangleArray(x, z,
                        nodes[nodeIndex],
                        nodes[nodeIndex + 1],
                        nodes[nodeIndex + 2]);
                
                if (x < nWidth) {
                    colours[2 * nodeIndex] = 1;
                    colours[2 * nodeIndex + 1] = 1;
                    colours[2 * nodeIndex + 2] = 1;
                }
                
                if (x > 0) {
                    colours[2 * nodeIndex - 3 * (2 * nDepth + 1)] = 1;
                    colours[2 * nodeIndex - 3 * (2 * nDepth + 1) + 1] = 1;
                    colours[2 * nodeIndex - 3 * (2 * nDepth + 1) + 2] = 1;
                }
            }
        }
    }

    private void FixCorners() {
        /*nodes[0] = initial1X;
        nodes[1] = initial1Y;
        nodes[2] = initial1Z;*/

        nodes[GetNode(nWidth, nDepth)] = initial2X;
        nodes[GetNode(nWidth, nDepth) + 1] = initial2Y;
        nodes[GetNode(nWidth, nDepth) + 2] = initial2Z;
        
         nodes[GetNode(nWidth, 0)] = initial3X;
         nodes[GetNode(nWidth, 0) + 1] = initial3Y;
         nodes[GetNode(nWidth, 0) + 2] = initial3Z;

        nodes[GetNode(0, nDepth)] = initial4X;
        nodes[GetNode(0, nDepth) + 1] = initial4Y;
        nodes[GetNode(0, nDepth) + 2] = initial4Z;

        /*UpdateNodesAsTriangleArray(0, 0,
         nodes[GetNode(0, 0)],
         nodes[GetNode(0, 0) + 1],
         nodes[GetNode(0, 0) + 2]);
        
         UpdateNodesAsTriangleArray(nWidth, nDepth,
         nodes[GetNode(nWidth, nDepth)],
         nodes[GetNode(nWidth, nDepth) + 1],
         nodes[GetNode(nWidth, nDepth) + 2]);
        
         UpdateNodesAsTriangleArray(nWidth, 0,
         nodes[GetNode(nWidth, 0)],
         nodes[GetNode(nWidth, 0) + 1],
         nodes[GetNode(nWidth, 0) + 2]);
        
         UpdateNodesAsTriangleArray(0, nDepth,
         nodes[GetNode(0, nDepth)],
         nodes[GetNode(0, nDepth) + 1],
         nodes[GetNode(0, nDepth) + 2]);*/
    }

    private void CollisionDetect() {
        for (int x = 0; x < nWidth + 1; x++) {
            for (int z = 0; z < nDepth + 1; z++) {
                if (Arit.GetLength(nodes[GetNode(x, z)],
                        nodes[GetNode(x, z) + 1],
                        nodes[GetNode(x, z) + 2]) < 1) {
                    // Revert position outside of the sphere
                    MoveNodeFromPoint(GetNode(x, z), 1.01f, 0, 0, 0);
                }
                if (nodes[GetNode(x, z) + 1] < 0) {
                    nodes[GetNode(x, z) + 1] = 0;
                }
            }
        }
    }

    // Sets the nodes in the triangle array to the updated node at x-z
    private void UpdateNodesAsTriangleArray(int x, int z,
            float xVal, float yVal, float zVal) {
        // The node at x-z is the (x * (nDepth + 1) + z)th node
        // This will appear in the triangle array in the
        // 2 * (x * (nDepth + 1) + z)th, and ((x - 1) * (nDepth + 1) + z + 1)th
        // positions (in 3-coordinates)
        int nodeIndex = GetNode(x, z);
        if (x < nWidth) {
            nodesAsTriangleArray[2 * nodeIndex] = xVal;
            nodesAsTriangleArray[2 * nodeIndex + 1] = yVal;
            nodesAsTriangleArray[2 * nodeIndex + 2] = zVal;
        }

        if (x > 0) {
            nodesAsTriangleArray[2 * nodeIndex - 3 * (2 * nDepth + 1)] = xVal;
            nodesAsTriangleArray[2 * nodeIndex - 3 * (2 * nDepth + 1) + 1] = yVal;
            nodesAsTriangleArray[2 * nodeIndex - 3 * (2 * nDepth + 1) + 2] = zVal;
        }
    }

    private void SetNodesAsTriangleArray() {
        // A strip is the set of nodes forming a triangle strip from two columns of nodes deep
        // # strips on one side = nWidth
        // # nodes per strip = 2 * (nDepth + 1) (since nDepth is number of across lines - 1)
        // # triangle points = # strips * # nodes per strips
        // # indeces = triangle points * 3
        int numberOfTriangleIndices = nWidth * (2 * (nDepth + 1)) * 3;
        nodesAsTriangleArray = new float[numberOfTriangleIndices];
        int count = 0;
        for (int x = 0; x < nWidth; x++) {
            for (int z = 0; z < nDepth + 1; z++) {
                for (int acrossWidth = 0; acrossWidth < 2; acrossWidth++) {
                    nodesAsTriangleArray[count]
                            = nodes[GetNode(x + acrossWidth, z)];
                    nodesAsTriangleArray[count + 1]
                            = nodes[GetNode(x + acrossWidth, z) + 1];
                    nodesAsTriangleArray[count + 2]
                            = nodes[GetNode(x + acrossWidth, z) + 2];

                    count += 3;
                }
            }
        }
    }

    public void AddToBG(BranchGroup bg) {
        for (int count = 0; count < colours.length; count++) {
            colours[count] = 0f;
        }


        int[] stripLengths = new int[nWidth];
        for (int count = 0; count < nWidth; count++) {
            stripLengths[count] = 2 * (nDepth + 1);
        }

        TriangleStripArray strips = new TriangleStripArray(stripLengths[0] * nWidth,
                GeometryArray.COORDINATES | GeometryArray.COLOR_3 | GeometryArray.BY_REFERENCE,
                stripLengths);

        strips.setCoordRefFloat(nodesAsTriangleArray);
        strips.setColorRefFloat(colours);

        strips.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
        strips.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);

        shape = new Shape3D(strips);
        //shape.setAppearance(CreateWireFrameAppearance());
        shape.setAppearance(CreateNormalAppearance());
        shape.setCapabilityIsFrequent(Shape3D.ALLOW_GEOMETRY_READ);
        bg.addChild(shape);
    }

    private Appearance CreateWireFrameAppearance() {
        Appearance materialAppear = new Appearance();
        PolygonAttributes polyAttrib = new PolygonAttributes();
        polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_LINE);
        polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
        materialAppear.setPolygonAttributes(polyAttrib);

        ColoringAttributes redColoring = new ColoringAttributes();
        redColoring.setColor(1.0f, 0.0f, 0.0f);
        materialAppear.setColoringAttributes(redColoring);

        return materialAppear;
    }
    
    private Appearance CreateNormalAppearance() {
        Appearance materialAppear = new Appearance();
        PolygonAttributes polyAttrib = new PolygonAttributes();
        polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
        polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
        materialAppear.setPolygonAttributes(polyAttrib);

        ColoringAttributes redColoring = new ColoringAttributes();
        redColoring.setColor(1.0f, 0.0f, 0.0f);
        materialAppear.setColoringAttributes(redColoring);

        return materialAppear;
    }

    // Helper methods //
    public static float[] ZeroFloatArray(int size) {
        float[] array = new float[size];
        for (int count = 0; count < array.length; count++) {
            array[count] = 0f;
        }
        return array;
    }
    
    public static void NL(Object o) {
        System.out.println(o);
    }
    
    public static void PrintArrayList(ArrayList<Integer> list) {
        for (int i : list) {
            System.out.println(i);
        }
    }
    
    public static void PrintArrayFloat(float[] list) {
        for (float i : list) {
            System.out.println(i);
        }
    }

    // Fills the given array with values x, y, z in groups of three
    public static void FillWithThreeValues(float[] array,
            float x, float y, float z) {
        for (int count = 0; count < array.length; count += 3) {
            array[count] = x;
            array[count + 1] = y;
            array[count + 2] = z;
        }
    }

    // Returns the index of the 'nodes' array of the x-zth node
    // Note that the 'nodes' array contains individual xyz coordinates
    // for each node (so has 3 * nNodes elements)
    // The data of nodes[GetNode(x, z)] is the x coordinate of node x-z
    // ____________nodes[GetNode(x, z)+1] is the y coordinate of node x-z
    // ____________nodes[GetNode(x, z)+2] is the z coordinate of node x-z
    private int GetNode(int x, int z) {
        return 3 * (x * (nDepth + 1) + z);
    }

    // Sets three successive pieces of array data to the given data in sequance
    private void SetNode(float[] data, int x, int y,
            float data0, float data1, float data2) {
        data[GetNode(x, y)] = data0;
        data[GetNode(x, y) + 1] = data1;
        data[GetNode(x, y) + 2] = data2;
    }

    // Given the position (index in 'nodes') of one object, move it so that its
    // distance of separation from another given object is the given amount
    private void MoveNodeFromPoint(int nodeIndex, float newDistance,
            float pointX, float pointY, float pointZ) {
        float distance = Arit.GetLength(nodes[nodeIndex] - pointX,
                nodes[nodeIndex + 1] - pointY, nodes[nodeIndex + 2] - pointZ);
        nodes[nodeIndex] = pointX + newDistance / distance * (nodes[nodeIndex] - pointX);
        nodes[nodeIndex + 1] = pointY + newDistance / distance * (nodes[nodeIndex + 1] - pointY);
        nodes[nodeIndex + 2] = pointZ + newDistance / distance * (nodes[nodeIndex + 2] - pointZ);
    }

    // Bounding box definition
    // Nodes and traingles are indexed thus:
    /*
     00---D+1--
     ||00/|2*D/
     || / || /   1
     ||/01||/2*D+1
     01---D+2--
     ||02/||2*D+2
     || / || /   2
     ||/03||/2*D+3
     02---D+3--
     Where D = nDepth (= 2 in this case
     */
    // The index of the first node in the 'triangleIndex'th triangle
    // is preciesly equal to 'triangleIndex' eg.
    // |/4|/...
    // 5--6--n..
    // |5/| /|..
    // |/6|/ |..
    // 7--8--n..
    // The sixth triangle is made from nodes 6, 7, 8 and six = 6!
    // For all columns other than the first
    
    private void ProcessBoundingBoxes() {
        DetermineBoundingBoxes();
        ReorderInAllDirections();
        DetectCollisions();
    }
    
    
    private void DetermineBoundingBoxes() {
        // The index of the first, second and third triangles in the
        //   considered triangle
        int XIndexOfFirstTriangle,
                XIndexOfSecondTriangle,
                XIndexOfThirdTriangle;
        for (int i = 0; i < nTriangles; i++) {
            // Consider triangles minimums in the order that they appear in 
            //   their corresponding index order array
            int triangleIndex = trianglesXMinIndexOrder[i];
            XIndexOfFirstTriangle = GetIndexOfFirstNodeInTriangle(triangleIndex);
            XIndexOfSecondTriangle = GetIndexOfSecondNodeInTriangle(triangleIndex);
            XIndexOfThirdTriangle = GetIndexOfThirdNodeInTriangle(triangleIndex);
            trianglesXMinBoundSorted[i]
                    = MinOfThree(nodes[XIndexOfFirstTriangle],
                            nodes[XIndexOfSecondTriangle],
                            nodes[XIndexOfThirdTriangle]);
            trianglesXMinBound[triangleIndex] = trianglesXMinBoundSorted[i];
            trianglesXMinIndexPosition[triangleIndex] = i;
            
            triangleIndex = trianglesYMinIndexOrder[i];
            XIndexOfFirstTriangle = GetIndexOfFirstNodeInTriangle(triangleIndex);
            XIndexOfSecondTriangle = GetIndexOfSecondNodeInTriangle(triangleIndex);
            XIndexOfThirdTriangle = GetIndexOfThirdNodeInTriangle(triangleIndex);
            trianglesYMinBoundSorted[i]
                    = MinOfThree(nodes[XIndexOfFirstTriangle + 1],
                            nodes[XIndexOfSecondTriangle + 1],
                            nodes[XIndexOfThirdTriangle + 1]);
            trianglesYMinBound[triangleIndex] = trianglesYMinBoundSorted[i];
            trianglesYMinIndexPosition[triangleIndex] = i;
            
            triangleIndex = trianglesZMinIndexOrder[i];
            XIndexOfFirstTriangle = GetIndexOfFirstNodeInTriangle(triangleIndex);
            XIndexOfSecondTriangle = GetIndexOfSecondNodeInTriangle(triangleIndex);
            XIndexOfThirdTriangle = GetIndexOfThirdNodeInTriangle(triangleIndex);
            trianglesZMinBoundSorted[i]
                    = MinOfThree(nodes[XIndexOfFirstTriangle + 2],
                            nodes[XIndexOfSecondTriangle + 2],
                            nodes[XIndexOfThirdTriangle + 2]);
            trianglesZMinBound[triangleIndex] = trianglesZMinBoundSorted[i];
            trianglesZMinIndexPosition[triangleIndex] = i;
            
            // For max bounds, triangles are never ordered so their indix ordering
            //   is an ascending array, coincidentally corresponding with the
            //   incremental increasing local variable, 'i'.
            triangleIndex = i;
            XIndexOfFirstTriangle = GetIndexOfFirstNodeInTriangle(triangleIndex);
            XIndexOfSecondTriangle = GetIndexOfSecondNodeInTriangle(triangleIndex);
            XIndexOfThirdTriangle = GetIndexOfThirdNodeInTriangle(triangleIndex);
            trianglesXMaxBound[i]
                    = MaxOfThree(nodes[XIndexOfFirstTriangle],
                            nodes[XIndexOfSecondTriangle],
                            nodes[XIndexOfThirdTriangle]);
            trianglesYMaxBound[i]
                    = MaxOfThree(nodes[XIndexOfFirstTriangle + 1],
                            nodes[XIndexOfSecondTriangle + 1],
                            nodes[XIndexOfThirdTriangle + 1]);
            trianglesZMaxBound[i]
                    = MaxOfThree(nodes[XIndexOfFirstTriangle + 2],
                            nodes[XIndexOfSecondTriangle + 2],
                            nodes[XIndexOfThirdTriangle + 2]);
        }
    }

    // For the 'triangleIndex'th triangle, returns the index in 'nodes' of the
    // first node, 'A', describing the triangle eg.
    // The 6'th triangle has vertices made by nodes 6, 7 and 8, so the first
    // node is 6 (which is what this function returns (multiplied by 3, since
    // 'nodes' is an array of coordinates))
    private int GetIndexOfFirstNodeInTriangle(int triangleIndex) {
        return 3 * (triangleIndex / 2 + triangleIndex / (2 * nDepth)
                + (triangleIndex % 2) * (nDepth + 1));
    }

    private int GetIndexOfSecondNodeInTriangle(int triangleIndex) {
        if (triangleIndex % (2 * nDepth) == 0) {
            return GetIndexOfFirstNodeInTriangle(triangleIndex + 1);
        } else {
            return GetIndexOfThirdNodeInTriangle(triangleIndex - 1);
        }
    }

    private int GetIndexOfThirdNodeInTriangle(int triangleIndex) {
        return GetIndexOfFirstNodeInTriangle(triangleIndex) + 3 * 1;
    }

    private void PrepareBoundingBoxes() {
        trianglesXMinBound = new float[nTriangles];
        trianglesYMinBound = new float[nTriangles];
        trianglesZMinBound = new float[nTriangles];
        trianglesXMinBoundSorted = new float[nTriangles];
        trianglesYMinBoundSorted = new float[nTriangles];
        trianglesZMinBoundSorted = new float[nTriangles];
        trianglesXMaxBound = new float[nTriangles];
        trianglesYMaxBound = new float[nTriangles];
        trianglesZMaxBound = new float[nTriangles];

        trianglesXMinIndexOrder = AscendingArray(nTriangles);
        trianglesYMinIndexOrder = AscendingArray(nTriangles);
        trianglesZMinIndexOrder = AscendingArray(nTriangles);
        trianglesXMinIndexPosition = AscendingArray(nTriangles);
        trianglesYMinIndexPosition = AscendingArray(nTriangles);
        trianglesZMinIndexPosition = AscendingArray(nTriangles);
    }

    // Returns an array with elements from 1...n inclusive of either end
    private int[] AscendingArray(int n) {
        int[] array = new int[n];
        for (int i = 0; i < n; i++) {
            array[i] = i;
        }
        return array;
    }

    // Returns the minimum value of a set of three numbers
    private float MinOfThree(float a, float b, float c) {
        return Math.min(Math.min(a, b), c);
    }

    // Returns the maximum value of a set of three numbers
    private float MaxOfThree(float a, float b, float c) {
        return Math.max(Math.max(a, b), c);
    }

    // Orders indices (held in 'trianglesXMinIndexOrder',
    //   'trianglesYMinIndexOrder' etc) according to the ordering of their bound
    //   counterparts (held in 'trianglesXMinBound', 'trianglesXMinBound' etc)
    private void OrderInAllDirections() {
        QuickSortByKeys(trianglesXMinIndexOrder,
                trianglesXMinBoundSorted,
                trianglesXMinIndexPosition);
        QuickSortByKeys(trianglesYMinIndexOrder,
                trianglesYMinBoundSorted,
                trianglesYMinIndexPosition);
        QuickSortByKeys(trianglesZMinIndexOrder,
                trianglesZMinBoundSorted,
                trianglesZMinIndexPosition);
    }
    
    private void CopyToArray(float[] arrayFrom, float[] arrayTo) {
        for (int i = 0; i < arrayFrom.length; i++) {
            arrayTo[i] = arrayFrom[i];
        }
    }

    // Checks that everything is in order and instertion sorts if not
    private void ReorderInAllDirections() {
        InsertionSortByKeys(trianglesXMinIndexOrder,
                trianglesXMinBoundSorted,
                trianglesXMinIndexPosition);
        InsertionSortByKeys(trianglesYMinIndexOrder,
                trianglesYMinBoundSorted,
                trianglesYMinIndexPosition);
        InsertionSortByKeys(trianglesZMinIndexOrder,
                trianglesZMinBoundSorted,
                trianglesZMinIndexPosition);
    }

    // Orders array according to the keys
    // Must have that array.length == keys.length is true
    private void QuickSortByKeys(int[] data, float[] keys, int[] otherOrder) {
        // Quicksort the keys and move array elements around accordingly
        QuickSortByKeys(data, keys, otherOrder, 0, keys.length);
    }

    private void QuickSortByKeys(int[] data, float[] keys, int[] otherOrder,
            int startIndex, int endIndex) {
        if (endIndex - 1 <= startIndex) {
            // Do nothing more
            return;
        }

        // Else
        int pivotIndex = (int) ((endIndex - startIndex) * Math.random())
                + startIndex;
        float pivotValue = keys[pivotIndex];

        int greaterThan = startIndex + (pivotIndex == startIndex ? 1 : 0);

        float temp;
        int temp2, temp3;
        for (int scanner = startIndex; scanner < endIndex; scanner++) {
            if (scanner != pivotIndex) {
                temp = keys[scanner];
                if (temp <= pivotValue) {
                    // Swap the value at scanner and the value at greaterThan
                    temp2 = data[scanner];
                    temp3 = otherOrder[temp2];
                    
                    keys[scanner] = keys[greaterThan];
                    data[scanner] = data[greaterThan];
                    otherOrder[temp2] = otherOrder[data[greaterThan]];
                    
                    keys[greaterThan] = temp;
                    otherOrder[data[greaterThan]] = temp3;
                    data[greaterThan] = temp2;
                    

                    // Progress greaterThan
                    greaterThan++;
                    if (greaterThan == pivotIndex) {
                        greaterThan++;
                    }
                }
            }
        }

        // Determine the index that the pivot should move to
        int movePivotToIndex = greaterThan;
        if (greaterThan > pivotIndex) {
            movePivotToIndex--;
        }

        // Move the pivot there
        temp = keys[movePivotToIndex];
        temp2 = data[movePivotToIndex];
        temp3 = otherOrder[temp2];
        
        keys[movePivotToIndex] = pivotValue;
        data[movePivotToIndex] = data[pivotIndex];
        otherOrder[temp2] = otherOrder[data[pivotIndex]];
        
        keys[pivotIndex] = temp;
        otherOrder[data[pivotIndex]] = temp3;
        data[pivotIndex] = temp2;

        // Sort the rest
        QuickSortByKeys(data, keys, otherOrder, startIndex, movePivotToIndex);
        QuickSortByKeys(data, keys, otherOrder, movePivotToIndex + 1, endIndex);
    }

    // Insertion sort assumes that the observed element is in order if
    // it is in order relative to adjacent elements
    private void InsertionSortByKeys(int[] data, float[] keys, int[] otherOrder) {
        float temp;
        int temp2, temp3;
        for (int i = 0; i < keys.length - 1; i++) {
            temp = keys[i + 1];
            if (temp < keys[i]) {
                // Insertion sort this element (insert it in the right place)
                int j = i - 1;
                while (j > 0) {
                    if (keys[j] < temp) {
                        // Insert it at position j + 1
                        temp2 = data[i + 1];
                        temp3 = otherOrder[temp2];
                        for (int k = i; k >= j + 1; k--) {
                            keys[k + 1] = keys[k];
                            data[k + 1] = data[k];
                            otherOrder[data[k + 1]] = k + 1;
                        }
                        keys[j + 1] = temp;
                        data[j + 1] = temp2;
                        otherOrder[temp2] = j + 1;

                        // Stop the (inner) while loop
                        j = -1;
                    }
                    j--;
                }
                if (j == 0) {
                    // keys[i + 1] must be the smallest
                    // Insert at position 0
                    temp2 = data[i + 1];
                    temp3 = otherOrder[temp2];
                    for (int k = i; k >= j + 1; k--) {
                        keys[k + 1] = keys[k];
                        data[k + 1] = data[k];
                        otherOrder[data[k + 1]] = k + 1;
                    }
                    keys[j + 1] = temp;
                    data[j + 1] = temp2;
                    otherOrder[temp2] = j + 1;
                }
            } 
            // Otherwise just skip over this element
        }
    }

    private void PrintArray(int[] array) {
        String s = "Array (length " + array.length + "): " + array[0];
        for (int i = 1; i < array.length; i++) {
            s += ", " + array[i];
        }

        System.out.println(s);
    }

    public static void ConfirmSorted(float[] array) {
        float max = array[0];
        for (int count = 1; count < array.length; count++) {
            if (array[count] < max) {
                System.out.println("Unsorted at index " + count);
                return;
            }
            max = array[count];
        }

        System.out.println("Array is sorted");
    }
    
    private void DetectCollisions() {
        // Don't bother checking for collision in the last triangle,
        //   since we always check for collision from i -> nTriangles anyway
        //   (so if a collision involving 'trianglesXMinIndexOrder[last]'
        //   existed, then it would have already been found from checking
        //   previous triangles)
        for (int i = 0; i < nTriangles - 1; i++) {
            int observedTriangleIndex = trianglesXMinIndexOrder[i];
            
            // Get possible collisions for each triangle in the x-direction
            float maxXOfObserved = trianglesXMaxBound[observedTriangleIndex];
            // 'possibleXCollisions' is the set of triangles that 
            //   'observedTriangleIndex' is near in the x-direction
            ArrayList<Integer> possibleXCollisions = new ArrayList<Integer>();
            for (int j = i + 1; j < nTriangles
                    && trianglesXMinBoundSorted[j] < maxXOfObserved; j++) {
                possibleXCollisions.add(trianglesXMinIndexOrder[j]);
            }
            
            // Get possible collisions for each triangle in the x-direction
            float maxYOfObserved = trianglesYMaxBound[observedTriangleIndex];
            // 'possibleYCollisions' is the set of triangles that 
            //   'observedTriangleIndex' is near in the y-direction
            ArrayList<Integer> possibleYCollisions = new ArrayList<Integer>();
            int YIndexOfObservedTriangle = trianglesYMinIndexPosition[observedTriangleIndex];
            for (int j = YIndexOfObservedTriangle + 1; j < nTriangles
                    && trianglesYMinBoundSorted[j] < maxYOfObserved; j++) {
                if (trianglesYMaxBound[trianglesYMinIndexOrder[j]]
                        > trianglesYMinBound[observedTriangleIndex]) {
                    possibleYCollisions.add(trianglesYMinIndexOrder[j]);
                }
            }
            
            // Get possible collisions for each triangle in the x-direction
            float maxZOfObserved = trianglesZMaxBound[observedTriangleIndex];
            // 'possibleZCollisions' is the set of triangles that 
            //   'observedTriangleIndex' is near in the z-direction
            ArrayList<Integer> possibleZCollisions = new ArrayList<Integer>();
            // Check triangles from index 'j = j0' in 'trianglesZMinBound' such
            //   that 'trianglesZMinBound[j0-1]' < 'trianglesZMinBound[observed]'
            //   (random search for it in 'trianglesZMinBound' OR keep a record
            //   of the triangles in ascending index order for ZMin and just get
            //   'trianglesZMinBoundUnsorted[observed]' and then 
            int ZIndexOfObservedTriangle = trianglesXMinIndexPosition[observedTriangleIndex];
            for (int j = ZIndexOfObservedTriangle + 1; j < nTriangles
                    && trianglesZMinBoundSorted[j] < maxZOfObserved; j++) {
                // Add only if max of triangle 'trianglesZMinIndexOrder[j]' is
                //   greater than the minimum z of the observed triangle
                //   ('trianglesZMinBound[SEARCH FOR OBSERVED TRIANGLE]
                //           < trianglesZMaxBound[trianglesZMinIndexOrder[j]]')
                if (trianglesZMaxBound[trianglesZMinIndexOrder[j]]
                        > trianglesZMinBound[observedTriangleIndex]) {
                    possibleZCollisions.add(trianglesZMinIndexOrder[j]);
                }
            }
            
            // This method will call on the robust triangle-triangle
            //   intersection determination mechanism between triangle 'i'
            //   and any triangle that appears in all three lists passed to it
            RobustDetectCollisions(observedTriangleIndex,
                    possibleXCollisions,
                    possibleYCollisions,
                    possibleZCollisions);
        }

        /*
        Analysis:
        n is the number of triangles which is proportional to the number of nodes
        (Sorting (initially) takes O(3 * n log n))
        Sorting (on each update call, assuming relatively consistentordering between updates)
          uses insertion sort and takes O(3 * n) time
        Loop on xmin takes O(c * n) time average case?
        loop on ymin ""
        loop on zmin ""
        
        Test for collisions requires
        approximately O(c) elements in possibleXCollisionsFor for triangle number k
        search within the list inside 'possibleYCollisionsFor triangle k's list (O(log c))
        search within the list inside 'possibleZCollisionsFor triangle k's list (O(log c))
        Repeating this for all $n$ triangles in the 'possibleXCollisionsFor' list
         - This must then take O(n * (c + 2 *log c)) = O(n log c)
        
        THE ENTIRE ALGORITHM:
        O(3 * n) + O(3 * c * n) + O(n log c) = O(n)
        */
    }
    
    // Calls the robust triangle collision detection method between triangles
    //   'observedTriangleIndex' and any triangle whose index appears in all
    //   three of the passed lists
    private void RobustDetectCollisions(int observedTriangleIndex,
            ArrayList<Integer> possibleXCollisions,
            ArrayList<Integer> possibleYCollisions,
            ArrayList<Integer> possibleZCollisions) {
        
        for (int xi = 0; xi < possibleXCollisions.size(); xi++) {
            int xTriangle = possibleXCollisions.get(xi);
            for (int yi = 0; yi < possibleYCollisions.size(); yi++) {
                int yTriangle = possibleYCollisions.get(yi);
                // Check to see if xTriangle is in the
                //   'possibleYCollisions' list
                if (yTriangle == xTriangle) {
                    // Check to see if xTriangle (and therefore also yTriangle)
                    //   is in the 'possibleYCollisions' list
                    for (int zi = 0; zi < possibleZCollisions.size(); zi++) {
                        int zTriangle = possibleZCollisions.get(zi);
                        if (zTriangle == xTriangle) {
                            // A collision is possible in each x, y and z
                            // directions: check for collisions
                            TrianglesDetectCollision(observedTriangleIndex,
                                    xTriangle);
                            
                            /*System.out.println("SHIZZAM");
                            System.out.println(observedTriangleIndex + " and " + xTriangle);
                            NL("Observed triangle X min " + trianglesXMinBound[observedTriangleIndex]);
                            NL("Observed Triangle X max " + trianglesXMaxBound[observedTriangleIndex]);
                            NL("Other triangle X min " + trianglesXMinBound[xTriangle]);
                            NL("Other triangle X max " + trianglesXMaxBound[xTriangle]);
                            
                            NL("Observed triangle Y min " + trianglesYMinBound[observedTriangleIndex]);
                            NL("Observed Triangle Y max " + trianglesYMaxBound[observedTriangleIndex]);
                            NL("Other triangle Y min " + trianglesYMinBound[xTriangle]);
                            NL("Other triangle Y max " + trianglesYMaxBound[xTriangle]);
                            
                            NL("Observed triangle Z min " + trianglesZMinBound[observedTriangleIndex]);
                            NL("Observed Triangle Z max " + trianglesZMaxBound[observedTriangleIndex]);
                            NL("Other triangle Z min " + trianglesZMinBound[xTriangle]);
                            NL("Other triangle Z max " + trianglesZMaxBound[xTriangle]);
                            
                            PrintArrayFloat(trianglesZMinBound);
                            System.out.println("MAX");
                            PrintArrayFloat(trianglesZMaxBound);*/
                        }
                        // Terminate the loop prematurely because the triangle
                        //   has been found in the z direction list and is
                        //   unique
                        zi = possibleZCollisions.size();
                    }
                    // Terminate the loop premeturely because the triangle has
                    //   been found and is unique
                    yi = possibleYCollisions.size();
                }
            }
        }
    }
    
    // Given indices of triangle (which are presumably close in proximity),
    //   detect if they intersect by returning the vector for the
    //   force that would push 'thisTriangle' away from 'otherTriangle'
    private void TrianglesDetectCollision(int thisTriangle,
            int otherTriangle) {
        int trianglesPerStrip = 2 * nDepth;
        if (thisTriangle % trianglesPerStrip == 0) {
            // 'thisTriangle' is on the top border
            if (otherTriangle == thisTriangle - trianglesPerStrip + 1) {
                return;
            } else if (otherTriangle == thisTriangle + 1) {
                return;
            }
        } else if ((thisTriangle + 1) % trianglesPerStrip == 0) {
            // 'thisTriangle' is on the bottom border and is odd
            if (otherTriangle == thisTriangle + trianglesPerStrip - 1) {
                return;
            } else if (otherTriangle == thisTriangle - 1) {
                return;
            }
        } else {
            if (otherTriangle == thisTriangle + 1
                    || otherTriangle == thisTriangle - 1) {
                return;
            } else if (thisTriangle % 2 == 0
                    && otherTriangle == thisTriangle - trianglesPerStrip + 1) {
                return;
            } else if (thisTriangle % 2 == 1
                    && otherTriangle == thisTriangle + trianglesPerStrip - 1) {
                return;
            }
        }
        
        
        ColouriseTriangle(thisTriangle);
        ColouriseTriangle(otherTriangle);
        
        int thisAIndex = GetIndexOfFirstNodeInTriangle(thisTriangle);
        int thisBIndex = GetIndexOfSecondNodeInTriangle(thisTriangle);
        int thisCIndex = GetIndexOfThirdNodeInTriangle(thisTriangle);
        
        int otherAIndex = GetIndexOfFirstNodeInTriangle(otherTriangle);
        int otherBIndex = GetIndexOfSecondNodeInTriangle(otherTriangle);
        int otherCIndex = GetIndexOfThirdNodeInTriangle(otherTriangle);
        
        // The coordinates of points A, B and C in triangle 'thisTriangle'
        float[] thisA = 
                new float[]{nodes[thisAIndex],
                    nodes[thisAIndex + 1],
                    nodes[thisAIndex + 2]};
        float[] thisB = 
                new float[]{nodes[thisBIndex],
                    nodes[thisBIndex + 1],
                    nodes[thisBIndex + 2]};
        float[] thisC = 
                new float[]{nodes[thisCIndex],
                    nodes[thisCIndex + 1],
                    nodes[thisCIndex + 2]};
        
        // The coordinates of points A, B and C in triangle 'otherTriangle'
        float[] otherA = 
                new float[]{nodes[otherAIndex],
                    nodes[otherAIndex + 1],
                    nodes[otherAIndex + 2]};
        float[] otherB = 
                new float[]{nodes[otherBIndex],
                    nodes[otherBIndex + 1],
                    nodes[otherBIndex + 2]};
        float[] otherC = 
                new float[]{nodes[otherCIndex],
                    nodes[otherCIndex + 1],
                    nodes[otherCIndex + 2]};
        
        Triangle_Pair trianglePair = new Triangle_Pair();
        trianglePair.SetTriangle1(thisA, thisB, thisC);
        trianglePair.SetTriangle2(otherA, otherB, otherC);
        
        if (trianglePair.DetermineCollision()) {
            System.out.println(thisTriangle + " " + otherTriangle);
        }
    }
    
    // Returns an array (3 floats) which is normal to the plane described by
    //   the three coordinates (A, B and C) describing a triagle (describing
    //   a plane). The normal is (B - A) x (C - A)
    private float[] GetNormalToTriangle(float[] A, float[] B, float[] C) {
        float[] normal = new float[3];
        // Determine the cross product
        normal[0] = (B[1] - A[1]) * (C[2] - A[2])
                - (B[2] - A[2]) * (C[1] - A[1]);
        normal[1] = (B[0] - A[0]) * (C[2] - A[2])
                - (B[2] - A[2]) * (C[0] - A[0]);
        normal[2] = (B[0] - A[0]) * (C[1] - A[1])
                - (B[1] - A[1]) * (C[0] - A[0]);
        
        return normal;
    }
    
    // Returns A x B
    private float[] CrossProduct(float[] A, float[] B) {
        float[] cross = new float[3];
        // Determine the cross product
        cross[0] = A[1] * B[2] - A[2] * B[1];
        cross[1] = A[0] * B[2] - A[2] * B[0];
        cross[2] = A[0] * B[1] - A[1] * B[0];
        
        return cross;
    }
    
    // Returns the dot product between the two given 'vectors' (arrays of
    //   length 3)
    private float DotProduct(float[]  A, float[] B) {
        return A[0] * B[0] + A[1] * B[1] + A[2] * B[2];
    }
    
    
    // Sets the colour of all three edges of the triangle to red
    private void ColouriseTriangle(int triangle) {
        int xAIndex = GetIndexOfFirstNodeInTriangle(triangle);
        int xBIndex = GetIndexOfSecondNodeInTriangle(triangle);
        int xCIndex = GetIndexOfThirdNodeInTriangle(triangle);
        
        NodeBleed(xAIndex);
        NodeBleed(xBIndex);
        NodeBleed(xCIndex);
    }
    
    // Makes the node red (for the visual component of the simulation)
    private void NodeBleed(int nodeXIndex) {
        int nodeIndex = nodeXIndex / 3;
        if (nodeIndex / (nDepth + 1) < nWidth) {
            colours[2 * nodeXIndex] = 1f;
            colours[2 * nodeXIndex + 1] = 0f;
            colours[2 * nodeXIndex + 2] = 0f;
        }
        
        if (nodeIndex / (nDepth + 1) > 0) {
            colours[2 * nodeXIndex - 3 * (2 * nDepth + 1)] = 1f;
            colours[2 * nodeXIndex - 3 * (2 * nDepth + 1) + 1] = 0f;
            colours[2 * nodeXIndex - 3 * (2 * nDepth + 1) + 2] = 0f;
        }
    }
    
}