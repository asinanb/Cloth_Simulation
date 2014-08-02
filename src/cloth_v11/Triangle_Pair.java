/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cloth_v11;

/**
 *
 * @author Jack Reardon
 * @created Jul 27, 2014
 * @edited Jul 27, 2014
 *
 * Detects collisions between this triangle and another
 */
public class Triangle_Pair {

    float[] A1, B1, C1; // Vertices ABC describing triangle 1
    float[] A2, B2, C2; // Vertices ABC describing triangle 2
    float[] normal1, normal2;

    float A1To2, B1To2, C1To2; // Distances between triangle 1 nodes and 2 plane
    float A2To1, B2To1, C2To1; // Distances between triangle 2 nodes and 1 plane

    float[] intersectingLine; // The line common to both planes described by triangles 1 and 2
    float[] loneVertex1, pairedVertexJ1, pairedVertexK1;
    float[] loneVertex2, pairedVertexJ2, pairedVertexK2;
    float loneDistance1, pairedDistanceJ1, pairedDistanceK1;
    float loneDistance2, pairedDistanceJ2, pairedDistanceK2;
    float projLone1, projJ1, projK1;
    float projLone2, projJ2, projK2;

    int majorAxis;

    float boundM1, boundN1; // Bounds (M and N, in order) of triangle 1 on the intersecting line
    float boundM2, boundN2; // Bounds (M and N, in order) of triangle 2 on the intersecting line

    public void SetTriangle1(float[] A, float[] B, float[] C) {
        A1 = A;
        B1 = B;
        C1 = C;
    }

    public void SetTriangle2(float[] A, float[] B, float[] C) {
        A2 = A;
        B2 = B;
        C2 = C;
    }

    // Performs calculations to check if there is a collision or not
    //   Returns weather the a collision takes place
    public boolean DetermineCollision() {
        DetermineNormals();
        DetermineDistances();
        
        // Collision is impossible if 'thisTriangle' is completely to one side
        //   of 'otherTriangle'. Ie. proceed only if A is not on the same side
        //   of at least one other vertex
        if (Math.signum(A1To2) != Math.signum(B1To2)
                || Math.signum(A1To2)
                != Math.signum(C1To2)) {

            // Calculate the line intersecting both triangles
            intersectingLine = CrossProduct(normal1, normal2);

            // Determine the isolated vertex (it will be on one side of the
            //   other plane opposite to the other two vertices)
            DetermineLoneVertices();

            // Determine the dominant axis for 'intersectingLine'
            DetermineMajorAxis();

            // Determine the "projection valule" of the lone vertex and the two
            //   other vertices
            DetermineProjectionValues();

            // Determine the bounds on the interesction of either triangle on
            //   the intersecting line (for the two planes)
            DetermineIntersectingBounds();
            
            // Detect collision based on these calculated bounds
            return DetectCollision();
        } else {
            // No collision is possible, because all three vetices of triangle
            //   1 lie on the same side of the plane described by triangle 2
            return false;
        }
    }

    private void DetermineNormals() {
        normal1 = GetNormalToTriangle(A1, B1, C1);
        normal2 = GetNormalToTriangle(A2, B2, C2);
    }

    // Determines the distances between triangle 1 nodes and triangle 2 plane
    private void DetermineDistances() {
        A1To2 = DotProduct(normal2, A1)
                - DotProduct(normal2, A2);
        B1To2 = DotProduct(normal2, B1)
                - DotProduct(normal2, A2);
        C1To2 = DotProduct(normal2, C1)
                - DotProduct(normal2, A2);
        
        A2To1 = DotProduct(normal1, A2)
                - DotProduct(normal1, A1);
        B2To1 = DotProduct(normal1, B2)
                - DotProduct(normal1, A1);
        C2To1 = DotProduct(normal1, C2)
                - DotProduct(normal1, A1);
    }

    private boolean DetectCollision() {
        if (boundM1 < boundM2) {
            // Triangle 1 has smaller minimum bound than triangle 2
            if (boundN1 > boundM2) {
                // Triangle 1's bounds surround the minimum bound of triangle 2
                return true;
            } else {
                return false;
            }
        } else {
            // Triangle 2 has smaller minimum bound than triangle 1
            if (boundN2 > boundM1) {
                // Triangle 2's bounds surround the minimum bound of triangle 1
                return true;
            } else {
                return false;
            }
        }
    }
    
    private void DetermineLoneVertices() {
        DetermineLoneVertexTriangle1();
        DetermineLoneVertexTriangle2();
    }

