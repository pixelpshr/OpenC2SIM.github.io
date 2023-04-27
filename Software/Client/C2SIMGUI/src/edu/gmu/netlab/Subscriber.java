/*----------------------------------------------------------------*
|   Copyright 2009-2022 Networking and Simulation Laboratory      |
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 
 * package edu.gmu.c4i.sbmlsubscriber;
 */

package edu.gmu.netlab;

import com.jaxfront.core.util.URLHelper;
import java.io.File;
import javax.jms.*;
import java.util.*;
import java.awt.Color;

// DOM and XPATH
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.io.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

import edu.gmu.c4i.c2simclientlib2.C2SIMClientSTOMP_Lib;
import edu.gmu.c4i.c2simclientlib2.C2SIMClientException;
import edu.gmu.c4i.c2simclientlib2.C2SIMSTOMPMessage;
import edu.gmu.c4i.c2simclientlib2.C2SIMHeader;

import org.w3c.dom.*;

/**
 * subscribes to STOMP server, reads STOMP message,
 * and if they are General Status Report posts them to map
 * 
 * @author dcorner
 * @author modified: mababneh
 * @since	3/1/2010
 * @param args the command line arguments
 */
public class Subscriber implements Runnable {
    
    static C2SIMGUI bml = C2SIMGUI.bml;
    public static String host = "HOSTNAME";
    static int count = 0;
    public static org.w3c.dom.Document listenerDocument;    // the C2SIM document
    public static boolean subscriberIsOn = false;
    public static TopicConnection conn = null;
    public static TopicSession session = null;
    private String rcvDocumentType = "";
    
    String subDocReportType = "";
    String subDocUnitID = "";
    String subDocGDCLat ="";
    String subDocGDCLon ="";
    
    int messageCount = 0;
    
    /**
     * Constructor (empty)
     */
    public Subscriber() throws Exception {}
    
    /**
     * Method to stop Subscriber
     */
    public void stopSub() {
    	subscriberIsOn = false;
        if(!bml.getConnected())return;
        bml.setConnected(false);
        if(bml.debugMode)bml.printDebug(
            "Subscriber is OFF - No messages will be accepted");
        try {
            stomp.disconnect();
        } 
        catch (C2SIMClientException bce) {
            bml.printError(
                "C2SIM Client Exception in disconnect: " + bce.getMessage());
        } 
    }// end stopSub()
    
    /**
     * make a DOM Document from a String
     * @param is - input stream
     * @return
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException 
     */  
    public static org.w3c.dom.Document loadXmlFrom(java.io.InputStream is)
        throws org.xml.sax.SAXException, java.io.IOException
    {
        javax.xml.parsers.DocumentBuilderFactory factory =
            javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder builder = null;
        try
        {
            builder = factory.newDocumentBuilder();
        }
        catch (javax.xml.parsers.ParserConfigurationException ex) {}
        org.w3c.dom.Document doc = builder.parse(is);
        if(bml.debugMode)bml.printDebug("Document string" + doc.toString());
        is.close();
        return doc;
        
    }// end loadXmlFrom()
    
