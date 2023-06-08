/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.gmu.netlab;

import java.io.*;
import javax.swing.JFileChooser;

/**
 * generates a sequence of selected report, with sequential ReportID
 * @author c2sim
 */
public class RepeatReportC2SIM implements Runnable 
{
    String root="";
    C2SIMGUI bml = C2SIMGUI.bml;
    String documentType = "C2SIM Report";
  
    // constructor
    public RepeatReportC2SIM() 
    {
        bml.orderDomainName = "C2SIM";
        bml.generalBMLFunction = "C2SIM";
        bml.reportBMLType = "C2SIM";
        bml.sendingRepeat = true;
    }
        
    /**
     * starts sending of repeated report
     */
    String repeatReportXml;
    Long sleepTime = 0L;
    
    /**
     * Thread method to repeat report
     */
    public void run() {
        
        // setup repeat interval
        try{
            sleepTime = new Long(bml.reportRepeatms);
            if(sleepTime < 1L){
                bml.printError("report receipt cannot be less than 1");
                sleepTime = 1L;
            }
        }
        catch(Exception e){
            bml.printError("Repeat report bad ReportRepeatms value");
            bml.showInfoPopup("bad ReportRepeatms value", "Repeat Report");
            return;
        }
        
        // get the report file to be repeated
        JFileChooser xmlFc = 
            new JFileChooser(bml.guiFolderLocation + "/Reports//");//XML file
        xmlFc.setDialogTitle("Enter the C2SIM Report XML file name");
        xmlFc.showOpenDialog(bml);
        if(xmlFc.getSelectedFile() == null){
            bml.showInfoPopup("no report file chosen to repeat", "Repeat Report");
            return;
        }
        if(bml.debugMode)
            bml.printDebug("Selected report file to repeat:" +
                xmlFc.getSelectedFile().getName());
        repeatReportXml = bml.readAnXmlFile(
            bml.guiFolderLocation + "/Reports//" + xmlFc.getSelectedFile().getName());
        
        // load the report into JAXFront panel
        try{
            File loadFile = xmlFc.getSelectedFile();
            bml.loadJaxFront(
                loadFile,
                "C2SIM Report:" + loadFile.getName(),
                bml.c2simReportSchemaLocation,
                "MessageBody");
        } catch(Exception e) {
            bml.printError("Exception in loading JaxFront report XML file:"+e);
            e.printStackTrace();
            return;
        }

        //Generate the swing GUI
        bml.drawFromXML(
            "default-context", 
            bml.xsdUrl, 
            bml.xmlUrl, 
            bml.xuiUrl, 
            bml.root, 
            documentType,
            "MessageBody",
            (new String[]{"SubjectEntity","ReportingEntity","UUID","SideHostilityCode",
                "PositionReportContent","ObservationReportContent","ActorReference",
                "ReportID","ReportBody"}),
            (new String[]{"Latitude","Longitude"}),
            bml.c2simns,
            null,
            bml.c2simProtocolVersion,
            true
        );     
       
        // send the current XML text
        
        // input and output of XML push
        String pushResultString ="";
        String pushReportInputString ="";
        
        // last 10 digits of 35 char reportID (35 = "ReportID".length()+ 25)
        int startReportId = repeatReportXml.indexOf("ReportID>")+35;
        
        // display input parameter
        int messagesSent = 0;
        long sendTimeTotal = 0L;
        System.out.println("Starting report repeat delay in ms:" + sleepTime);
        long sendTime = System.currentTimeMillis();
        String paddedId = "        ";
        // Pushing the C2SIM Report Query into the C2SIMClient
        while(bml.sendingRepeat) {
            pushResultString = 
                bml.ws.sendC2simREST(
                    repeatReportXml,
                    "REPORT",
                    bml.c2simProtocolVersion);
            long lastSendTime = sendTime;
            sendTime = System.currentTimeMillis();
            if(bml.debugMode)bml.printDebug(
                "The C2SIM report push result is : " + pushResultString);
            int elapsedTime = (int)(sendTime - lastSendTime - sleepTime);
            System.out.println("sent ID:" + paddedId + " send ms:" + elapsedTime);
            messagesSent++;
            sendTimeTotal += elapsedTime;

            // wait designated time before next send
            try{Thread.sleep(sleepTime);}catch(InterruptedException ie){
                if(bml.debugMode)bml.printDebug("Report Repeat sleep interrupted");
                return;
            }
            if(!bml.sendingRepeat || sleepTime == 0){
                int avgSendTime = (int)(sendTimeTotal/messagesSent);
                int mps = 1000/(avgSendTime + sleepTime.intValue());
                System.out.println("Report repeat stopped; delay:" + sleepTime +
                    " ms; total messages sent:" + messagesSent + "; avg send time:" + 
                    avgSendTime + " ms; message/sec:" + mps );
                return;
            }
            bml.threadRepeat.yield();

            // increment last 10 hex digits of reportID
            Long reportIdLong = 0L;
            try{
                reportIdLong = Long.valueOf(
                repeatReportXml.substring(startReportId,startReportId+10),16);
            } catch (NumberFormatException nfe){
                bml.printError("bad ReportID for repeated reports - must be hex:" +
                    repeatReportXml.substring(startReportId,startReportId+10));
                return; 
            }
            String incrementedReportId = 
                Long.toHexString(reportIdLong+1).toUpperCase();
            int incrementedLength = incrementedReportId.length();
            paddedId = 
                ("0000000000").substring(0,10-incrementedLength) + incrementedReportId;

            // insert the incremented String into the repeatReport
            repeatReportXml = repeatReportXml.substring(0, startReportId) +
                paddedId + repeatReportXml.substring(startReportId+10);
        }// end while(bml.sendingRepeat)
    }// end run()
}// end class RepeatReportC2SIM
