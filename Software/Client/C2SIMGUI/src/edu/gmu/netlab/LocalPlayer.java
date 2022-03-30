/*----------------------------------------------------------------*
|   Copyright 2009-2021 Networking and Simulation Laboratory      |
|         George Mason University, Fairfax, Virginia              |
|                                                                 |
| Permission to use, copy, modify, and distribute this            |
| software and its documentation for all purposes is hereby       |
| granted without fee, provided that the above copyright notice   |
| and this permission appear in all copies and in supporting      |
| documentation, and that the name of George Mason University     |
| not be used in advertising or publicity pertaining to           |
| distribution of the software without specific, written prior    |
| permission. GMU makes no representations about the suitability  |
| of this software for any purposes.  It is provided "AS IS"      |
| without express or implied warranties.  All risk associated     |
| with use of this software is expressly assumed by the user.     |
*----------------------------------------------------------------*/
/**
 * plays from to local queue when not connected
 */
package edu.gmu.netlab;

import edu.gmu.netlab.C2SIMGUI;

public class LocalPlayer extends Thread
{
    C2SIMGUI bml = C2SIMGUI.bml;
    Subscriber subscriber;
    
    public void LocalPlayer() {}
    
    boolean playFileNotFinished = true;
    
    public void setPlayFileFinished(){
        playFileNotFinished = false;
    }

    // play whatever is in the localQueue
    public void run() {
        
        // make a local copy of subscriber since we are not subscribed
        try {
            subscriber = new Subscriber();
        }
        catch(Exception e) {
            bml.printError("Exception in Subscriber:" + e);
            return;
        }

        // if localQueue has data, play it
        // otherwise wait 100 ms to look again
        try {
            while(bml.runPlayer &&
                  (playFileNotFinished || !bml.localQueue.isEmpty())) { 
                if(bml.localQueue.isEmpty() || bml.pausePlayer)
                    Thread.sleep(100);
                else {
                    yield();
                    subscriber.interpretMessage(
                        bml.localQueue.take(),
                        bml.c2simProtocolVersion);
                }
            }
        } catch(InterruptedException ie) {
            bml.printError("local playback interrupted");
        } catch(Exception e) {
            bml.printError("Exception in LocalPlayer:");
            e.printStackTrace();
        }
        bml.playFileDone();
    } 
    
}//end class LocalPlayer