    // Determine the isolated vertex (it will be on one side of the
    //   other plane opposite to the other two vertices) for triangle 1
    private void DetermineLoneVertexTriangle1() {
        loneVertex1 = null;
        pairedVertexJ1 = null;
        pairedVertexK1 = null;

        if (Math.signum(A1To2)
                != Math.signum(B1To2)) {
            if (Math.signum(A1To2)
                    != Math.signum(C1To2)) {
                loneVertex1 = A1;
                pairedVertexJ1 = B1;
                pairedVertexK1 = C1;

                loneDistance1 = A1To2;
                pairedDistanceJ1 = B1To2;
                pairedDistanceK1 = C1To2;
            } else {
                loneVertex1 = B1;
                pairedVertexJ1 = A1;
                pairedVertexK1 = C1;

                loneDistance1 = B1To2;
                pairedDistanceJ1 = A1To2;
                pairedDistanceK1 = C1To2;
            }
        } else {
            loneVertex1 = C1;
            pairedVertexJ1 = A1;
            pairedVertexK1 = B1;

            loneDistance1 = C1To2;
            pairedDistanceJ1 = A1To2;
            pairedDistanceK1 = B1To2;
        }
    }
    
    // Determine the isolated vertex (it will be on one side of the
    //   other plane opposite to the other two vertices) for triangle 1
    private void DetermineLoneVertexTriangle2() {
        loneVertex2 = null;
        pairedVertexJ2 = null;
        pairedVertexK2 = null;

        if (Math.signum(A2To1)
                != Math.signum(B2To1)) {
            if (Math.signum(A2To1)
                    != Math.signum(C2To1)) {
                loneVertex2 = A2;
                pairedVertexJ2 = B2;
                pairedVertexK2 = C2;

                loneDistance2 = A2To1;
                pairedDistanceJ2 = B2To1;
                pairedDistanceK2 = C2To1;
            } else {
                loneVertex2 = B2;
                pairedVertexJ2 = A2;
                pairedVertexK2 = C2;

                loneDistance2 = B2To1;
                pairedDistanceJ2 = A2To1;
                pairedDistanceK2 = C2To1;
            }
        } else {
            loneVertex2 = C2;
            pairedVertexJ2 = A2;
            pairedVertexK2 = B2;

            loneDistance2 = C2To1;
            pairedDistanceJ2 = A2To1;
            pairedDistanceK2 = B2To1;
        }
    }

    private void DetermineMajorAxis() {
        if (intersectingLine[0] > intersectingLine[1]) {
            if (intersectingLine[0] > intersectingLine[2]) {
                // 'intersectingLine' is most closely aligned with the x
                //   axis
                majorAxis = 0;
            } else {
                // 'intersectingLine' is most closely aligned with the z
                //   axis
                majorAxis = 2;
            }
        } else {
            if (intersectingLine[1] > intersectingLine[2]) {
                // 'intersectingLine' is most closely aligned with the y
                //   axis
                majorAxis = 1;
            } else {
                // 'intersectingLine' is most closely aligned with the z
                //   axis
                majorAxis = 2;
            }
        }
    }

    private void DetermineProjectionValues() {
        projLone1 = loneVertex1[majorAxis];
        projJ1 = pairedVertexJ1[majorAxis];
        projK1 = pairedVertexK1[majorAxis];
        
        projLone2 = loneVertex2[majorAxis];
        projJ2 = pairedVertexJ2[majorAxis];
        projK2 = pairedVertexK2[majorAxis];
    }

    private void DetermineIntersectingBounds() {
        boundM1 = projJ1 + (projLone1 - projJ1)
                * pairedDistanceJ1 / (loneDistance1 - pairedDistanceJ1);
        boundN1 = projK1 + (projLone1 - projK1)
                * pairedDistanceK1 / (loneDistance1 - pairedDistanceK1);
        
        // Order the bounds
        if (boundM1 > boundN1) {
            float temp = boundM1;
            boundM1 = boundN1;
            boundN1 = temp;
        }
        
        boundM2 = projJ2 + (projLone2 - projJ2)
                * pairedDistanceJ2 / (loneDistance2 - pairedDistanceJ2);
        boundN2 = projK2 + (projLone2 - projK2)
                * pairedDistanceK2 / (loneDistance2 - pairedDistanceK2);
        
        // Order the bounds
        if (boundM2 > boundN2) {
            float temp = boundM2;
            boundM2 = boundN2;
            boundN2 = temp;
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
    private float DotProduct(float[] A, float[] B) {
        return A[0] * B[0] + A[1] * B[1] + A[2] * B[2];
    }

}