    /**
     *  Thread run() method to receive STOMP
     */
    C2SIMClientSTOMP_Lib stomp = null;
    public void run() {
      
        // setup for subscription
        host = bml.serverName;
        if(bml.debugMode)bml.printDebug("Begin Subscriber");
        if(bml.debugMode)bml.printDebug("Subscribe to : " +  host);
        bml.subscribeButton.setEnabled(false);
        if(bml.debugMode)bml.printDebug("making STOMP connection");
        String user, domain, hostSource;
        Document doc;

        // Create STOMP Client Object
        stomp = new C2SIMClientSTOMP_Lib();

        // Set STOMP parameters
        user = "C2SIMGUI";
        domain = bml.orderDomainName;
        hostSource = bml.serverName;
        stomp.setHost(hostSource);
        stomp.setDestination("/topic/C2SIM");
        
        // set STOMP filters
        // C2SIM subscriptions
        String as = null;
        if(bml.autoDisplayReports.equals("C2SIM") || 
            bml.autoDisplayReports.equals("ALL")){
            if(bml.autoDisplayOrders.equals("C2SIM") || 
                bml.autoDisplayOrders.equals("ALL")){ 
                as = "(message-selector = 'C2SIM_Command')" +
                    " or (message-selector = 'C2SIM_Initialization')" +
                    " or (message-selector = 'C2SIM_Order')" +
                    " or (message-selector = 'C2SIM_Report')";
            } else as ="(message-selector = 'C2SIM_Command')" +
                " or (message-selector = 'C2SIM_Initialization')" +
                " or (message-selector = 'C2SIM_Report')";
        } else {
            if(bml.autoDisplayOrders.equals("C2SIM") || 
                bml.autoDisplayOrders.equals("ALL")){
                as = "(message-selector = 'C2SIM_Command')" +
                " or (message-selector = 'C2SIM_Initialization')" +
                " or (message-selector = 'C2SIM_Order')";
            } else as = "(message-selector = 'C2SIM_Command')" +
                " or (message-selector = 'C2SIM_Initialization')";
        } 
        
        // BML subscriptions
        if(bml.autoDisplayReports == "BML"
            || bml.autoDisplayReports == "IBML"
            || bml.autoDisplayReports == "IBML09")
            as = "protocol = 'BML'";
        
        // submit advanced subscription
        stomp.addAdvSubscription(as);
        
        // echo parameters to log
        if(bml.debugMode)bml.printDebug("STOMP client parameters:");
        if(bml.debugMode)bml.printDebug("Domain:" + domain);
        if(bml.debugMode)bml.printDebug("STOMP host:" + hostSource);
        if(bml.debugMode)bml.printDebug("STOMP advSubscription" + as);

        // Connect to the host
        C2SIMSTOMPMessage response = null;
        if(!bml.getConnected()) {
            System.out.println("Connecting to STOMP server at " + 
                stomp.getHost());
            try {
                response = stomp.connect();
                if (response.getMessageBody()==null) {
                    bml.printError(
                        "Failed to connect to STOMP host: " + 
                        hostSource + " - " + response.getMessageBody());
                    subscriberIsOn = false;
                    bml.setConnected(false);
                    return;
                }
            }
            catch (C2SIMClientException bce) {
                bml.printError("exception in connect to STOMP:" + bce.getCauseMessage());
                bml.showErrorPopup(
                    "connect timed out - restart C2SIMGUI to try again",
                    "Server connection failed");
            }
        } 
        if(response == null){
            
            // turn off label in top panel
            bml.subscribeButton.setEnabled(true);
            bml.subscriberStatusLabel.setText("NO");
            bml.subscriberStatusLabel.setForeground(Color.RED);
            return;
        }
        System.out.println("STOMP connect response received");
        bml.setConnected(true);
        subscriberIsOn = true;
 
        // change labels accordingly
        bml.subscriberStatusLabel.setText("YES");
        bml.subscriberStatusLabel.setForeground(Color.BLUE);
        bml.subscribeButton.setText("UNSUBSCRIBE STOMP");
        bml.subscribeButton.setEnabled(true);
        bml.reportListenerButton.setEnabled(true);
        bml.playButton.setEnabled(true);
        bml.recordButton.setEnabled(true);
        
        // send server and recordPlay status requests
        InitC2SIM initC2SIM = new InitC2SIM();
        initC2SIM.pushStatusC2SIM();
        initC2SIM.pushStatusRecordPlayback();

        // Start listening
        C2SIMSTOMPMessage message = null;
        try{
            while (subscriberIsOn)
            {
                // read message with STOMP blocking until next
                try {
                    message = stomp.getNext_Block();
                } catch(NullPointerException npe) {
                    if(bml.debugMode)bml.printDebug("STOMP GETNEXTBLOCK NULLPOINTER");
                    npe.printStackTrace();
                    subscriberIsOn = false;
                    bml.setConnected(false);
                    bml.subscribeButton.setText("SUBSCRIBE STOMP");
                    return;
                }

                // network read error - display popup and stop reading
                if(message == null){
                    try{Thread.sleep(1);}catch(InterruptedException ie){}
                    continue;
                }
               
                // if we are recording, send a copy to the recorder
                if(bml.recorder != null)
                    bml.recorder.recordStomp(message);

                // extract parameters from header
                try {
                    String selectorDomain = message.getHeader("selectorDomain");
                    String protocol = message.getHeader("protocol");
                    String protocolVersion = message.getHeader("c2sim-version");
                    String submitter = message.getHeader("submitter");
                    String firstforwarder = message.getHeader("first-forwarder");
                    String messageSelector = message.getHeader("message-selector");
                    String messageBody = message.getMessageBody().trim();
                    if(bml.debugMode)
                        bml.printDebug(
                        "received STOMP protocol:" + protocol + 
                        " submitter:" + submitter +
                        " c2sim-version:" + protocolVersion +
                        " first-forwarder:" + firstforwarder +
                        " message-selector:" + messageSelector +
                        " messageBodyLength:" + messageBody.length());       
                    bml.threadSub.yield();
                    if(messageBody.length() == 0)continue;      
                    interpretMessage(
                        messageBody, protocolVersion,messageSelector, submitter);
                } catch(NullPointerException npe) {
                    if(bml.debugMode)bml.printDebug("STOMP PARAMETERS NULLPOINTER");
                    npe.printStackTrace();
                    subscriberIsOn = false;
                    bml.setConnected(false);
                    bml.subscribeButton.setText("SUBSCRIBE STOMP");
                }
                
                // reset message and go back to wait loop
                message = null;
           
            }// end Subscriber while(subscriberIsOn)
        
        } catch(NullPointerException npe) {
          if(bml.debugMode)bml.printDebug("STOMP NULLPOINTER");
          npe.printStackTrace();
          subscriberIsOn = false;
          bml.setConnected(false);
          bml.subscribeButton.setText("SUBSCRIBE STOMP");
        }
        catch(C2SIMClientException bce) {
            bml.printError("BMLClientException:" + bce.getMessage() +
                " cause:" + bce.getCauseMessage());
            bml.showErrorPopup( 
                "network read failed - disconnecting", 
                "Network Error Message");
            subscriberIsOn = false;
            bml.setConnected(false);
            bml.subscribeButton.setText("SERVER SUBSCRIBE");
            bml.subscriberStatusLabel.setText("NO");
            bml.subscriberStatusLabel.setForeground(Color.RED);
        }
        catch(Exception e) {
          if(bml.debugMode)bml.printDebug("STOMP SUBSCRIBER EXCEPTION "+e.getMessage());
          e.printStackTrace();
          subscriberIsOn = false;
          bml.setConnected(false);
          bml.subscribeButton.setText("SERVER SUBSCRIBE");
        } 
            
        // wait for messages to be delivered to the interpretMessage method
        while (subscriberIsOn) 
            try{Thread.sleep(100);}catch(InterruptedException ie){}
        
        // disconnect STOMP
        if(bml.getConnected()){
            try {
                stomp.disconnect();
            }
            catch(C2SIMClientException bce) {
                bml.printError("error disconnecting STOMP:" + bce.getCauseMessage());
            }
        }
        bml.setConnected(false);
        bml.subscribeButton.setText("SERVER SUBSCRIBE");
        bml.subscribeButton.setEnabled(true);
        
    }// end run()
      
