/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cloth_v11;

import java.awt.event.KeyEvent;
import java.util.Enumeration;
import javax.media.j3d.Behavior;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.media.j3d.WakeupOnActivation;
import javax.media.j3d.WakeupOnElapsedTime;
import javax.media.j3d.WakeupOr;

/**
 *
 * @author Jack Reardon
 * @created Jul 9, 2014
 * @edited Jul 9, 2014
 */
public class BehaviourHandler extends Behavior {
    
    private static int runningTime, numberOfFrames;
    private static final int calculationTimerFrequency = 3, visualTimerFrequency = 60;
    
    public BehaviourHandler() {
        runningTime = 0;
        numberOfFrames = 0;
    }

    @Override
    public void initialize() {
        WakeupOr conditions =
                new WakeupOr(new WakeupCriterion[]{
                    new WakeupOnElapsedTime(calculationTimerFrequency),
                    new WakeupOnElapsedTime(visualTimerFrequency)});
        this.wakeupOn(conditions);
        Arit.NL("YEAH");
    }
    

    /*@Override
    public void processStimulus(Enumeration e) {
        Arit.NL(e.toString());
        Arit.NL("ASFD");
    }*/

    @Override
    public void processStimulus(Enumeration enmrtn) {
        Arit.NL(enmrtn.toString());
        Arit.NL("ASFD");
    }
}
