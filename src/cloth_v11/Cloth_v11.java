/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cloth_v11;

/**
 *
 * @author User
 */
public class Cloth_v11 {

    /*
    This version extends on previous versions and
        - ???
    
    TODO in later versions:
        - Use new method SetNextThree for forces
        - Incorporate mass into the model
        - Update position and velocity with more stable methods
        - Implement different force models (eg. force limit as distance increases)
        - Permit only one GetNode call per iteration for position and velocity
            updates as well
        - Make the Cloth constructor (with parameters) be able to modify most of
            Cloth's field
        - Replace Interaction Handler with Behaviour of Java3D
        - Morph triangle array and nodes array into one for faster computation
        - In the 'RobustCollisionDetect' method, perform randomised search to
            determine if a triangle is in 'possibleYCollisions' and
            'possibleZCollisions'
    */
    public static void main(String[] args) {
        new Simulation("Cloth Simulation: Version 11");
    }
    
}