    /**
     * interprets MessageBody, saves data, and adds to map
     */
    public void interpretMessage(
        String messageBody, 
        String protocolVersion,
        String messageSelector,
        String submitter) {
        
        messageCount++;  

        // message could be server control, initialization, order or report
        int index500 = messageBody.length();
        if(index500 > 500)index500 = 500;
        String first500 = messageBody.substring(0,index500);
        // for debugging, print the message
        //System.out.println("MESSAGEBODY PROTOCOL:" + protocolVersion + " SUBMITTER:" +
        //submitter + " SELECTOR:" + messageSelector + " FIRST500:\n" + messageBody);
       
        // server control messages
        if(first500.contains(bml.c2simSystemCommandDomain)) {
            System.out.println("***CONTROL:"+first500);//debugx
            
            // check for status of Playback and Record
            if(first500.contains("StopPlayback")){
                bml.recordPlayStatus.setVisible(false);
                bml.runServerPlayer = false;
                if(bml.previousUnitMap != null)
                if(bml.okCancelPopup("Initialization replaced for Playback",
                    "Revert to previous Initialization?"))
                        bml.revertORBAT(); 
            }
            if(first500.contains("StartPlayback")){
                bml.recordPlayStatus.setText("PLAYING");
                bml.recordPlayStatus.setVisible(true);
                bml.runServerPlayer = true;
            }
            if(first500.contains("StopRecording"))
                bml.recordPlayStatus.setVisible(false);
            if(first500.contains("StartRecording")){
                bml.recordPlayStatus.setText("RECORDING");
                bml.recordPlayStatus.setVisible(true);
            }

           // get the state element value and post it
           int stateStartIndex = messageBody.indexOf("<SystemCommandTypeCode>") + 23;
           int stateEndIndex = messageBody.indexOf("</SystemCommandTypeCode>");
           if(stateEndIndex - stateStartIndex < 5){
                if(bml.debugMode)bml.printDebug("can't find state in message:" + messageBody);
                return;
            }
            bml.setServerStateLabel(
                messageBody.substring(stateStartIndex, stateEndIndex));
            
            return;
        }

        // C2SIMInitializationBody - MSDL case (only for ServerValidation)
        if(bml.runningServerTest && protocolVersion.equals("") && 
            first500.contains("MilitaryScenario")){
            bml.serverTest.writeOutputXml(messageBody,"MSDLINIT");
            return;
        }
        if(first500.contains(bml.c2simInitializationDomain) ||
            first500.contains(bml.objectInitializationDomain)){

            // process server validation
            if(bml.runningServerTest){
                if(protocolVersion.equals("0.0.9"))
                    bml.serverTest.writeOutputXml(messageBody,"009INIT");
                else 
                    bml.serverTest.writeOutputXml(messageBody,"100INIT");
            }
            
            // new initialization when running Playback:
            // ask user whether to replace initialization
            if(bml.initializationDone) {
                if(bml.runPlayer || bml.runServerPlayer){
                    if(!bml.okCancelPopup("Playback has new Initialization",
                        "Replace Initialization?"))
                        return ;
                    bml.restartORBAT();
                }
                // unlesss in Playback, don't process the initialize 
                // until REST response OK popup done and then only once
                else {
                    System.out.println("IGNORING INITIALIZATION INPUT BECAUSE ALREADY INITIALIZED");
                    System.out.println("message begins:" + first500);
                    return;
                }
            }
          
            // confirm server has the protocol version we use
            if(!bml.c2simProtocolOK(protocolVersion)){
                bml.printError("received wrong initialization protocol version:" + 
                    protocolVersion);
                return;
            }
            bml.startupInitialization = messageBody;
            int timeoutCounter = 600;
            while(bml.pushingInitialize && timeoutCounter-- > 0)
                try{Thread.sleep(100);}catch(InterruptedException ie){}
            if(timeoutCounter <= 0){
                bml.showErrorPopup(
                    "C2SIM Init Push Message",
                    "timeout waiting for REST response to Initialize");
                return;
            }
            
            // parse the intialization into MilOrg objects
            bml.showButtons(false);
            MilOrg milOrg = new MilOrg();
            if(milOrg.parseC2SIMInit(messageBody) > 0)
                bml.showButtons(true);
            if(bml.orderIconsOnScreen)
                bml.orderRemoveButton.setVisible(true);
            if(bml.reportIconsOnScreen)
                bml.reportRemoveButton.setVisible(true);
            
            // push to JaxFront if so configured
            if(bml.autoDisplayInit.equals("1")) {
                if(bml.debugMode)bml.printDebug("Subscriber pushing to JaxFront:" + 
                    bml.c2simInitializationDomain);
                bml.releaseXUICache();
                rcvDocumentType = "InitC2SIM";
                
                // put it into file as JaxFront demands
                if(bml.runningServerTest)return;
                try{
                    File xmlInitfile = File.createTempFile("xml","TempInit");
                    FileWriter tempFile = new FileWriter(xmlInitfile);
                    tempFile.write(messageBody);
                    tempFile.close();

                    // load the initialize document into JAXFront panel
                    bml.loadJaxFront(
                        xmlInitfile,
                        "C2SIM Initialize From Server",
                        bml.c2simInitSchemaLocation,
                        "MessageBody");
                } catch(Exception e) {
                    bml.printError("Exception loading JaxFront:"+e);
                    e.printStackTrace();
                    return;
                }
            }
            return;
        }// end first500.contains(bml.c2simInitializationDomain)..

        // is it a C2SIM Order?
        if(bml.autoDisplayOrders.equals("C2SIM") ||
           bml.autoDisplayOrders.equals("ALL") ||
           bml.runningServerTest)
        if(messageBody.contains(bml.c2simOrderMessageType))
        if(bml.c2simProtocolOK(protocolVersion) &&
            bml.runningServerTest){
              
            // process server validation
            if(bml.runningServerTest)
                if(protocolVersion.equals("0.0.9"))
                    bml.serverTest.writeOutputXml(messageBody,"009ORDER");
                else bml.serverTest.writeOutputXml(messageBody,"100ORDER");

            // display the order on map
            bml.releaseXUICache();
            rcvDocumentType = "C2SIM Order";	
            bml.xsdUrl = //Schema File XSD
            URLHelper.getUserURL(bml.c2simOrderSchemaLocation);	
            bml.xmlUrl = 
                URLHelper.getUserURL(messageBody);
            bml.tmpUrl = 
                URLHelper.getUserURL(messageBody + "(tmp)");
            bml.tmpFileString = messageBody + "(tmp)";
            bml.xuiUrl = // Jaxfront XUI file
                URLHelper.getUserURL(bml.xuiFolderLocation + 
                    "/TabStyleOrderC2SIM.xui");
            bml.root = "MessageBody"; 
                        
            // adjust for ASX report  // start change
            String schemaFileLocation = bml.c2simOrderSchemaLocation;
            String taskTag = "ManeuverWarfareTask";
            if(messageBody.contains("Autonomous")){
                schemaFileLocation = bml.asxOrderSchemaLocation;// ASX order schema XSD
                taskTag = "AutonomousSystemManeuverWarfareTask";
            }
            bml.xsdUrl = URLHelper.getUserURL(schemaFileLocation);
 
            // generate the swing GUI
            if(!bml.drawFromXML(
                "default-context", 
                bml.xsdUrl, 
                bml.xmlUrl, 
                bml.xuiUrl, 
                bml.root, 
                rcvDocumentType,
                taskTag,
                (new String[]{
                  "PerformingEntity",
                  "StartTime",
                  "TemporalAssociationWithAction",
                  "UUID",
                  "Name"}),
                (new String[]{
                  "Latitude",
                  "Longitude"}),
                bml.c2simns,
                messageBody,
                protocolVersion,
                false
            ))return; // stop here if error or duplicate        
         
            // load the order into JAXFront panel
            if(bml.runningServerTest)return;
            try{
                // put it into file as JaxFront demands
                File c2simTemp = File.createTempFile("c2sim","TempOrder");
                FileWriter tempFile = new FileWriter(c2simTemp);
                tempFile.write(messageBody);
                tempFile.close();
                bml.loadJaxFront(
                    c2simTemp,
                    "C2SIM Order From Server",
                    schemaFileLocation,
                    bml.root);
                c2simTemp.delete();
            } catch(Exception e) {return;}     
            return;
        }// end if(messageBody.contains(bml.c2simOrderMessageType))
        
        // is it an IBML09 Order?
        if(bml.autoDisplayOrders.equals("IBML") ||
           bml.autoDisplayOrders.equals("ALL")  ||
           bml.runningServerTest)
        if(messageBody.contains("OrderPushIBML")) {      
                                    
            // process server validation
            if(bml.runningServerTest)
                bml.serverTest.writeOutputXml(messageBody,"IBMLORDER");

            // display the order on map
            bml.releaseXUICache();
            rcvDocumentType = "IBML09 Order";	
            bml.xsdUrl = //Schema File XSD
            URLHelper.getUserURL(bml.c2simOrderSchemaLocation);	
            bml.xmlUrl = 
                URLHelper.getUserURL(messageBody);
            bml.tmpUrl = 
                URLHelper.getUserURL(messageBody + "(tmp)");
            bml.tmpFileString = messageBody + "(tmp)";
            bml.xuiUrl = // Jaxfront XUI file
                URLHelper.getUserURL(bml.xuiFolderLocation + 
                    "/TabStyleOrder09.xui");
            bml.root = "OrderPushIBML";

            // generate the swing GUI
            if(!bml.drawFromXML(
                "default-context", 
                bml.xsdUrl, 
                bml.xmlUrl, 
                bml.xuiUrl, 
                bml.root, 
                rcvDocumentType,
                "GroundTask",
            (new String[]{
                "UnitID",
                "DateTime",
                "WhereID",
                "WhereClass",
                "WhereCategory",
                "WhereLabel",
                "WhereQualifier"}),
            (new String[]{
                "Latitude",
                "Longitude"}),
                bml.ibmlns,
                messageBody,
                protocolVersion,
                false
            ))return;// stop here is error or duplicate
            
            // load the order into JAXFront panel
            if(bml.runningServerTest)return;
            try{
                // put it into file as JaxFront demands
                File ibml09Temp = File.createTempFile("ibml09","TempOrder");
                FileWriter tempFile = new FileWriter(ibml09Temp);
                tempFile.write(messageBody);
                tempFile.close();
                bml.loadJaxFront(
                    ibml09Temp,
                    "IBML09 Order From Server",
                    bml.c2simOrderSchemaLocation,
                    bml.root);
                ibml09Temp.delete();
            } catch(Exception e) {return;}
            return;
        } // end if(messageBody.contains("OrderPushIBML")) {
        
        // is it a CBML Order?
        if(bml.autoDisplayOrders.equals("CBML") ||
           bml.autoDisplayOrders.equals("ALL")  ||
           bml.runningServerTest)
        if(messageBody.contains("CBMLOrder")) {
            
            // process server validation
            if(bml.runningServerTest)
                bml.serverTest.writeOutputXml(messageBody,"CBMLORDER");

            // display the order on map
            bml.releaseXUICache();
            rcvDocumentType = "CBML Light Order";
            bml.xsdUrl = //Schema File XSD
            URLHelper.getUserURL(bml.cbmlOrderSchemaLocation);	
            bml.xmlUrl = 
                URLHelper.getUserURL(messageBody);
            bml.tmpUrl = 
                URLHelper.getUserURL(messageBody + "(tmp)");
            bml.tmpFileString = messageBody + "(tmp)";
            bml.xuiUrl = // Jaxfront XUI file
                URLHelper.getUserURL(bml.xuiFolderLocation + 
                    "/TabStyleOrderC2SIM.xui");
            bml.root = "CBMLOrder";

            // generate the swing GUI
            if(!bml.drawFromXML(
                "default-context", 
                bml.xsdUrl, 
                bml.xmlUrl, 
                bml.xuiUrl, 
                bml.root, 
                rcvDocumentType,
                "Task",
                (new String[]{
                  "AtWhere",
                  "RouteWhere",
                  "OID",
                  "TaskID",
                  "PointLight",
                  "Line",
                  "Surface",
                  "CorridorArea",
                  "TaskeeWhoRef",
                  "SpecificRoute",
                  "DateTime"}),
                (new String[]{
                  "Latitude",
                  "Longitude"}),
                bml.c2simns,
                messageBody,
                protocolVersion,
                false
            ))return;// stop here if error or duplicate ID          
            
            // load the order into JAXFront panel
            if(bml.runningServerTest)return;
            try{  
                // put it into file as JaxFront demands
                File cbmlTemp = File.createTempFile("cbml","TempOrder");
                FileWriter tempFile = new FileWriter(cbmlTemp);
                tempFile.write(messageBody);
                tempFile.close();
                bml.loadJaxFront(
                    cbmlTemp,
                    "CBML Order From Server",
                    bml.c2simOrderSchemaLocation,
                    bml.root);
                cbmlTemp.delete();
            } catch(Exception e) {return;}
            return;
        }// end if(messageBody.contains("CBMLOrder")) 

        // must be Report (or junk)
        if(bml.listenToXml || bml.runningServerTest) {

            // in order to use the message to display a report
            // we will need to make a DOM Document out of it

            // look in report header to see what the protocol is
            if(first500.contains(bml.c2simReportMessageType)){
                bml.reportBMLType = "C2SIM";
                bml.xmlReport = messageBody;
                bml.loadReportButton.setEnabled(true);        
            }
            else
            if(first500.contains("CBMLReport"))
            {
                bml.reportBMLType = "CBML";
            }
            else
            if(first500.contains("BMLReport"))
            {
                bml.reportBMLType = "IBML";
            }
            else return;// Order or junk
            if(bml.debugMode)bml.printDebug("STOMP received report of type " + bml.reportBMLType +
                "; autodisplay report setting is:" + bml.autoDisplayReports);

            // draw report messages of type IBML 
            if (bml.reportBMLType.equals("IBML") && 
                (bml.autoDisplayReports.equals("IBML") ||
                bml.autoDisplayReports.equals("IBML09") ||
                bml.autoDisplayReports.equals("ALL") ||
                bml.runningServerTest)) {            
                               
                // process server validation
                if(bml.runningServerTest)
                    bml.serverTest.writeOutputXml(messageBody,"IBMLREPORT");

                // set parameters for IBML09 report
                bml.orderDomainName = "IBML";
                bml.generalBMLFunction = "IBML";
                rcvDocumentType = "IBML09 Report";	
                bml.xsdUrl = //Schema File XSD
                  URLHelper.getUserURL(bml.ibml09ReportSchemaLocation);
                bml.tmpUrl = null;
                bml.tmpFileString = null;
                bml.xuiUrl = URLHelper.getUserURL(
                // Jaxfront XUI file
                bml.xuiFolderLocation + "/GeneralStatusReportView09.xui");
               
                // Generate the swing GUI
                if(!bml.drawFromXML(
                    "default-context", 
                    bml.xsdUrl, 
                    bml.xmlUrl, 
                    bml.xuiUrl, 
                    bml.root, 
                    rcvDocumentType,
                    "Report",
                    (new String[]{"GeneralStatusReport","ReportID","UnitID","Hostility"}),
                    (new String[]{"Latitude","Longitude"}),
                    bml.ibmlns,
                    messageBody,
                    protocolVersion,
                    false
                )){return;}// stop here if error or duplicate ID
                                
                // load the JaxFront form
                if(bml.runningServerTest)return;
                bml.xmlReport = messageBody;
                //bml.loadJaxFrontPanel(); don't load when received; let user select
            }// end if(bml.reportBMLType.equals("IBML")...

            // draw report messages of type C2SIM 
            else if (bml.reportBMLType.equals("C2SIM") &&
                    (bml.autoDisplayReports.equals("C2SIM") ||
                    bml.autoDisplayReports.equals("ALL") ||
                    bml.runningServerTest)) {           

                // process server validation
                if(!bml.c2simProtocolOK(protocolVersion) &&
                    !bml.runningServerTest)return;
                if(bml.runningServerTest)
                    if(protocolVersion.equals("0.0.9"))
                        bml.serverTest.writeOutputXml(messageBody,"009REPORT");
                    else bml.serverTest.writeOutputXml(messageBody,"100REPORT");
                
                // check whether this is final report of just-ended playback
                // as determined by matching ReportingEntiuty amd ReportID
                if(bml.previousUnitMap != null){
                    if(bml.playbackReportingEntity.equals(
                        bml.extractDataFromXml(messageBody,"ReportingEntity")))
                    if(bml.playbackReportID.equals(
                        bml.extractDataFromXml(messageBody,"ReportID"))){
                        if(bml.previousUnitMap != null)
                        if(!bml.okCancelPopup("Initialization replaced for Playback",
                            "Revert to previous Initialization?"))
                            return ;
                        bml.revertORBAT(); 
                    }
                }

                // set parameters for C2SIM report
                bml.orderDomainName = "C2SIM";
                bml.generalBMLFunction = "C2SIM";
                rcvDocumentType = "C2SIM Report";	
                bml.tmpUrl = null;
                bml.tmpFileString = null;
                bml.xuiUrl = URLHelper.getUserURL(
                    // Jaxfront XUI file
                    bml.xuiFolderLocation + "/C2SIMReportView.xui");
                bml.root = bml.c2simns+bml.c2simReportMessageType;
                
                // screen out TaskStatus reports
                if(messageBody.contains("<ReportContent><TaskStatus>")){
                    
                    // TO DO: make use of TaskComplete here
                    return;
                }

                // adjust for ASX report
                String schemaFileLocation = bml.c2simReportSchemaLocation;
                if(messageBody.contains("Autonomous"))
                     schemaFileLocation = bml.asxReportSchemaLocation;// ASX report schema XSD
                bml.xsdUrl = URLHelper.getUserURL(schemaFileLocation);
      
                // the C2SIM standard report can consist of multiple
                // <ReportContent> segments. Here we convert that format 
                // to single-segment so we can use the existing drawFomXML
                // to place its icon on the map
                   
                // extract the parts that apply to all pieces
                String firstPart = bml.extractReportFirstPart(messageBody);
                if(firstPart == null)return;
                int endingIndex = bml.findEndingIndex(messageBody);
                if(endingIndex < 0)return;
                String endingPart = messageBody.substring(endingIndex);
 
                // extract the ReportContent parts in order
                // parse them and draw to map
                int startReportContent = firstPart.length();
                String reportContent = 
                    bml.extractReportContent(messageBody,startReportContent);    
                while(reportContent != null){
 
                    // make a new single-ReportContent report with uniqe ReportID
                    String newReport = firstPart + reportContent +
                        bml.uniqueEndingPart(messageBody,endingPart);
             
                    // draw the map icon, using a generated reportId
                    // Generate the swing GUI 
                    if(!bml.drawFromXML(
                        "default-context", 
                        bml.xsdUrl, 
                        null, 
                        bml.xuiUrl, 
                        bml.root, 
                        rcvDocumentType,
                        "MessageBody",
                        (new String[]{"SubjectEntity","ReportingEntity","UUID",
                            "SideHostilityCode","PositionReportContent",
                            "ObservationReportContent","ReportID","Name",
                            "Latitude","Longitude","OperationalStatusCode"}),
                        (new String[]{}),
                        bml.c2simns,
                        newReport,
                        protocolVersion,
                        false
                    ))return;// stop here if error or duplicate ID
                    
                    
                    // go on to the next ReportContent part
                    startReportContent += reportContent.length();
                    reportContent = 
                        bml.extractReportContent(messageBody,startReportContent);
                }
              
                // load the JaxFront form
                if(bml.runningServerTest)return;
                bml.xmlUrl = URLHelper.getUserURL(messageBody);                bml.xmlReport = messageBody;
                //bml.loadJaxFrontPanel(); don't load when received; let user select
            }// end if (bml.reportBMLType.equals("C2SIM"))  

            // draw report messages of type CBML 
            else if (bml.reportBMLType.equals("CBML") && 
                (bml.autoDisplayReports.equals("CBML") ||
                bml.autoDisplayReports.equals("ALL") ||
                bml.runningServerTest)) {              
                                                    
                // process server validation
                if(bml.runningServerTest)
                    bml.serverTest.writeOutputXml(messageBody,"CBMLREPORT");

                // set parameters for C2SIM report
                bml.orderDomainName = "CBML";
                bml.generalBMLFunction = "CBML";
                rcvDocumentType = "CBML Light Report";	
                bml.xsdUrl = //Schema File XSD
                    URLHelper.getUserURL(bml.cbmlReportSchemaLocation);
                bml.tmpUrl = null;
                bml.tmpFileString = null;
                bml.xuiUrl = // Jaxfront XUI file
                    URLHelper.getUserURL(
                        bml.xuiFolderLocation + 
                            "/GeneralStatusReportView09.xui");
                bml.root = bml.c2simns + "CBMLReport";
                
                // Generate the swing GUI
                if(!bml.drawFromXML(
                    "default-context", 
                    bml.xsdUrl, 
                    bml.xmlUrl, 
                    bml.xuiUrl, 
                    bml.root, 
                    rcvDocumentType,
                    "Report",
                    (new String[]{
                        "GeneralStatusReport",
                        "ReportingData",
                        "OID",
                        "UnitID",
                        "Hostility"}),
                    (new String[]{
                        "Latitude",
                        "Longitude"}),
                     bml.cbmlns,
                     messageBody,
                     protocolVersion,
                     false
                ))return;// stop here if error or duplicate ID
                                
                // load the JaxFront form
                if(bml.runningServerTest)return;
                bml.xmlReport = messageBody;
                //bml.loadJaxFrontPanel(); don't load when received; let user select
            }// end if (bml.reportBMLType.equals("CBML"))
            // end else if chain (first500.contains(bml.reportMessageType)){

            // listening for C2SIM order
            if(first500.contains(bml.c2simOrderMessageType)){
                bml.orderBMLType = "C2SIM";
                bml.xmlOrder = messageBody;
                bml.loadReportButton.setEnabled(true);

                // send C2SIM Order to be parsed
            }
        }// end if(bml.listenToXml)
                
    } // end interpretMessage()

    /**
     * Returns String YYYYMMDDHHMMSS.sss
     */
    public synchronized static String getTimeStamp() {
        Calendar now = Calendar.getInstance();        // Create a timestamp
        long i = System.currentTimeMillis() % 1000;
        String dttm = String.format("%04d/%02d/%02d %02d:%02d:%02d",
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DATE),
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                now.get(Calendar.SECOND),
                i);
        return dttm;
    }
    
} // End of Subscriber Class
