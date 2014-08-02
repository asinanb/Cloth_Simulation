/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cloth_v11;

/**
 *
 * @author Jack Reardon
 * @created Jul 6, 2014
 * @edited Jul 6, 2014
 */
public class ForceObject {
    
    public float x, y, z;
    public float scale;
    
    int nodeThisIndex; // Index in the force matrix (held externally) of the
        // node to which this force applies
    int nodeThatIndex; // Index in the force matrix (held externally) of the
        // node from which this force has been applied
    
    public void SetForce(float x, float y, float z, float scale) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
    }
    
    public float GetXScaled() {
        return x * scale;
    }
    
    public float GetYScaled() {
        return y * scale;
    }
    
    public float GetZScaled() {
        return z * scale;
    }
    
    // Assigns forces to nodes relevant to this force object
    public void AssignForces(float[] forceArray) {
        AssignForceAccumulate(forceArray);
        
        AssignForceToThatAccumulate(forceArray);
    }
    
    // Assigns this scaled force the next three elements in the given array
    public void AssignForce(float[] forceArray) {
        forceArray[nodeThisIndex] = x * scale;
        forceArray[nodeThisIndex + 1] = y * scale;
        forceArray[nodeThisIndex + 2] = z * scale;
    }
    
    // Accumulates this scaled force the next three elements in the given array
    public void AssignForceAccumulate(float[] forceArray) {
        forceArray[nodeThisIndex] += x * scale;
        forceArray[nodeThisIndex + 1] += y * scale;
        forceArray[nodeThisIndex + 2] += z * scale;
    }
    
    // Assigns this scaled force the next three elements in the given array
    // for the node from which the force comes from
    public void AssignForceToThat(float[] forceArray) {
        forceArray[nodeThatIndex] = -x * scale;
        forceArray[nodeThatIndex + 1] = -y * scale;
        forceArray[nodeThatIndex + 2] = -z * scale;
    }
    
    // Accumulates this scaled force the next three elements in the given array
    public void AssignForceToThatAccumulate(float[] forceArray) {
        forceArray[nodeThatIndex] -= x * scale;
        forceArray[nodeThatIndex + 1] -= y * scale;
        forceArray[nodeThatIndex + 2] -= z * scale;
    }

}
