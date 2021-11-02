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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.*;
import edu.gmu.c4i.c2simclientlib2.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * @author mpullen derived from Doug Corner's C2SIM ClientLib
 plays recording into REST input of server; it will display
 on the C2SIMGUI when it comes back to us in STOMP from server
 
 limitation: recording must cover no more than two successive days
 
 updated 6/24/2019
 */
/*
    Message formats in recorded file:
    INFO        legacy recordings
    Receive     Message as received
    Client      Original message as published
    Translated	Translated message as published
    Generated	Message generated, e.g. MessageBody 
                or C2SIM_SimulationControl as published
    Command	Received Command (e.g. SHARE)
    Cyber       Received cyber attack definition
    Document	Document mode message as published
*/
public class Player extends Thread
{
    C2SIMGUI bml = C2SIMGUI.bml;
    
    LocalPlayer playLocal;
    
    int timeScale;
    long timeLimit;
    String host;
    String source;
    String protocol;
    String submitter;

    public Player() {
  
        // where to find the recording
        JFileChooser xmlFc = //XML file
            new JFileChooser(bml.playbackFilesLocation + "//");	
        xmlFc.setDialogTitle("Provide the Recording XML file name");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "C2SIM logfiles", "log");
        xmlFc.setFileFilter(filter);
        xmlFc.showOpenDialog(null);
        bml.logFile = xmlFc.getSelectedFile();
        if(bml.logFile == null) {
            bml.showInfoPopup( 
                "no file selected - cannot playback", 
                "Playback File Select");
            bml.playFailed = true;
            return;
        }
        
        // Host name or address
        host = bml.serverName;

        // Source
        source = "Receive";
 
        // Protocol
        protocol = bml.playbackProtocol;

        // Submitter
        submitter = bml.playbackSubmitter;

        // timeScale
        timeScale = bml.playbackTimescale;
        
        // timeLimit
        timeLimit = 1000*bml.playbackTimelimit;

        // Display parameters
        if(bml.debugMode)bml.printDebug("starting Player for file:" + bml.logFile.getName());
        if(bml.debugMode)bml.printDebug("protocol:" + protocol + " submitter:" + submitter +
            " timescale:" + timeScale + " timeLimit:" + timeLimit);
        
        // run playback in a separate thread so it does not lock up button
        start();
        
