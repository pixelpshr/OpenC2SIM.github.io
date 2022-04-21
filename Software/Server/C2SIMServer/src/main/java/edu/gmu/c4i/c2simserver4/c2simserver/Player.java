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
package edu.gmu.c4i.c2simserver4.c2simserver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.*;
//import edu.gmu.c4i.c2simclientlib2.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jdom2.*;

/**
 * @author mpullen derived from Doug Corner's C2SIM ClientLib
 plays recording into REST input of server; it will display
 on the C2SIMGUI when it comes back to us in STOMP from server
 
 limitation: recording must cover no more than two successive days
 
 adapted from C2SIMGUI 15Apr2022
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
    File playbackFile;
    int timeScale;
    long timeLimit;
    String source = "Receive";
    String protocol = "C2SIM";
    String submitter;
    public static String SISOSTD = "SISO-STD-C2SIM";

    public Player(String submitterID) {
        
        // collect parameters
        playbackFile = new File(C2SIM_Server.getPlaybackFileName());
        timeScale = C2SIM_Server.getPlaybackTimeMultiple();
        submitter = submitterID;
        
        // timeLimit
        timeLimit = 1000000;//ms

        // Display parameters
        C2SIM_Server.debugLogger.info("starting Player for file:" + 
                C2SIM_Server.getPlaybackFileName() +
                " timescale:" + C2SIM_Server.getPlaybackTimeMultiple());
        
        // run playback in a separate thread
        start();
        
    } // end constructor
    
    public void run()
    {
        // Get the replay log entries read into a list
        List<String> replayLog = readReplayLog(playbackFile);
        if(replayLog == null) {
            C2SIM_Server.debugLogger.error("unable to open logile at " +
                C2SIM_Server.getPlaybackFileName());
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
            if(!C2SIM_Server.getPlaybackState())break;
            if(C2SIM_Server.getPlaybackPaused()){
                try {
                    Thread.sleep(100);
                    continue;
                }
                catch(InterruptedException ie){
                    C2SIM_Server.debugLogger.error("Sleep interrupted in Player:"+
                        ie.getMessage());
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
                            C2SIM_Server.debugLogger.error("UNEXPECTED PLAYBACK DATA:" + 
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
                
                // no delay if timeScale is 0
                if (timeScale == 0)
                    delayTime = 0.0;
                else {
                    delayTime = 
                        (double)(totalTimeThisLine - totalTimePreviousLine)/
                            dTimeScale;
                }
                
                // display the message number and delay time
                C2SIM_Server.debugLogger.info("Player send MessageNumber:" + messageNumber + 
                    " Submitter:" + submitterId + " Protocol:" + protocol +
                    " DelayTime:" + delayTime.longValue());

                // sleep for the computed time (milliseconds)
                if (delayTime > 0) {
                    //if(bml.debugMode)bml.printDebug("Sleeping " + delayTime + " ms");
                    Thread.sleep(delayTime.longValue());
                }
                
                // Create a document to hold the contents of the file
                Document d = C2SIM_Mapping.parseMessage(xml);

                // Create a transaction for the Notification message
                C2SIM_Transaction trans = new C2SIM_Transaction();
                trans.setXmlText(xml);
                trans.setDocument(d);
                trans.setProtocol(SISOSTD);
                trans.setSubmitterID(submitter);
                    
                // pass transaction to server to send
                C2SIM_Server_STOMP.publishMessage(trans);
                
            }   // try
            catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                C2SIM_Server.debugLogger.error(date + "Playback Exception:" + e.getMessage() +
                    sw.toString());
                return;
            } // end catch
            
        } // end while(lineIterator.hasNext()
        
        // turn off play state and quit
        C2SIM_Server.setPlaybackState(false);
        
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
        C2SIM_Server.debugLogger.info("starting readReplayLog for " +
            playbackFile.getPath());
        try {
            BufferedReader in = new BufferedReader(new FileReader(playbackFile));
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
                if(!C2SIM_Server.getPlaybackState())break;
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
                        C2SIM_Server.debugLogger.error(
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
            C2SIM_Server.debugLogger.info("Player lines parsed:" + lines.size());

            // all done; lines holds the parsed file
            in.close();
            
        } // try
        catch (Exception ex) {
            C2SIM_Server.debugLogger.error("Exception:" + ex.getMessage());
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
