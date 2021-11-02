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
package edu.gmu.netlab;

import edu.gmu.c4i.c2simclientlib2.*;
import edu.gmu.c4i.c2simclientlib2.C2SIMHeader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @author mpullen derived from Doug Corner's C2SIM ClientLib
 */
public class Recorder extends Thread 
{
    C2SIMGUI bml = C2SIMGUI.bml;
  
    BufferedWriter capture;
    C2SIMClientSTOMP_Lib stompLib;
    C2SIMHeader header = new C2SIMHeader();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String protocol = bml.recordingProtocol;
    String submitter = bml.playbackSubmitter;
    int msgsCollected = 0;
    String previousMessageNumber = "";

    public Recorder() {
        
        // check whether we have STOMP source
        if(!bml.getConnected()) {
            bml.showInfoPopup( 
                "STOMP is not connected - cannot record", 
                "Recording Error");
            bml.recordFailed = true;
            return;
        }

        // where to put the recording
        JFileChooser xmlFc = //XML file
            new JFileChooser(bml.recordingFilesLocation + "//");	
        xmlFc.setDialogTitle("Provide the Recording logfile name");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "C2SIM logfiles", "log");
        xmlFc.setFileFilter(filter);
        xmlFc.showSaveDialog(bml);
        bml.captureFile = xmlFc.getSelectedFile();
        if(bml.captureFile == null) {
            bml.showInfoPopup( 
                "no file selected - cannot record", 
                "Recording File Select");
            bml.recordFailed = true;
            return;
        }
        String captureName = bml.captureFile.getName();
        if(!captureName.endsWith(".log") && bml.captureFile.length() == 0){
            String capturePath = bml.captureFile.getPath();
            bml.captureFile.delete();
            bml.captureFile = new File(capturePath + ".log");
        }
        if(bml.debugMode)bml.printDebug(
            "RECORDER captureFile:" + bml.captureFile.getName());

        // Open the capture file - append to end if it exists
        try{
            FileWriter fileWriter = new FileWriter(bml.captureFile, true);
            capture = new BufferedWriter(fileWriter);
        }
        catch(IOException ioe) {
             bml.showErrorPopup(
                "error opening selected file:" + ioe, 
                "Recording File Select");
            ioe.printStackTrace();
            bml.recordFailed = true;
            return;
        }
        
        // if there has been an initialization message, record it
        if(bml.startupInitialization != null)
            record(
                header.insertC2SIM(
                    bml.startupInitialization,
                    "STOMP", "ALL", "INFORM",
                    bml.c2simProtocolVersion),
                "INIT","0");        
        else if(bml.lateJoinerInitialization != null)
            record(bml.lateJoinerInitialization,"INIT","0");
        else
            bml.printError("no initialization available for recording");
            
    }// end constructor
    
    /**
     * closes the recorded file
     */
    public void close() {
        try{
            capture.close();
        }
        catch(IOException ioe){
            bml.printError(
                "IOException closing recording file:" + ioe);
        }
    }
    
    /**
     * records one C2SIM MessageBody string to file
     * can also be used for earlier C2SIM messages
     */
    public void record(String messageBody, String msgSubmitter, String messageNumber) {
  
        // messageNumber not changed => duplicate  => do not record
        if(messageNumber.equals(previousMessageNumber))return;
        previousMessageNumber = messageNumber;
 
        Date nowDate = new Date();
        String outDate = sdf.format(nowDate);
        String outLine = outDate + ",111 Receive " + messageNumber + " " + 
            msgSubmitter + " " +
            messageBody.replaceAll("\n", "").replaceAll(">\\s*<", "><") + "\n";
        try{
            capture.write(outLine);
            capture.newLine();
        }
        catch(IOException e) {
            bml.showErrorPopup( 
                bml.serverName + " " + e.getMessage() + " - " + e.getCause(), 
                "Recording Error");
            e.printStackTrace();
            close();
        }
    }
    
    /**
     * records one BMLSTOMPMessage to file
     * @param message 
     */
    public void recordStomp(C2SIMSTOMPMessage message)
    {
        // assemble record header info
        String msgSubmitter = message.getHeader("submitter");
        String msgProtocol = message.getHeader("protocol");
        String messageNumber = message.getHeader("message-number");
        String xml = message.getMessageBody();
        Date nowDate = new Date();
        String outDate = sdf.format(nowDate);
        if(bml.debugMode)bml.printDebug(
            "recordStomp input config protocol:" + protocol + " message protocol:" +
            msgProtocol+" config submitter:" + submitter + " message submitter:" + 
            msgSubmitter);
        
        // write to the file
        if (protocol.equalsIgnoreCase("ALL") || msgProtocol.equalsIgnoreCase(protocol)){
            if (submitter.equalsIgnoreCase("ALL") || (msgSubmitter.equalsIgnoreCase(submitter))) {
                // Here we are going to collect this message
                // C2SIM messages received have their C2SIM envelope stripped off.  
                //  We need to put it back to make these entries look like replay log entries
                if (msgProtocol.equalsIgnoreCase("C2SIM"))
                    xml = C2SIMHeader.insertC2SIM(xml, "STOMP", "ALL",
                        "INFORM",// performative for Initialize
                        bml.c2simProtocolVersion);
                record(xml, msgSubmitter, messageNumber);  
            } // end if
        } // end if
    }// end record()
}// end Recorder class