        // use local queue for offline playback
        if(!bml.getConnected()) {
            playLocal = new LocalPlayer();
            playLocal.start();
        }
        
    } // end constructor
    
    public void run()
    {
        // Get the replay log entries read into a list
        List<String> replayLog = readReplayLog(bml.logFile);
        if(replayLog == null) {
            bml.showErrorPopup(
                "error playing selected file", 
                "Playback File Select");
            bml.player = null;
            return;
        }
               
        // setup to play the list
        String submitterId = "";
        String date = "", time, nextPiece;
        Double delayTime, dTimeScale = (double)timeScale;
        Integer messageNumber = 0;
        char previousDay = ' ', testDay;
        int endSource, endNumber = 0, startXml;
        long totalTimeThisLine, totalTimePreviousLine = 0L;// in milliseconds

        // Process each entry in the replay log
        Iterator<String> lineIterator = replayLog.iterator();
        while(lineIterator.hasNext()){
            if(!bml.runPlayer)continue;
            if(bml.pausePlayer){
                try {
                    Thread.sleep(100);
                    continue;
                }
                catch(InterruptedException ie){
                    System.err.println("Sleep interrupted in Player:"+ie.getMessage());
                    break;
                }
            }
            String line = lineIterator.next();
            if (line.length() <= 4)continue;
            
            // parse the recording header before XML
            startXml = line.indexOf('<');
            try {
                // Convert date and time string to elapsed time
                // limitation: not over more than one day
                date = line.substring(0,10);
                time = line.substring(11,23);
                long hour = Integer.parseInt(time.substring(0,2));
                long minute = Integer.parseInt(time.substring(3,5));
                long second = Integer.parseInt(time.substring(6,8));
                totalTimeThisLine = (long)Integer.parseInt(time.substring(9));
                totalTimeThisLine += // convert hour-minute-second to ms
                    (3600000*hour + 60000*minute + 1000*second);
                
                // deal with date that changes by one day in mid-recording
                testDay = date.charAt(9);
                if(previousDay == ' ')previousDay = testDay;
                if(testDay != previousDay)totalTimeThisLine += 86400000; // = 1000*24*3600
                previousDay = testDay;
                
                // start time offset
                if(totalTimePreviousLine == 0L)
                    totalTimePreviousLine = totalTimeThisLine;
                
                // deal with legacy recordings that may not have all
                // current preamble data of current recordings source
                nextPiece = line.substring(24,75);
                
                // latest uncordinated change: two blanks after time
                if(nextPiece.charAt(0) == ' ')nextPiece = line.substring(25,75);
                endSource = nextPiece.indexOf(' ');
                
                // extract source (must be Receive or INFO)
                source = nextPiece.substring(0,endSource);
                if(!source.equals("Receive") && !source.equals("INFO"))
                    continue;

                // message number
                if(startXml-24 > endSource+1){
                    int startMessageNumber = endSource + 1;
                    nextPiece = nextPiece.substring(endSource+1);
                    endNumber = nextPiece.indexOf(' ');
                    try{
                      messageNumber = Integer.parseInt(nextPiece.substring(0,endNumber));
                    }
                    catch (NumberFormatException nfe)
                    {
                        // probably this is junk from new server 
                        // recording - try to look past it
                        nextPiece = nextPiece.substring(endNumber+1);
                        if(nextPiece.substring(0,1).equals(" "))
                            nextPiece = nextPiece.substring(1);
                        endNumber = nextPiece.indexOf(' ');
                        try{
                            messageNumber = Integer.parseInt(nextPiece.substring(0,endNumber));
                        }
                        catch (Exception e)
                        {
                            bml.printError("UNEXPECTED PLAYBACK DATA:" + 
                                nextPiece.substring(0,endNumber) + "|");
                            continue;
                        }
                    }
                }
                else messageNumber++;
                
                // submitter
                if(startXml > endSource + endNumber){
                    int startSubmitter = endNumber;
                    while(nextPiece.charAt(startSubmitter) == ' ')
                        startSubmitter++;
                    nextPiece = nextPiece.substring(startSubmitter);
                    int endSubmitter = nextPiece.indexOf(' ');
                    submitterId = nextPiece.substring(0,endSubmitter);
                }
                else submitter = "";
                if (!(submitterId.equalsIgnoreCase(submitter)) && 
                    !(submitter.equalsIgnoreCase("ALL")))
                    continue; 

                // C2SIM/BML XML
                String xml = line.substring(startXml);
                
                if(bml.getConnected()) {
                    // correct C2SIMInitializationBody to ObjecctInitializationBody
                    // this asumes the XML does not contain data matching swapFrom tags
                    String swapFrom = "<C2SIMInitializationBody>";
                    String swapTo = "<ObjectInitializationBody>";
                    if(xml.contains("<C2SIMInitializationBody>")){
                        int startReplace = xml.indexOf(swapFrom);
                        int endReplace = startReplace + swapFrom.length();
                        String replaceXml =
                            xml.substring(0,startReplace)  +
                            swapTo +
                            xml.substring(endReplace);

                        // end tag
                        swapFrom = "</C2SIMInitializationBody>";
                        swapTo = "</ObjectInitializationBody>";
                        startReplace = replaceXml.indexOf(swapFrom);
                        endReplace = startReplace + swapFrom.length();
                        xml = 
                            replaceXml.substring(0,startReplace)  +
                            swapTo +
                            replaceXml.substring(endReplace);
                    }
                }
                
                // no delay if user set timeScale to 0
                if (timeScale == 0)
                    delayTime = 0.0;
                else {
                    delayTime = 
                        (double)(totalTimeThisLine - totalTimePreviousLine)/
                            dTimeScale;
                }

                // Check the protocol and instantiate BMLClientREST object
                C2SIMClientREST_Lib bmlRest = null;
                String sendProtocol = "SISO-STD-C2SIM";
                if (xml.contains("<MessageBody")){
                    // It is a C2SIM Message
                    if ((protocol.equalsIgnoreCase("C2SIM")) || 
                        protocol.equalsIgnoreCase("ALL")) {
                        if(xml.contains("<C2SIMHeader"))
                            xml = C2SIMHeader.removeC2SIM(xml);                 
                        bmlRest = new C2SIMClientREST_Lib("REPLAY", "ALL", "INFORM",
                            bml.c2simProtocolVersion);
                        bmlRest.getC2SIMHeader().setProtocolVersion(bml.c2simProtocolVersion);
                    }
                    else if(!bml.autoDisplayReports.equals("C2SIM") &&
                            !bml.autoDisplayReports.equals("ALL")) {
                        bml.printError(
                            "Player reading logfile with C2SIM which does not match AutoDisplay setting");
                        bml.player = null;
                        return;
                    }
                }
                else {  
                    // It must be a BML Message
                    if ((protocol.equalsIgnoreCase("BML")) || protocol.equalsIgnoreCase("ALL")){
                        bmlRest = new C2SIMClientREST_Lib();
                        sendProtocol = "BML";
                    }
                    else if(!bml.autoDisplayReports.substring(0,4).equals("IBML") &&
                            !bml.autoDisplayReports.equals("ALL")) {
                        bml.printError(
                            "Player reading logfile with BML which does not match AutoDisplay setting");
                        bml.player = null;
                        return;
                    }
                }
                
                // Did one of the above protocol tests pass?  
                // If so we really have instantiated the REST client
                if (bmlRest == null)continue;
                
                
                // display the message number and delay time
                if(bml.debugMode)bml.printDebug("\nPlayer send MessageNumber:" + messageNumber + 
                    " Submitter:" + submitterId + " Protocol:" + sendProtocol +
                    " DelayTime:" + delayTime.longValue());

                // sleep for the computed time (milliseconds)
                if (delayTime > 0) {
                    //if(bml.debugMode)bml.printDebug("Sleeping " + delayTime + " ms");
                    Thread.sleep(delayTime.longValue());
                }

                // Set parameters for this transaction
                bmlRest.setHost(host);
                bmlRest.setSubmitter(submitterId);
                String response;

                // Submit the transaction to the server
                if(bml.getConnected()){
                
                    // NullPointer from ClientLib implies unplayable record
                    try{
                        response = bmlRest.bmlRequest(xml);
                    } catch(NullPointerException npe)
                    {
                        if(bml.debugMode)bml.printDebug("NULLPOINTER");
                        continue;
                    }
                    
                    // Check and display the response
                    if (response.contains("OK"))
                        if(bml.debugMode)bml.printDebug(date + "  OK");
                    else
                        bml.printError(date + " ERROR: " + response);
                }
                // if not online, queue the message locally
                else
                    bml.localQueue.add(xml);
            }   // try
            catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                bml.printError(date + "Exception:" + e.getMessage());
                bml.printError(sw.toString());
                bml.showErrorPopup( 
                    "error in file - cannot playback:" + e, 
                    "Playback Error");
                bml.player = null;
                return;
            } // end catch
            
        } // end while(lineIterator.hasNext()
        
        // turn off play state and quit
        if(bml.getConnected())
            bml.playFileDone();
        else if(playLocal != null)
            playLocal.setPlayFileFinished();
        bml.player = null;
        
    }// end run()

    /**
     * reads logfile written by Recorder and also by server
     * creates a LinkedList of selected xml strings in memory
     * @param logfile
     * @return LinkedList of xml
     */
    LinkedList<String> readReplayLog(File logfile) {

        LinkedList<String> lines = new LinkedList<String>();
        long startTime = 0;
        if(bml.debugMode)bml.printDebug("starting readReplayLog for " + bml.logFile);
        try {
            BufferedReader in = new BufferedReader(new FileReader(bml.logFile));
            String str;
            char previousDay= ' ';

            // read all lines into linked list
            int nullStrCount = 0;
            while (nullStrCount < 3) {
                str = in.readLine();
                if(str == null){
                    nullStrCount++;
                    continue;
                }
                nullStrCount = 0;
                if(!bml.runPlayer)break;
                if(str.length() == 0)continue;
                while(!str.endsWith("</Message>") &&//TODO: other document ends
                      !str.endsWith("</BMLReport>")  &&
                      !str.endsWith("</C2SIM_Message>")  &&
                      !str.endsWith("</OrderPushIBML>")){// partial line
                    String strExt = in.readLine();
                    if(strExt == null)break;
                    if(strExt.length() == 0)break;
                    str += strExt;
                    
                    // assume no single message will be longer than 100000 bytes
                    if(str.length() > 100000){
                        bml.printError(
                            "can't play selected file - document end not found before length " +
                            str.length());
                        return null;
                    }           
                }// end while)!str
                
                // decode the time - can't span more than 2 days
                char testDay = str.charAt(9);
                if(previousDay == ' ')previousDay = testDay;
                long total = 0;
                if(testDay != previousDay)total = 86400000; // = 1000*24*3600
                previousDay = testDay;
                if(str.length() < 24)continue;
                String time = str.substring(11,23);
                long hour = Integer.parseInt(time.substring(0,2));
                long minute = Integer.parseInt(time.substring(3,5));
                long second = Integer.parseInt(time.substring(6,8));
                long millisec = Integer.parseInt(time.substring(9));
                total += 3600000*hour + 60000*minute + 1000*second + millisec;
                if(startTime == 0){
                    startTime = total;
                    total -= startTime;
                }

                // quit if past configured time
                if(timeLimit > 0)if(total > timeLimit)break;
                lines.add(str);
                
            } // end while
            if(bml.debugMode)bml.printDebug("Player lines parsed:" + lines.size());

            // all done; lines holds the parsed file
            in.close();
            
        } // try
        catch (Exception ex) {
            bml.printError("Exception:" + ex.getMessage());
            ex.printStackTrace();
            return null;
        }   // catch

        return lines;
        
    }   // readReplayLog

    // returns value of element given tag
    static String getElementData(String inXML, String tag) {
        int start, end;
        start = inXML.indexOf("<" + tag);
        end = inXML.indexOf("</" + tag);
        String result = inXML.substring(start + tag.length() + 2, end);
        return result;
        
    }// end getElementData()
    
}// end Player class
